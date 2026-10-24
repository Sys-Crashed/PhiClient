package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * 三次缓入缓出（ease-in-out cubic）：两头慢、中间快，适合状态切换。
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
