package org.dpdns.syscrashed.phiclient.utils.animation;

import org.dpdns.syscrashed.phiclient.utils.TimerUtils;

/**
 * Generic tweening animation base class (package {@code org.dpdns.syscrashed.phiclient.utils.animation}).
 *
 * <p>Maps "elapsed time" to a normalized {@code 0..1} progress, then lets subclasses apply their own
 * easing. The curve parameter was changed from milliseconds to a normalized {@code 0..1} value
 * (named {@code t}) so the semantics are clearer and different easings are easier to write.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * Animation anim = new EaseOutCubic(280, 1.0D);
 * anim.setDirection(Direction.FORWARDS);
 * float v = anim.getValueFloat();   // 0 -&gt; 1
 * </pre>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getValue()} - the current animated value (eased, scaled by the end point).</li>
 *   <li>{@link #getValueFloat()} / {@link #getValueInt()} - the value coerced to float / int.</li>
 *   <li>{@link #getEquation(double)} - the easing curve (implemented by subclasses).</li>
 *   <li>{@link #isDone()} / {@link #isDone(Direction)} - completion checks.</li>
 *   <li>{@link #reset()} - re-bases the timer to now.</li>
 *   <li>{@link #setDirection(Direction)} - sets the play direction (re-bases the timer on change to avoid jumps).</li>
 *   <li>{@link #getDirection()} / {@link #getDuration()} / {@link #getEndPoint()} - accessors.</li>
 * </ul>
 */
public abstract class Animation {

    protected final TimerUtils timer = new TimerUtils();

    /** Full animation duration in milliseconds. */
    protected int duration;

    /** End value (usually 1.0, but may be any number). */
    protected double endPoint;

    protected Direction direction = Direction.FORWARDS;

    protected Animation(int duration, double endPoint) {
        this.duration = duration;
        this.endPoint = endPoint;
        this.timer.reset();
    }

    /**
     * @return the current animated value. With {@code FORWARDS} it goes from 0 to {@code endPoint};
     *         with {@code BACKWARDS} it goes the other way.
     */
    public double getValue() {
        long elapsed = Math.min(this.duration, this.timer.getElapsedTime());
        double t = this.duration <= 0 ? 1.0D : (double) elapsed / (double) this.duration;

        double eased = getEquation(t);
        if (this.direction == Direction.BACKWARDS) {
            eased = 1.0D - eased;
        }
        return eased * this.endPoint;
    }

    /**
     * @return {@link #getValue()} coerced to a float.
     */
    public float getValueFloat() {
        return (float) getValue();
    }

    /**
     * @return {@link #getValue()} coerced to an int.
     */
    public int getValueInt() {
        return (int) getValue();
    }

    /**
     * The easing curve: maps a normalized progress {@code 0..1} to an eased progress {@code 0..1}.
     *
     * @param t the normalized progress in {@code [0, 1]}.
     * @return the eased progress in {@code [0, 1]}.
     */
    protected abstract double getEquation(double t);

    /**
     * @return true once the animation duration has fully elapsed.
     */
    public boolean isDone() {
        return this.timer.getElapsedTime() >= this.duration;
    }

    /**
     * @param direction the direction to test against.
     * @return true if the animation is finished and is currently playing in {@code direction}.
     */
    public boolean isDone(Direction direction) {
        return isDone() && this.direction == direction;
    }

    /**
     * Re-bases the internal timer to the current time (restart the animation from the beginning).
     */
    public void reset() {
        this.timer.reset();
    }

    /**
     * Sets the play direction. On change, the timer origin is shifted to "already-elapsed progress"
     * so the animation does not jump.
     *
     * @param direction the new direction.
     */
    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            long elapsed = Math.min(this.duration, this.timer.getElapsedTime());
            this.timer.setLastMs(System.currentTimeMillis() - elapsed);
        }
    }

    /**
     * @return the current play direction.
     */
    public Direction getDirection() {
        return this.direction;
    }

    /**
     * @return the full animation duration in milliseconds.
     */
    public int getDuration() {
        return this.duration;
    }

    /**
     * @return the configured end value.
     */
    public double getEndPoint() {
        return this.endPoint;
    }
}
