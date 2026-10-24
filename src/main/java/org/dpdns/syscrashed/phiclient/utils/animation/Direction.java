package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * 动画方向。
 *
 * <p>FORWARDS 表示「补间正在向终点推进」，BACKWARDS 表示「正在退回起点」。
 */
public enum Direction {

    FORWARDS,
    BACKWARDS;

    public Direction opposite() {
        return this == FORWARDS ? BACKWARDS : FORWARDS;
    }
}
