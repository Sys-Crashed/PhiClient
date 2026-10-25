package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * Ease-in-out cubic interpolation (package {@code org.dpdns.syscrashed.phiclient.utils.animation}).
 *
 * <p>Slow at both ends and fast in the middle; suitable for state transitions. The easing curve
 * maps a normalized progress in {@code [0, 1]} to an eased progress in {@code [0, 1]}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getEquation(double)} - the cubic ease-in-out curve implementation.</li>
 * </ul>
 */
public class EaseInOutCubic extends Animation {

    public EaseInOutCubic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    @Override
    protected double getEquation(double t) {
        if (t < 0.5D) {
            return 4.0D * t * t * t;
        }
        double inverted = -2.0D * t + 2.0D;
        return 1.0D - inverted * inverted * inverted / 2.0D;
    }
}
