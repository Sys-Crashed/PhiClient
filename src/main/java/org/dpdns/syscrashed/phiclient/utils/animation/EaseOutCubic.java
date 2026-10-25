package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * Ease-out cubic interpolation (package {@code org.dpdns.syscrashed.phiclient.utils.animation}).
 *
 * <p>Fast at the start and gentle at the end; suitable for UI entrance animations. The easing curve
 * maps a normalized progress in {@code [0, 1]} to an eased progress in {@code [0, 1]}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getEquation(double)} - the cubic ease-out curve implementation.</li>
 * </ul>
 */
public class EaseOutCubic extends Animation {

    public EaseOutCubic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    @Override
    protected double getEquation(double t) {
        double inverted = 1.0D - t;
        return 1.0D - inverted * inverted * inverted;
    }
}
