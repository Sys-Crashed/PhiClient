package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.utils.mouse.MouseUtils;

/**
 * 界面组件基类。
 *
 * <p>对齐原 SoarClient 的 {@code me.eldodebug.soar.ui.comp.Comp}：组件有自己的位置与尺寸，
 * 负责绘制与鼠标交互；父级（分类 / 界面）只负责布局与事件分发。
 */
public abstract class PhiComponent {

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    private boolean hovered;

    public PhiComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 绘制组件。坐标一律是 GuiScreen 的缩放后坐标系。
     */
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    public void keyTyped(char typedChar, int keyCode) {
    }

    /**
     * 每帧刷新 hover 状态（子类在 draw 开头调用）。
     */
    protected void updateHover(int mouseX, int mouseY) {
        this.hovered = MouseUtils.isInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return this.width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
