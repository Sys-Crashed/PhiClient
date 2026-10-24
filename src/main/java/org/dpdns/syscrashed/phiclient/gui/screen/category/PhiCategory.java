package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.PhiComponent;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * 界面分类（侧栏里的每一项）。
 *
 * <p>对齐原 SoarClient 的 {@code gui.modmenu.category.Category}：分类持有自己的组件列表，
 * 负责把组件布局到内容区并分发交互；切换分类时由界面负责淡入淡出。
 */
public abstract class PhiCategory {

    private final String name;

    /** 侧栏里显示的短标记（当前用名称首字母，避免依赖未知的图标字形）。 */
    private final String marker;

    protected final List<PhiComponent> components = new ArrayList<PhiComponent>();

    protected PhiCategory(String name, String marker) {
        this.name = name;
        this.marker = marker;
    }

    /**
     * 构建组件（只调用一次）。
     */
    public abstract void init(float contentWidth);

    public void draw(NanoVGManager nvg, float x, float y, float width, float height, int mouseX, int mouseY, float partialTicks) {
        for (PhiComponent component : this.components) {
            // 组件是用「相对内容区」的坐标创建的，这里临时平移到绝对坐标再画。
            float relativeX = component.getX();
            float relativeY = component.getY();

            component.setX(x + relativeX);
            component.setY(y + relativeY);
            component.draw(nvg, mouseX, mouseY, partialTicks);
            component.setX(relativeX);
            component.setY(relativeY);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (PhiComponent component : this.components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (PhiComponent component : this.components) {
            component.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (PhiComponent component : this.components) {
            component.keyTyped(typedChar, keyCode);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getMarker() {
        return this.marker;
    }
}
