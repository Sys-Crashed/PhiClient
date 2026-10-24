package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;

/**
 * 设置分类：界面相关的开关位（占位）。
 */
public class SettingsCategory extends PhiCategory {

    public SettingsCategory() {
        super("Settings", "S");
    }

    @Override
    public void init(float contentWidth) {
        this.components.clear();

        float cardWidth = contentWidth - 20.0F;

        this.components.add(new PhiCard(0.0F, 0.0F, cardWidth, 38.0F,
                "Blur", "Background blur strength", "20"));
        this.components.add(new PhiCard(0.0F, 46.0F, cardWidth, 38.0F,
                "Theme", "Accent colour", "Default"));
    }
}
