package org.dpdns.syscrashed.phiclient.gui.layout;

/**
 * Geometry of the global "right leaning parallelogram" shape
 * (package {@code org.dpdns.syscrashed.phiclient.gui.layout}).
 *
 * <p>The whole UI uses a single shape family: a parallelogram whose adjacent angles are 75 degrees
 * (top left / bottom right) and 105 degrees (top right / bottom left). The top edge is shifted to the
 * right relative to the bottom edge by {@code h / tan(75 degrees)} - approximately {@code 0.268 * h}.
 * Small elements must <b>not</b> compress this offset; it always scales with the element height.</p>
 *
 * <p>Because the offset is proportional to the height, <b>every</b> parallelogram of the UI shares the
 * same edge slope. A shape can therefore be placed flush against the slanted edge of another shape and
 * still be perfectly inscribed - this is what keeps the navigation band and the hub window aligned.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #offset(float)} - the horizontal shift of the top edge for a given height.</li>
 *   <li>{@link #corners(float, float, float, float, float[])} - the four corners of the shape.</li>
 *   <li>{@link #leftEdgeAt(float, float, float, float)} / {@link #rightEdgeAt(float, float, float, float, float)}
 *       - the horizontal position of the slanted edges at a given vertical position.</li>
 *   <li>{@link #contentLeft(float, float, float)} / {@link #contentRight(float, float, float)} - the content
 *       rectangle that is inscribed in the shape.</li>
 *   <li>{@link #contains(float, float, float, float, float, float)} - point in shape test.</li>
 * </ul>
 */
public final class PhiSkew {

    private PhiSkew() {
    }

    /** The acute angle of the parallelogram in degrees. */
    public static final float ANGLE_DEGREES = 75.0F;

    /**
     * The shear angle that produces the parallelogram: the horizontal shear of a vertical edge is
     * {@code tan(90 - 75) = 0.2679}, which is applied as a negative (left leaning) x shear.
     */
    public static final float SHEAR_DEGREES = -15.0F;

    /** Tangent of {@link #ANGLE_DEGREES} - the slope factor of every slanted edge. */
    private static final double TANGENT = Math.tan(Math.toRadians(ANGLE_DEGREES));

    /**
     * The horizontal shift per vertical pixel (the slope of every slanted edge). Because the offset
     * of a shape is proportional to its height, every edge of the interface shares this slope - which
     * is what keeps the blocks of a page aligned with each other.
     */
    public static final float SLOPE = (float) (1.0D / TANGENT);

    /**
     * @param height the height of the shape.
     * @return the horizontal distance by which the top edge is shifted to the right.
     */
    public static float offset(float height) {
        return (float) (height / TANGENT);
    }

    /**
     * Writes the four corners of the parallelogram into an array.
     *
     * <p>The order is: bottom-left, bottom-right, top-right, top-left. Each corner occupies two entries
     * ({@code x} then {@code y}), so the array must hold at least eight floats.</p>
     *
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     * @param out    the array that receives the corners.
     */
    public static void corners(float x, float y, float width, float height, float[] out) {
        float off = offset(height);

        out[0] = x;
        out[1] = y + height;
        out[2] = x + width;
        out[3] = y + height;
        out[4] = x + width + off;
        out[5] = y;
        out[6] = x + off;
        out[7] = y;
    }

    /**
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param height the height of the bounding box.
     * @param pointY the absolute vertical position to evaluate.
     * @return the horizontal position of the slanted left edge at {@code pointY}.
     */
    public static float leftEdgeAt(float x, float y, float height, float pointY) {
        if (height <= 0.0F) {
            return x;
        }

        // t = 0 at the bottom edge, t = 1 at the top edge.
        float t = (y + height - pointY) / height;
        return x + offset(height) * t;
    }

    /**
     * @param x      the left x of the bounding box.
     * @param width  the width of the bounding box.
     * @param y      the top y of the bounding box.
     * @param height the height of the bounding box.
     * @param pointY the absolute vertical position to evaluate.
     * @return the horizontal position of the slanted right edge at {@code pointY}.
     */
    public static float rightEdgeAt(float x, float width, float y, float height, float pointY) {
        return leftEdgeAt(x, y, height, pointY) + width;
    }

    /**
     * The left edge of the content rectangle of a parallelogram.
     *
     * <p>A parallelogram leans to the right, so its left edge moves to the right when the shape gets
     * higher. The only axis aligned rectangle that fits into the shape at <b>every</b> height therefore
     * starts at the top left corner of the shape. Every element of a card uses this single x, so the
     * texts of a card share one column and cannot drift apart - this is the rule that keeps the
     * interface aligned.</p>
     *
     * @param x       the left x of the bounding box.
     * @param height  the height of the shape.
     * @param padding the inset between the shape and its content.
     * @return the left x of the content rectangle.
     */
    public static float contentLeft(float x, float height, float padding) {
        return x + offset(height) + padding;
    }

    /**
     * The right edge of the content rectangle of a parallelogram.
     *
     * <p>It is the mirrored counterpart of {@link #contentLeft(float, float, float)}: the right edge of
     * the shape moves to the left when the shape gets lower, so the content rectangle ends at the
     * bottom right corner of the shape.</p>
     *
     * @param x       the left x of the bounding box.
     * @param width   the width of the bounding box.
     * @param padding the inset between the shape and its content.
     * @return the right x of the content rectangle.
     */
    public static float contentRight(float x, float width, float padding) {
        return x + width - padding;
    }

    /**
     * Tests whether a point lies inside the parallelogram.
     *
     * @param pointX the point x.
     * @param pointY the point y.
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param width  the width of the bounding box.
     * @param height the height of the bounding box.
     * @return true if the point is inside the shape.
     */
    public static boolean contains(float pointX, float pointY, float x, float y, float width, float height) {
        if (pointY < y || pointY > y + height) {
            return false;
        }

        float left = leftEdgeAt(x, y, height, pointY);
        return pointX >= left && pointX <= left + width;
    }
}
