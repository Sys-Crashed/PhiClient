package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * Ease-in cubic interpolation (package {@code org.dpdns.syscrashed.phiclient.utils.animation}).
 *
 * <p>Slow at the start and fast at the end; the natural counterpart of {@link EaseOutCubic} for
 * "settle back" animations (for example a pressed button that returns to its resting size). The
 * easing curve maps a normalized progress in {@code [0, 1]} to an eased progress in {@code [0, 1]}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getEquation(double)} - the cubic ease-in curve implementation.</li>
 * </ul>
 */
public class EaseInCubic extends Animation {

    public EaseInCubic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    @Override
    protected double getEquation(double t) {
        return t * t * t;
    }
}
