package org.dpdns.syscrashed.phiclient.utils;

/**
 * 极简计时器：记录上一次 reset 的时间戳，提供「是否已经过去 N 毫秒」与「已过去多少毫秒」。
 *
 * <p>仅保留动画真正需要的部分（计时 / 帧间隔测量），去掉了与剪贴板 / 文件相关的功能。
 */
public final class TimerUtils {

    private long lastMs;

    public TimerUtils() {
        this.lastMs = System.currentTimeMillis();
    }

    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }

    /**
     * 是否已经过去 {@code nextDelay} 毫秒（不自动重置）。
     */
    public boolean delay(long nextDelay) {
        return System.currentTimeMillis() - this.lastMs >= nextDelay;
    }

    /**
     * 是否已经过去 {@code nextDelay} 毫秒；{@code reset} 为 true 时顺带重置计时。
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

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMs;
    }

    public long getLastMs() {
        return this.lastMs;
    }

    public void setLastMs(long lastMs) {
        this.lastMs = lastMs;
    }
}
