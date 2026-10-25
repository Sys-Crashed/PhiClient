package org.dpdns.syscrashed.phiclient.gui.util;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A circular reveal that spreads from a point inside a shape of the global shape family
 * (package {@code org.dpdns.syscrashed.phiclient.gui.util}).
 *
 * <p>The reveal is used for the hover and the press feedback of the navigation entries: the highlight
 * starts at the position of the pointer and grows until it covers the whole entry. The shape of the
 * entry is a rounded parallelogram, so the circle is drawn in the sheared coordinate system of the
 * shape and clipped to it - that way the highlight always stays inside the entry.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #drawReveal(NanoVGManager, float, float, float, float, float, float, float, float, int)}
 *       - draws the reveal.</li>
 * </ul>
 */
public final class PhiRipple {

    private PhiRipple() {
    }

    /**
     * Draws a circular reveal inside a parallelogram of the global shape.
     *
     * @param nvg      the NanoVG drawing context.
     * @param x        the left x of the bounding box of the shape.
     * @param y        the top y of the bounding box of the shape.
     * @param width    the width of the shape.
     * @param height   the height of the shape.
     * @param radius   the corner radius of the shape.
     * @param originX  the x of the point the reveal spreads from.
     * @param originY  the y of the point the reveal spreads from.
     * @param amount   the progress of the reveal (0 = invisible, 1 = the whole shape is covered).
     * @param color    the fill color of the reveal.
     */
    public static void drawReveal(NanoVGManager nvg, float x, float y, float width, float height, float radius,
                                  float originX, float originY, float amount, int color) {
        if (amount <= 0.001F || width <= 0.0F || height <= 0.0F) {
            return;
        }

        // The shape is drawn in the sheared coordinate system, so the point of the pointer has to be
        // converted back into it (the shear moves the x by the tangent times the height).
        float shift = PhiSkew.offset(height);
        float localY = originY - y;
        float localX = (originX - (x + shift)) + PhiSkew.SLOPE * localY;

        // The radius grows until the circle covers the whole shape.
        float maxRadius = (float) Math.sqrt(width * width + height * height);
        float revealRadius = amount * maxRadius;

        nvg.save();
        nvg.translate(x + shift, y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);

        // The shape is filled with a radial gradient that is opaque at the pointer and transparent at
        // the edge of the reveal - the highlight therefore never leaves the shape.
        nvg.beginPath();
        nvg.addRoundedRect(0.0F, 0.0F, width, height, radius);
        nvg.fillPathRadialGradient(localX, localY, revealRadius, color, PhiTheme.withAlpha(color, 0.0F));

        nvg.restore();
    }
}
