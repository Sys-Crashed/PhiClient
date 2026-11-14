package org.dpdns.syscrashed.phiclient.gui.component.capability;

/**
 * The unified hover state machine of the interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.capability}).
 *
 * <p>One machine drives the tint of every hover feedback, with its timings passed in as parameters.
 * The two observations of the interface are:</p>
 * <ul>
 *   <li>the components: {@code (120 ms in, 420 ms out, 0.12 min start, 0.2 restart)};</li>
 *   <li>the navigation band: {@code (110 ms in, 380 ms out, 0.18 min start, 0.2 restart)}.</li>
 * </ul>
 *
 * <p>The progress grows quickly while the pointer is inside and fades out slowly afterwards, so the
 * feedback stays visible long enough to be seen even when the pointer only passes over the component
 * for a moment. When the pointer re-enters a component whose progress has almost faded out, the
 * progress restarts at the new position of the pointer - otherwise the highlight would flicker during
 * a quick movement over it.</p>
 */
public final class PhiHoverRipple {

    /** How long the progress takes to grow to 1 (milliseconds). */
    private final float inMs;

    /** How long the progress takes to fade out again (milliseconds). */
    private final float outMs;

    /** The progress a new highlight starts with, so a quick movement is still visible. */
    private final float minStart;

    /** Below this progress the highlight restarts at the new position of the pointer. */
    private final float restart;

    /** Whether the highlight keeps drawing while it fades out after the pointer has left. */
    private final boolean keepRevealOnLeave;

    /** The point where the pointer entered (the centre of the highlight). */
    private float originX;

    private float originY;

    /** The progress of the highlight (1 = fully hovered). */
    private float progress;

    /** The time of the last frame in which the state was updated. */
    private long lastTime;

    /** Whether the pointer is currently inside. */
    private boolean inside;

    /**
     * @param inMs               how long the progress takes to grow to 1 (milliseconds).
     * @param outMs              how long the progress takes to fade out (milliseconds).
     * @param minStart           the progress a new highlight starts with.
     * @param restart            below this progress the highlight restarts at the new position.
     * @param keepRevealOnLeave  whether the highlight keeps drawing while it fades out after the
     *                           pointer has left (false clears it immediately).
     */
    public PhiHoverRipple(float inMs, float outMs, float minStart, float restart,
                          boolean keepRevealOnLeave) {
        this.inMs = inMs;
        this.outMs = outMs;
        this.minStart = minStart;
        this.restart = restart;
        this.keepRevealOnLeave = keepRevealOnLeave;
    }

    /**
     * Advances the state machine. The first frame is treated as 16 ms, and a gap between two frames
     * is capped at 100 ms, so returning to the window cannot make the progress jump.
     *
     * @param mouseX the current pointer x.
     * @param mouseY the current pointer y.
     * @param inside whether the pointer is currently inside the component.
     */
    public void update(int mouseX, int mouseY, boolean inside) {
        long now = System.currentTimeMillis();
        float deltaMs = this.lastTime == 0L ? 16.0F : Math.min(100.0F, now - this.lastTime);
        this.lastTime = now;

        if (inside && !this.inside) {
            // The pointer entered the component. The highlight only restarts at the new position
            // when it has almost faded out - otherwise a quick movement makes it flicker.
            if (this.progress < this.restart) {
                this.originX = mouseX;
                this.originY = mouseY;

                // The highlight does not start at a single point, so it is also visible when the
                // pointer only passes over the component for a very short time.
                this.progress = Math.max(this.progress, this.minStart);
            }
        }

        this.inside = inside;

        if (!inside && !this.keepRevealOnLeave) {
            this.progress = 0.0F;
            return;
        }

        // The highlight grows quickly and fades out slowly.
        float speed = inside ? deltaMs / this.inMs : -deltaMs / this.outMs;
        this.progress = Math.max(0.0F, Math.min(1.0F, this.progress + speed));
    }

    /**
     * @return the progress of the highlight in the range {@code [0, 1]}.
     */
    public float getProgress() {
        return this.progress;
    }

    /**
     * @return the x of the point where the pointer entered.
     */
    public float getOriginX() {
        return this.originX;
    }

    /**
     * @return the y of the point where the pointer entered.
     */
    public float getOriginY() {
        return this.originY;
    }

    /**
     * @return true if the pointer is currently inside.
     */
    public boolean isInside() {
        return this.inside;
    }
}
