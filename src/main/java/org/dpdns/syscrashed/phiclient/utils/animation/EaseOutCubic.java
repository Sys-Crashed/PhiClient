package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * 三次缓出（ease-out cubic）：起步快、收尾稳，适合界面入场。
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
