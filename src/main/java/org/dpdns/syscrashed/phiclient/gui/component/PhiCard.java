package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.lwjgl.nanovg.NanoVG;

/**
 * A content card of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The card uses the global shape of the interface (a parallelogram with 75 / 105 degree angles) and
 * is drawn without any outline: the hover state is expressed by a brighter background level only.</p>
 *
 * <p><b>Layout rule</b> (the same rule the Soar client uses): a card owns exactly <b>one</b> content
 * column. The column is the rectangle that is inscribed in the parallelogram, so every text of the card
 * starts at the same x - the headline and the description below it can never drift apart, no matter how
 * high they are placed. Every text is shortened to the width of the column, so it can never leave the
 * card.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the card and its texts.</li>
 * </ul>
 */
public class PhiCard extends PhiComponent {

    private final String title;
    private final String description;
    private final String status;

    private final float uiScale;

    /** An optional widget that is drawn on the card (for example a toggle switch). */
    private PhiComponent trailing;

    /**
     * @param x          the left x of the bounding box.
     * @param y          the top y of the bounding box.
     * @param width      the width of the bounding box.
     * @param height     the height of the bounding box.
     * @param uiScale    the design scale factor of the interface.
     * @param title      the headline of the card.
     * @param description the secondary line below the headline (may be null).
     * @param status     the right aligned status text (may be null).
     */
    public PhiCard(float x, float y, float width, float height, float uiScale, String title, String description, String status) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    /**
     * Attaches a widget to the card. The widget is drawn on top of the card and replaces the status
     * text; its position is managed by the page that owns the card.
     *
     * @param trailing the widget (may be null).
     */
    public void setTrailing(PhiComponent trailing) {
        this.trailing = trailing;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        // The card is a parallelogram, so it needs its own hit test instead of the rectangular one.
        setHovered(PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height));

        int background = isHovered() ? PhiTheme.level4() : PhiTheme.level3();
        nvg.drawSkewRect(this.x, this.y, this.width, this.height,
                PhiMetrics.CORNER_RADIUS * this.uiScale, background);

        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        // The one content column of the card.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = contentRight - contentLeft;

        float titleY = this.y + padding * 0.85F;
        String headline = nvg.limitText(this.title, titleSize, Fonts.REGULAR, contentWidth);

        nvg.drawText(headline, contentLeft, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        if (this.description != null && !this.description.isEmpty()) {
            float descriptionY = titleY + titleSize * 1.35F;
            String text = nvg.limitText(this.description, bodySize, Fonts.REGULAR, contentWidth);

            nvg.drawText(text, contentLeft, descriptionY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
        }

        if (this.trailing != null) {
            this.trailing.draw(nvg, mouseX, mouseY, partialTicks);
            return;
        }

        if (this.status != null && !this.status.isEmpty()) {
            // The status shares the row of the headline, so it must not run into it.
            float headlineWidth = nvg.textWidth(headline, titleSize, Fonts.REGULAR);
            float statusWidth = contentWidth - headlineWidth - padding * 0.8F;

            if (statusWidth > bodySize) {
                String text = nvg.limitText(this.status, bodySize, Fonts.REGULAR, statusWidth);

                nvg.drawTextAligned(text, contentRight, titleY,
                        NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, PhiTheme.accent(), bodySize, Fonts.REGULAR);
            }
        }
    }
}
