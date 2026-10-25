package org.dpdns.syscrashed.phiclient.gui.widget;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A short confirmation at the top of the game window
 * (package {@code org.dpdns.syscrashed.phiclient.gui.widget}).
 *
 * <p>The toast shows a title and a message for a couple of seconds and then fades out. It is used to
 * confirm actions that have no visible result in the interface itself (for example the theme switch).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #show(String, String)} - shows a toast.</li>
 *   <li>{@link #update()} - hides the toast when its time is over.</li>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the toast.</li>
 * </ul>
 */
public class PhiToast {

    /** How long a toast stays visible. */
    private static final float VISIBLE_MS = 2600.0F;

    /** The duration of the fade in and the fade out. */
    private static final float FADE_MS = 220.0F;

    private String title = "";

    private String message = "";

    private long startTime;

    private boolean visible;

    /**
     * Shows a toast. A new toast replaces the one that is currently shown.
     *
     * @param title   the headline of the toast.
     * @param message the text of the toast.
     */
    public void show(String title, String message) {
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.startTime = System.currentTimeMillis();
        this.visible = true;
    }

    /**
     * Hides the toast when its time is over.
     */
    public void update() {
        if (this.visible && System.currentTimeMillis() - this.startTime > VISIBLE_MS) {
            this.visible = false;
        }
    }

    /**
     * Draws the toast at the top of the game window.
     *
     * @param nvg          the NanoVG drawing context.
     * @param screenWidth  the width of the game window.
     * @param screenHeight the height of the game window.
     * @param uiScale      the design scale factor of the interface.
     */
    public void draw(NanoVGManager nvg, int screenWidth, int screenHeight, float uiScale) {
        if (!this.visible) {
            return;
        }

        float age = System.currentTimeMillis() - this.startTime;
        float alpha = 1.0F;

        if (age < FADE_MS) {
            alpha = age / FADE_MS;
        } else if (age > VISIBLE_MS - FADE_MS) {
            alpha = Math.max(0.0F, (VISIBLE_MS - age) / FADE_MS);
        }

        float padding = PhiMetrics.CARD_PADDING * uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * uiScale;
        float bodySize = PhiMetrics.FONT_BODY * uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * uiScale;

        float textWidth = Math.max(nvg.textWidth(this.title, titleSize, Fonts.REGULAR),
                nvg.textWidth(this.message, bodySize, Fonts.REGULAR));
        float height = titleSize * 1.25F + bodySize + padding * 1.6F;

        // The box is a parallelogram, so it is wider than the text by the skew as well - the text sits
        // in the content rectangle that is inscribed in the shape.
        float width = textWidth + padding * 2.0F + PhiSkew.offset(height);

        float x = (screenWidth - (width + PhiSkew.offset(height))) * 0.5F;
        float y = 26.0F * uiScale;

        float contentLeft = PhiSkew.contentLeft(x, height, padding);

        nvg.setGlobalAlpha(alpha);

        nvg.drawSkewRect(x, y, width, height, radius, PhiTheme.level3());

        nvg.drawText(this.title, contentLeft, y + padding * 0.6F, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);
        nvg.drawText(this.message, contentLeft, y + padding * 0.6F + titleSize * 1.25F,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        nvg.resetGlobalAlpha();
    }
}
