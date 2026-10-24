package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;

/**
 * 模块分类：目前只是占位卡片，等模块系统落地后替换为真实的开关 / 滑块组件。
 */
public class ModulesCategory extends PhiCategory {

    public ModulesCategory() {
        super("Modules", "M");
    }

    @Override
    public void init(float contentWidth) {
        this.components.clear();

        float cardWidth = contentWidth - 20.0F;

        this.components.add(new PhiCard(0.0F, 0.0F, cardWidth, 38.0F,
                "Module list", "No module loaded yet", "0"));
        this.components.add(new PhiCard(0.0F, 46.0F, cardWidth, 38.0F,
                "Components", "Toggle / Slider / Keybind", "Soon"));
    }
}
