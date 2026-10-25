package org.dpdns.syscrashed.phiclient.utils.mouse;

/**
 * Mouse hit-testing helpers (package {@code org.dpdns.syscrashed.phiclient.utils.mouse}).
 *
 * <p>Provides common UI-interaction utilities: point-in-rectangle tests and value clamping used
 * by the GUI components and drag handling.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #isInside(float, float, float, float, float, float)} - float point-in-rect test.</li>
 *   <li>{@link #isInside(int, int, int, int, int, int)} - int point-in-rect test (delegates to the float variant).</li>
 *   <li>{@link #clamp(float, float, float)} - clamps a value into the inclusive {@code [min, max]} range.</li>
 * </ul>
 */
public final class MouseUtils {

    private MouseUtils() {
    }

    /**
     * Tests whether a point lies inside an axis-aligned rectangle.
     *
     * @param mouseX the point x coordinate.
     * @param mouseY the point y coordinate.
     * @param x      the rectangle's top-left x.
     * @param y      the rectangle's top-left y.
     * @param width  the rectangle width.
     * @param height the rectangle height.
     * @return true if the point is inside or on the rectangle's edge.
     */
    public static boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Integer overload of {@link #isInside(float, float, float, float, float, float)}.
     * Values are widened to float and forwarded to the float variant.
     *
     * @param mouseX the point x coordinate.
     * @param mouseY the point y coordinate.
     * @param x      the rectangle's top-left x.
     * @param y      the rectangle's top-left y.
     * @param width  the rectangle width.
     * @param height the rectangle height.
     * @return true if the point is inside or on the rectangle's edge.
     */
    public static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return isInside((float) mouseX, (float) mouseY, (float) x, (float) y, (float) width, (float) height);
    }

    /**
     * Clamps a value into the inclusive {@code [min, max]} range. Useful while dragging.
     *
     * @param value the value to clamp.
     * @param min   the lower bound.
     * @param max   the upper bound.
     * @return the clamped value.
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
