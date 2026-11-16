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
 *   <li>{@link #setOffsetRange(float, float)} - sets explicit scroll bounds (dial style lists).</li>
 *   <li>{@link #scrollBy(float)} - applies a wheel delta.</li>
 *   <li>{@link #setOffset(float)} - moves the scroll position directly (snap animations).</li>
 *   <li>{@link #update(float)} - advances the inertia and the rubber band.</li>
 *   <li>{@link #getOffset()} / {@link #getMaxOffset()} / {@link #getVelocity()} - the current state.</li>
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

    /** Duration of the eased wheel step in milliseconds. */
    private static final long WHEEL_ANIMATION_MS = 180L;

    /** The start of the running wheel glide (0 = no glide). */
    private long animStart;

    private float animFrom;
    private float animTo;

    private float contentHeight;
    private float viewportHeight;

    /** The lower bound of the scroll range (usually 0, negative for dial style lists). */
    private float minOffset;

    /** An explicit upper bound of the scroll range (-1 = derive it from the content sizes). */
    private float explicitMax = -1.0F;

    /**
     * Updates the sizes of the scrollable content. The valid range of the scroll offset is reset to
     * {@code [0, contentHeight - viewportHeight]}.
     *
     * @param contentHeight  the total height of the content.
     * @param viewportHeight the visible height of the list.
     */
    public void setContentSize(float contentHeight, float viewportHeight) {
        this.contentHeight = contentHeight;
        this.viewportHeight = viewportHeight;
        this.minOffset = 0.0F;
        this.explicitMax = -1.0F;
    }

    /**
     * Sets the valid range of the scroll offset explicitly. It is used by dial style lists, whose
     * offset may start below zero so that the first entry can be centered.
     *
     * @param min the lowest valid offset.
     * @param max the highest valid offset.
     */
    public void setOffsetRange(float min, float max) {
        this.minOffset = Math.min(min, max);
        this.explicitMax = Math.max(min, max);
    }

    /**
     * Applies a scroll delta (positive scrolls towards the end of the list).
     *
     * @param delta the scroll distance in pixels.
     */
    public void scrollBy(float delta) {
        this.animStart = 0L;
        float target = this.offset + delta;

        if (target < this.minOffset || target > getMaxOffset()) {
            // Rubber band: only a part of the delta is applied outside of the valid range.
            target = this.offset + delta * OVERSCROLL_RESISTANCE;
        }

        this.offset = target;
        this.velocity = delta;
    }

    /**
     * Scrolls by the given delta with a CubicOut ease: the list glides to its new position instead
     * of jumping there. A wheel notch while the glide runs accumulates onto the running target, so
     * quick successive notches chain into one smooth movement.
     *
     * @param delta the scroll distance in pixels.
     */
    public void wheelStep(float delta) {
        float base = this.animStart != 0L ? this.animTo : this.offset;
        this.animFrom = this.offset;
        this.animTo = clamp(base + delta, this.minOffset, getMaxOffset());
        this.animStart = System.currentTimeMillis();
        this.velocity = 0.0F;
    }

    /**
     * Moves the scroll position directly to the given offset and stops the inertia. It is used by
     * snap animations, which control the offset of a dial style list frame by frame.
     *
     * @param offset the new scroll offset, clamped to the valid range.
     */
    public void setOffset(float offset) {
        this.animStart = 0L;
        this.offset = clamp(offset, this.minOffset, getMaxOffset());
        this.velocity = 0.0F;
    }

    /**
     * Advances the inertia and pulls the list back into its valid range.
     *
     * @param deltaMs the time in milliseconds since the previous frame.
     */
    public void update(float deltaMs) {
        if (this.animStart != 0L) {
            // The eased wheel glide owns the offset until it has finished.
            float t = Math.min(1.0F,
                    (System.currentTimeMillis() - this.animStart) / (float) WHEEL_ANIMATION_MS);
            float eased = 1.0F - (float) Math.pow(1.0F - t, 3.0D);
            this.offset = this.animFrom + (this.animTo - this.animFrom) * eased;
            this.velocity = 0.0F;
            if (t >= 1.0F) {
                this.animStart = 0L;
            }
            return;
        }

        float frames = Math.max(0.0F, deltaMs) / 16.6667F;

        if (this.offset < this.minOffset || this.offset > getMaxOffset()) {
            // Rubber band: spring back towards the valid range.
            float clamped = clamp(this.offset, this.minOffset, getMaxOffset());
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
        if (this.explicitMax >= 0.0F) {
            return this.explicitMax;
        }

        return Math.max(0.0F, this.contentHeight - this.viewportHeight);
    }

    /**
     * @return the current scroll velocity (0 = the list is stopped).
     */
    public float getVelocity() {
        return this.velocity;
    }

    /**
     * Resets the scroll position and the inertia.
     */
    public void reset() {
        this.animStart = 0L;
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
