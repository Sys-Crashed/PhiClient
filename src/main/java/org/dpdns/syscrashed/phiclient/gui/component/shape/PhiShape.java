package org.dpdns.syscrashed.phiclient.gui.component.shape;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * The shape contract of a component (package
 * {@code org.dpdns.syscrashed.phiclient.gui.component.shape}).
 *
 * <p>The shape family decides everything that depends on the geometry of a component: the hit test,
 * the inscribed content rectangle, the horizontal centre of its icon and the base drawing. Hover and
 * click therefore always use the same rule - a component can no longer be drawn as one shape and
 * tested as another.</p>
 *
 * <p>There are exactly two families in the interface:</p>
 * <ul>
 *   <li>{@link PhiSkewShape} - the global right leaning parallelogram (75 / 105 degree angles), used
 *       by cards, panels, control blocks, toggles, badges and the navigation band;</li>
 *   <li>{@link PhiRoundedSquareShape} - the upright rounded rectangle of the small icon buttons (the
 *       transport buttons of the music area). A row of leaning buttons would not look even, so these
 *       stay axis aligned and use an axis aligned hit test.</li>
 * </ul>
 *
 * <p>Implementations are stateless singletons - the draw path must not allocate.</p>
 */
public interface PhiShape {

    /**
     * Tests whether a point lies inside the shape.
     *
     * @param pointX the point x.
     * @param pointY the point y.
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param w      the width of the bounding box.
     * @param h      the height of the bounding box.
     * @return true if the point is inside the shape.
     */
    boolean contains(float pointX, float pointY, float x, float y, float w, float h);

    /**
     * @param x       the left x of the bounding box.
     * @param h       the height of the bounding box.
     * @param padding the inset between the shape and its content.
     * @return the left x of the content rectangle that is inscribed in the shape.
     */
    float contentLeft(float x, float h, float padding);

    /**
     * @param x       the left x of the bounding box.
     * @param w       the width of the bounding box.
     * @param padding the inset between the shape and its content.
     * @return the right x of the content rectangle that is inscribed in the shape.
     */
    float contentRight(float x, float w, float padding);

    /**
     * @param x the left x of the bounding box.
     * @param w the width of the bounding box.
     * @param h the height of the bounding box.
     * @return the horizontal centre of the shape (for a parallelogram this is the centroid, which
     *         sits right of the bounding box centre).
     */
    float iconCenterX(float x, float w, float h);

    /**
     * Draws the base surface of the shape.
     *
     * @param nvg    the NanoVG drawing context.
     * @param x      the left x of the bounding box.
     * @param y      the top y of the bounding box.
     * @param w      the width of the bounding box.
     * @param h      the height of the bounding box.
     * @param radius the corner radius of the shape.
     * @param color  the fill color.
     */
    void draw(NanoVGManager nvg, float x, float y, float w, float h, float radius, int color);
}
