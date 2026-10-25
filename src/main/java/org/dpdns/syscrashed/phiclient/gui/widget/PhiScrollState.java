package org.dpdns.syscrashed.phiclient.gui.widget;

/**
 * Scroll state of a scrollable list (package {@code org.dpdns.syscrashed.phiclient.gui.widget}).
 *
 * <p>The behavior follows the design specification: there is no visible scroll bar, the list scrolls
 * with the mouse wheel, the movement keeps its inertia (a decay factor of 0.92 per frame) and the
 * boundaries are rubber banded instead of being hard stops.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #setContentSize(float, float)} - updates the content and viewport height.</li>
 *   <li>{@link #scrollBy(float)} - applies a wheel delta.</li>
 *   <li>{@link #update(float)} - advances the inertia and the rubber band.</li>
 *   <li>{@link #getOffset()} / {@link #getMaxOffset()} - the current scroll position.</li>
 * </ul>
 */
public class PhiScrollState {

    /** Inertia decay per 60 Hz frame. */
    private static final float DECAY = 0.92F;

    /** Resistance applied while the list is dragged beyond its bounds. */
    private static final float OVERSCROLL_RESISTANCE = 0.35F;

    /** A scroll velocity below this value is considered "stopped". */
    private static final float MIN_VELOCITY = 0.05F;

    private float offset;
    private float velocity;

    private float contentHeight;
    private float viewportHeight;

    /**
     * Updates the sizes of the scrollable content.
     *
     * @param contentHeight  the total height of the content.
     * @param viewportHeight the visible height of the list.
     */
    public void setContentSize(float contentHeight, float viewportHeight) {
        this.contentHeight = contentHeight;
        this.viewportHeight = viewportHeight;
    }

    /**
     * Applies a scroll delta (positive scrolls towards the end of the list).
     *
     * @param delta the scroll distance in pixels.
     */
    public void scrollBy(float delta) {
        float target = this.offset + delta;

        if (target < 0.0F || target > getMaxOffset()) {
            // Rubber band: only a part of the delta is applied outside of the valid range.
            target = this.offset + delta * OVERSCROLL_RESISTANCE;
        }

        this.offset = target;
        this.velocity = delta;
    }

    /**
     * Advances the inertia and pulls the list back into its valid range.
     *
     * @param deltaMs the time in milliseconds since the previous frame.
     */
    public void update(float deltaMs) {
        float frames = Math.max(0.0F, deltaMs) / 16.6667F;

        if (this.offset < 0.0F || this.offset > getMaxOffset()) {
            // Rubber band: spring back towards the valid range.
            float clamped = clamp(this.offset, 0.0F, getMaxOffset());
            float factor = Math.min(1.0F, Math.max(0.0F, deltaMs) / 80.0F);
            this.offset += (clamped - this.offset) * factor;
            this.velocity = 0.0F;
            return;
        }

        this.offset += this.velocity * frames;
        this.velocity *= (float) Math.pow(DECAY, frames);

        if (Math.abs(this.velocity) < MIN_VELOCITY) {
            this.velocity = 0.0F;
        }
    }

    /**
     * @return the current scroll offset (0 = the top of the list is visible).
     */
    public float getOffset() {
        return this.offset;
    }

    /**
     * @return the maximum scroll offset of the list.
     */
    public float getMaxOffset() {
        return Math.max(0.0F, this.contentHeight - this.viewportHeight);
    }

    /**
     * Resets the scroll position and the inertia.
     */
    public void reset() {
        this.offset = 0.0F;
        this.velocity = 0.0F;
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
}
