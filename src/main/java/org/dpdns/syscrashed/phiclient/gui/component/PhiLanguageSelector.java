package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.dpdns.syscrashed.phiclient.i18n.PhiLanguage;

/**
 * The language selector of the settings page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>A row of three small parallelogram segments, one per supported language; every segment shows the
 * display name of its language in that language itself (English / 简体中文 / 日本語). A click on a
 * segment switches the interface language through {@link PhiI18n#setLanguage(PhiLanguage)}, which
 * persists the choice and notifies the hub - the hub then rebuilds every page, so the whole interface
 * refreshes immediately. The selected segment is read from {@link PhiI18n#current()} every frame, so
 * the selector never holds a stale selection.</p>
 *
 * <p>Every segment owns the standard feedback of the interface: a {@link PhiSheen} sweep plus a faint
 * tint while hovered (120 ms in / 420 ms out, the shared component timing). Hit tests of hover and
 * click both use {@link PhiSkew#contains}, so the feedback never appears outside the slanted shape.</p>
 *
 * <p>The control is scale-agnostic: the design scale is derived from its height, which the settings
 * page sets during layout (the same pattern the theme toggle uses).</p>
 */
public class PhiLanguageSelector extends PhiComponent {

    /** Design height of the row; the real scale is derived from the height set during layout. */
    private static final float OPTION_HEIGHT = 20.0F;

    /** Design gap between two segments. */
    private static final float OPTION_GAP = 6.0F;

    /** Hover tint timing, shared with the rest of the components. */
    private static final float HOVER_IN = 120.0F;
    private static final float HOVER_OUT = 420.0F;

    private final PhiSheen[] sheens = new PhiSheen[PhiLanguage.values().length];
    private final float[] hoverAmounts = new float[PhiLanguage.values().length];
    private final float[] selectedAmounts = new float[PhiLanguage.values().length];

    private long lastFrame;

    /**
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     */
    public PhiLanguageSelector(float x, float y, float width, float height) {
        super(x, y, width, height);

        for (int i = 0; i < this.sheens.length; i++) {
            this.sheens[i] = new PhiSheen();
        }

        this.lastFrame = System.currentTimeMillis();
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float scale = this.height / OPTION_HEIGHT;
        float gap = OPTION_GAP * scale;
        float radius = PhiMetrics.CORNER_RADIUS * scale * 0.5F;
        float textSize = PhiMetrics.FONT_BODY * scale * 0.85F;

        PhiLanguage[] languages = PhiI18n.available();
        float segmentWidth = (this.width - gap * (languages.length - 1)) / languages.length;

        int hovered = segmentAt(mouseX, mouseY);

        for (int i = 0; i < languages.length; i++) {
            float segmentX = this.x + i * (segmentWidth + gap);
            boolean selected = languages[i] == PhiI18n.current();

            // Hover tint of the segment (grows fast, fades slowly).
            this.hoverAmounts[i] = i == hovered
                    ? Math.min(1.0F, this.hoverAmounts[i] + deltaMs / HOVER_IN)
                    : Math.max(0.0F, this.hoverAmounts[i] - deltaMs / HOVER_OUT);

            this.selectedAmounts[i] = selected
                    ? Math.min(1.0F, this.selectedAmounts[i] + deltaMs / 160.0F)
                    : Math.max(0.0F, this.selectedAmounts[i] - deltaMs / 160.0F);
            this.sheens[i].update(i == hovered);

            nvg.drawSkewRect(segmentX, this.y, segmentWidth, this.height, radius,
                    PhiTheme.mix(PhiTheme.level3(), PhiTheme.level5(), this.selectedAmounts[i]));
            this.sheens[i].drawSkew(nvg, segmentX, this.y, segmentWidth, this.height, radius,
                    this.hoverAmounts[i], PhiTheme.level4());

            // The label sits on the centroid of the parallelogram, not the bounding box centre.
            float centerX = segmentX + segmentWidth * 0.5F + PhiSkew.offset(this.height) * 0.5F;
            float textY = this.y + (this.height - textSize) * 0.5F;
            float maxWidth = Math.max(0.0F, segmentWidth - 8.0F * scale);

            String label = nvg.limitText(languages[i].getDisplayName(), textSize, Fonts.REGULAR, maxWidth);
            int color = PhiTheme.mix(
                    PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), this.hoverAmounts[i]),
                    PhiTheme.accent(), this.selectedAmounts[i]);

            nvg.drawTextCentered(label, centerX, textY, color, textSize, Fonts.REGULAR);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        int segment = segmentAt(mouseX, mouseY);

        if (segment < 0) {
            return;
        }

        PhiLanguage language = PhiI18n.available()[segment];

        // The sweep restarts as the click feedback, even when the segment was already selected.
        this.sheens[segment].restart();
        PhiI18n.setLanguage(language);
    }

    /**
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     * @return the index of the segment below the pointer (-1 if there is none). The hit test follows
     *         the slanted shape of every segment.
     */
    private int segmentAt(int mouseX, int mouseY) {
        float scale = this.height / OPTION_HEIGHT;
        float gap = OPTION_GAP * scale;
        int count = PhiLanguage.values().length;
        float segmentWidth = (this.width - gap * (count - 1)) / count;

        for (int i = 0; i < count; i++) {
            float segmentX = this.x + i * (segmentWidth + gap);

            if (PhiSkew.contains(mouseX, mouseY, segmentX, this.y, segmentWidth, this.height)) {
                return i;
            }
        }

        return -1;
    }
}
