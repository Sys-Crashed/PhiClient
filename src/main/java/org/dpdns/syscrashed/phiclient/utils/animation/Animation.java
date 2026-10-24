package org.dpdns.syscrashed.phiclient.utils.animation;

import org.dpdns.syscrashed.phiclient.utils.TimerUtils;

/**
 * 通用补间动画：把「时间进度」映射成 0..1 的进度，再交给子类做缓动。
 *
 * <p>对齐原 SoarClient 的 {@code utils.animation.normal.Animation}，但把曲线参数从「毫秒」
 * 改成归一化的 0..1（{@code t}），语义更直观、也更容易写不同的缓动。
 *
 * <p>用法：
 * <pre>
 * Animation anim = new EaseOutCubic(280, 1.0D);
 * anim.setDirection(Direction.FORWARDS);
 * float v = anim.getValueFloat();   // 0 -&gt; 1
 * </pre>
 */
public abstract class Animation {

    protected final TimerUtils timer = new TimerUtils();

    /** 一次完整动画的时长（毫秒）。 */
    protected int duration;

    /** 终点值（通常是 1.0，也可以是任意数值）。 */
    protected double endPoint;

    protected Direction direction = Direction.FORWARDS;

    protected Animation(int duration, double endPoint) {
        this.duration = duration;
        this.endPoint = endPoint;
        this.timer.reset();
    }

    /**
     * 当前值。FORWARDS 时从 0 走向 endPoint，BACKWARDS 时反向。
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

    public float getValueFloat() {
        return (float) getValue();
    }

    public int getValueInt() {
        return (int) getValue();
    }

    /**
     * 缓动曲线：输入归一化进度 0..1，输出缓动后的进度 0..1。
     */
    protected abstract double getEquation(double t);

    public boolean isDone() {
        return this.timer.getElapsedTime() >= this.duration;
    }

    public boolean isDone(Direction direction) {
        return isDone() && this.direction == direction;
    }

    public void reset() {
        this.timer.reset();
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            // 换向时把计时起点挪到「已经走完的进度」上，动画才不会跳变
            long elapsed = Math.min(this.duration, this.timer.getElapsedTime());
            this.timer.setLastMs(System.currentTimeMillis() - elapsed);
        }
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getDuration() {
        return this.duration;
    }

    public double getEndPoint() {
        return this.endPoint;
    }
}
