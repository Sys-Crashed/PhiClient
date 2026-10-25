package org.dpdns.syscrashed.phiclient.utils;

/**
 * Minimal timer utility (package {@code org.dpdns.syscrashed.phiclient.utils}).
 *
 * <p>Records the timestamp of the last {@link #reset()} and exposes helpers to ask "has N
 * milliseconds elapsed?" and "how many milliseconds have elapsed?". Only the parts needed by the
 * animation system (timing / frame-interval measurement) are kept; clipboard / file related
 * functionality from the original helper has been removed.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #reset()} - resets the timer to the current time.</li>
 *   <li>{@link #delay(long)} - returns true if at least {@code nextDelay} ms have elapsed (no reset).</li>
 *   <li>{@link #delay(long, boolean)} - like {@link #delay(long)} but optionally resets the timer.</li>
 *   <li>{@link #getElapsedTime()} - milliseconds since the last reset.</li>
 *   <li>{@link #getLastMs()} / {@link #setLastMs(long)} - direct access to the stored timestamp.</li>
 * </ul>
 */
public final class TimerUtils {

    private long lastMs;

    public TimerUtils() {
        this.lastMs = System.currentTimeMillis();
    }

    /**
     * Resets the timer to the current system time.
     */
    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }

    /**
     * Checks whether at least {@code nextDelay} milliseconds have elapsed since the last reset.
     * Does not reset the timer.
     *
     * @param nextDelay the delay threshold in milliseconds.
     * @return true if the elapsed time is greater than or equal to {@code nextDelay}.
     */
    public boolean delay(long nextDelay) {
        return System.currentTimeMillis() - this.lastMs >= nextDelay;
    }

    /**
     * Checks whether at least {@code nextDelay} milliseconds have elapsed, optionally resetting
     * the timer when the threshold is met.
     *
     * @param nextDelay the delay threshold in milliseconds.
     * @param reset     if true, resets the timer when the threshold is met.
     * @return true if the elapsed time is greater than or equal to {@code nextDelay}.
     */
    public boolean delay(long nextDelay, boolean reset) {
        if (System.currentTimeMillis() - this.lastMs >= nextDelay) {
            if (reset) {
                this.reset();
            }
            return true;
        }
        return false;
    }

    /**
     * @return milliseconds elapsed since the last reset.
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMs;
    }

    /**
     * @return the timestamp recorded by the last reset.
     */
    public long getLastMs() {
        return this.lastMs;
    }

    /**
     * Overrides the stored timestamp (used to re-base the timer, e.g. when an animation reverses).
     *
     * @param lastMs the new base timestamp in milliseconds.
     */
    public void setLastMs(long lastMs) {
        this.lastMs = lastMs;
    }
}
