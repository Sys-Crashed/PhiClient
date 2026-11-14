package org.dpdns.syscrashed.phiclient.gui.util;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The hover feedback of the interface: a soft sheen that sweeps along the 75 degree axis of the
 * global shape, followed by a faint tint while the pointer stays inside
 * (package {@code org.dpdns.syscrashed.phiclient.gui.util}).
 *
 * <p>The sheen replaces the former circular reveal. It is drawn in the sheared coordinate system of
 * the shape, where both the shape and the highlight band are rectangles. The band therefore stays
 * inside the shape without relying on a scissor rectangle. A sweep that has already started always
 * plays to the end, so a pointer that only passes over a component still produces visible feedback.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #update(boolean)} / {@link #restart()} - drives the sweep.</li>
 *   <li>{@link #drawSkew(NanoVGManager, float, float, float, float, float, float, int)} - draws the
 *       tint and the sheen of a parallelogram.</li>
 * </ul>
 */
public final class PhiSheen {

    /** Duration of one sweep in milliseconds. */
    private static final float SWEEP_MS = 260.0F;

    /** Height of the highlight band relative to the height of the shape. */
    private static final float BAND_HEIGHT = 0.95F;

    /** Opacity of the tint when the hover progress is 1. */
    private static final float TINT_ALPHA = 0.50F;

    /** Opacity at the centre of the highlight band. */
    private static final float SHEEN_ALPHA = 0.80F;

    /** The moment the current sweep started (0 = no sweep). */
    private long sweepStart;

    private boolean hovered;

    /**
     * Starts a sweep when the pointer enters the component.
     *
     * @param hovered whether the pointer is currently inside the component.
     */
    public void update(boolean hovered) {
        if (hovered && !this.hovered) {
            restart();
        }

        this.hovered = hovered;
    }

    /** Restarts the sweep (used when a shared highlight moves to another entry). */
    public void restart() {
        this.sweepStart = System.currentTimeMillis();
    }

    /**
     * Draws the hover feedback of a parallelogram of the global shape.
     *
     * @param nvg    the NanoVG drawing context.
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the shape.
     * @param height the height of the shape.
     * @param radius the corner radius of the shape.
     * @param amount the hover progress of the tint (0 = no tint, 1 = fully hovered).
     * @param color  the base color of the feedback.
     */
    public void drawSkew(NanoVGManager nvg, float x, float y, float width, float height, float radius,
                         float amount, int color) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        if (amount > 0.001F) {
            nvg.drawSkewRect(x, y, width, height, radius,
                    PhiTheme.withAlpha(color, amount * TINT_ALPHA));
        }

        float sweep = getSweep();

        if (sweep >= 1.0F) {
            return;
        }

        nvg.save();
        nvg.translate(x + PhiSkew.offset(height), y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);

        drawBand(nvg, width, height, sweep, color);

        nvg.restore();
    }

    /**
     * @return the progress of the current sweep (1 = finished, nothing to draw).
     */
    private float getSweep() {
        if (this.sweepStart == 0L) {
            return 1.0F;
        }

        float progress = (System.currentTimeMillis() - this.sweepStart) / SWEEP_MS;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    /**
     * Draws the highlight band in the sheared coordinate system. Two linear gradients form a band
     * that is transparent at both ends and brightest at its centre.
     */
    private static void drawBand(NanoVGManager nvg, float width, float height, float sweep, int color) {
        float band = height * BAND_HEIGHT;
        float eased = 1.0F - (float) Math.pow(1.0F - sweep, 3.0D);
        float center = -band * 0.5F + (height + band) * eased;

        int transparent = PhiTheme.withAlpha(color, 0.0F);
        int bright = PhiTheme.withAlpha(color, SHEEN_ALPHA);

        bandHalf(nvg, width, height, center - band * 0.5F, center, transparent, bright);
        bandHalf(nvg, width, height, center, center + band * 0.5F, bright, transparent);
    }

    /**
     * Fills one half of the band. The path is clipped to the shape rectangle, while the gradient
     * keeps its unclipped end points so the brightness curve does not change at the shape edges.
     */
    private static void bandHalf(NanoVGManager nvg, float width, float height,
                                 float fromY, float toY, int fromColor, int toColor) {
        float top = Math.max(0.0F, fromY);
        float bottom = Math.min(height, toY);

        if (bottom - top <= 0.01F || toY - fromY <= 0.01F) {
            return;
        }

        nvg.beginPath();
        nvg.moveTo(0.0F, top);
        nvg.lineTo(width, top);
        nvg.lineTo(width, bottom);
        nvg.lineTo(0.0F, bottom);
        nvg.closePath();
        nvg.fillPathGradient(fromColor, toColor, 0.0F, fromY, 0.0F, toY);
    }
}
