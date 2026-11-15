package org.dpdns.syscrashed.phiclient.gui.util;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The hover feedback of the interface: a soft sheen that sweeps horizontally across the global
 * shape, followed by a faint tint while the pointer stays inside
 * (package {@code org.dpdns.syscrashed.phiclient.gui.util}).
 *
 * <p>The sheen replaces the former circular reveal. It is drawn in the sheared coordinate system of
 * the shape, where both the shape and the highlight band are rounded rectangles. A band end is
 * either a full shape-radius corner or a round capsule head, so it never protrudes with sharp
 * corners, and the gradients fade out exactly at the shape edges. The two gradient halves meet on
 * one straight seam, forming a single continuous band with no corner gap at its centre. A sweep
 * that has already started always plays to the end, so a pointer that only passes over a component
 * still produces visible feedback.</p>
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

    /** Width of the highlight band relative to the width of the shape. */
    private static final float BAND_WIDTH = 0.55F;

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

        drawBand(nvg, width, height, radius, sweep, color);

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
     * that is transparent at both ends and brightest at its centre. The band travels horizontally;
     * the shear turns its vertical sides into edges parallel to the slanted sides of the shape.
     *
     * <p>The visible part of the band spans the whole shape and its two gradient halves are two
     * paths that meet on one straight seam (zero corner radius there) - a scissor would be clipped
     * through the sheared transform and cut a visible wedge out of the centre. A band end is either
     * a full shape-radius corner or a round capsule head (half the band width), never a protruding
     * sharp corner, and the gradients end exactly at the clamped band edges, so the band always
     * fades out at the shape edge.</p>
     */
    private static void drawBand(NanoVGManager nvg, float width, float height, float radius,
                                 float sweep, int color) {
        float band = width * BAND_WIDTH;
        float eased = 1.0F - (float) Math.pow(1.0F - sweep, 3.0D);
        float center = -band * 0.5F + (width + band) * eased;

        float left = Math.max(0.0F, center - band * 0.5F);
        float right = Math.min(width, center + band * 0.5F);

        if (right - left <= 0.01F) {
            return;
        }

        int transparent = PhiTheme.withAlpha(color, 0.0F);
        int bright = PhiTheme.withAlpha(color, SHEEN_ALPHA);

        float cap = Math.min(radius, (right - left) * 0.5F);
        float mid = Math.max(left, Math.min(right, center));

        // Leading half: transparent at the band start, bright at the centre.
        if (mid - left > 0.01F) {
            nvg.beginPath();
            nvg.addRoundedRectVarying(left, 0.0F, mid - left, height, cap, 0.0F, 0.0F, cap);
            nvg.fillPathGradient(transparent, bright, left, 0.0F, mid, 0.0F);
        }
        // Trailing half: bright at the centre, transparent at the band end.
        if (right - mid > 0.01F) {
            nvg.beginPath();
            nvg.addRoundedRectVarying(mid, 0.0F, right - mid, height, 0.0F, cap, cap, 0.0F);
            nvg.fillPathGradient(bright, transparent, mid, 0.0F, right, 0.0F);
        }
    }
}
