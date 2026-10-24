package org.dpdns.syscrashed.phiclient.utils.mouse;

/**
 * 鼠标命中判定工具。
 *
 * <p>对齐原 SoarClient 的 {@code me.eldodebug.soar.utils.mouse.MouseUtils}。
 */
public final class MouseUtils {

    private MouseUtils() {
    }

    /**
     * 鼠标是否落在矩形内。
     */
    public static boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * 鼠标是否落在矩形内（int 版本）。
     */
    public static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return isInside((float) mouseX, (float) mouseY, (float) x, (float) y, (float) width, (float) height);
    }

    /**
     * 把鼠标坐标限制在给定矩形内（拖拽时用）。
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
