package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;

/**
 * 首页：欢迎卡片 + 状态概览。
 */
public class HomeCategory extends PhiCategory {

    public HomeCategory() {
        super("Home", "H");
    }

    @Override
    public void init(float contentWidth) {
        this.components.clear();

        float cardWidth = contentWidth - 20.0F;

        this.components.add(new PhiCard(0.0F, 0.0F, cardWidth, 44.0F,
                "PhiClient", "NanoVG based hub interface", "v0.1.0"));
        this.components.add(new PhiCard(0.0F, 52.0F, cardWidth, 38.0F,
                "Renderer", "NanoVG GL2 backend", "Ready"));
        this.components.add(new PhiCard(0.0F, 98.0F, cardWidth, 38.0F,
                "Open with", "Right Shift", "In game"));
    }
}
