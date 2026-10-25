package org.dpdns.syscrashed.phiclient.gui.widget;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The hover tooltip of the PhiClient interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.widget}).
 *
 * <p>Behavior (design specification): the tooltip appears 0.2 s after the pointer entered its anchor,
 * disappears 0.2 s after the pointer left it, fades in over 0.15 s while travelling 6 px upwards and
 * is placed to the <b>right</b> of the icon (vertically centered on it). When the pointer leaves, the
 * tooltip keeps its position and fades out in place instead of following the mouse. It may overlap the
 * hub window but never leaves the game window.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #update(String, boolean, float)} - advances the hover delay and the fade.</li>
 *   <li>{@link #isVisible()} - whether the tooltip currently draws anything.</li>
 *   <li>{@link #draw(NanoVGManager, float, float, int, int, float)} - renders the tooltip.</li>
 * </ul>
 */
public class PhiTooltip {

    /** Minimum distance between the tooltip and the border of the game window. */
    private static final float SCREEN_MARGIN = 4.0F;

    private String text = "";

    private boolean hovering;
    private float hoverTime;
    private float exitTime;
    private float progress;

    /**
     * Advances the hover delay and the fade animation.
     *
     * @param text     the text of the current anchor (may be empty when nothing is hovered).
     * @param hovering whether the pointer currently hovers the anchor.
     * @param deltaMs  the time in milliseconds since the previous frame.
     */
    public void update(String text, boolean hovering, float deltaMs) {
        boolean anchorChanged = hovering && (this.text == null || !this.text.equals(text));

        if (anchorChanged) {
            this.text = text;
            this.hoverTime = 0.0F;
            this.exitTime = 0.0F;
            this.progress = 0.0F;
        }

        if (hovering) {
            this.hovering = true;
            this.hoverTime += Math.max(0.0F, deltaMs);
            this.exitTime = 0.0F;

            if (this.hoverTime >= PhiMetrics.TOOLTIP_HOVER_DELAY) {
                this.progress += (1.0F - this.progress) * fadeFactor(deltaMs);
            }
        } else {
            this.hovering = false;
            this.hoverTime = 0.0F;
            this.exitTime += Math.max(0.0F, deltaMs);

            if (this.exitTime >= PhiMetrics.TOOLTIP_EXIT_DELAY) {
                this.progress += (0.0F - this.progress) * fadeFactor(deltaMs);
            }
        }

        if (!hovering && this.progress <= 0.0F) {
            this.text = "";
        }
    }

    /**
     * @return true while the tooltip is (partially) visible.
     */
    public boolean isVisible() {
        return this.progress > 0.01F && this.text != null && !this.text.isEmpty();
    }

    /**
     * Renders the tooltip next to its anchor.
     *
     * @param nvg          the NanoVG drawing context.
     * @param anchorX      the horizontal center of the anchor (usually the icon).
     * @param anchorY      the vertical center of the anchor.
     * @param screenWidth  the width of the game window.
     * @param screenHeight the height of the game window.
     * @param uiScale      the design scale factor of the interface.
     */
    public void draw(NanoVGManager nvg, float anchorX, float anchorY, int screenWidth, int screenHeight, float uiScale) {
        if (!isVisible()) {
            return;
        }

        float fontSize = PhiMetrics.FONT_TOOLTIP * uiScale;
        float padding = PhiMetrics.TOOLTIP_PADDING * uiScale;
        float height = PhiMetrics.TOOLTIP_HEIGHT * uiScale;
        float gap = PhiMetrics.TOOLTIP_GAP * uiScale;
        float travel = PhiMetrics.TOOLTIP_OFFSET_Y * uiScale * (1.0F - this.progress);
        float width = nvg.textWidth(this.text, fontSize, Fonts.REGULAR) + padding * 2.0F;

        float margin = SCREEN_MARGIN * uiScale;

        // The tooltip is placed to the right of the icon and vertically centered on it.
        float left = anchorX + gap;
        float top = anchorY - height * 0.5F + travel;

        if (left + width > screenWidth - margin) {
            // Not enough room on the right: show the tooltip on the left of the icon instead.
            left = anchorX - gap - width;
        }

        left = clamp(left, margin, screenWidth - width - margin);
        top = clamp(top, margin, screenHeight - height - margin);

        nvg.drawSkewRect(left, top, width, height, PhiMetrics.CORNER_RADIUS * uiScale,
                PhiTheme.withAlpha(PhiTheme.level5(), this.progress));

        float textY = top + (height - fontSize) * 0.5F - fontSize * 0.08F;
        float textX = PhiSkew.leftEdgeAt(left, top, height, textY) + padding;
        nvg.drawText(this.text, textX, textY, PhiTheme.withAlpha(PhiTheme.textPrimary(), this.progress),
                fontSize, Fonts.REGULAR);
    }

    /**
     * Clamps a value into the inclusive {@code [min, max]} range.
     *
     * @param value the value to clamp.
     * @param min   the lower bound.
     * @param max   the upper bound.
     * @return the clamped value.
     */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    /**
     * @param deltaMs the frame time.
     * @return the interpolation factor of the fade animation.
     */
    private static float fadeFactor(float deltaMs) {
        return Math.min(1.0F, Math.max(0.0F, deltaMs) / PhiMetrics.TOOLTIP_FADE);
    }
}
