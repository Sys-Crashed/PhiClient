package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * 动画方向。
 *
 * <p>与原 SoarClient 的 {@code me.eldodebug.soar.utils.animation.normal.Direction} 语义一致：
 * FORWARDS 表示「补间正在向终点推进」，BACKWARDS 表示「正在退回起点」。
 */
public enum Direction {

    FORWARDS,
    BACKWARDS;

    public Direction opposite() {
        return this == FORWARDS ? BACKWARDS : FORWARDS;
    }
}
