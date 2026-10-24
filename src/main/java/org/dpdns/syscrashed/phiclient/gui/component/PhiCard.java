package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * 内容卡片：标题 + 说明 + 右侧状态标签。
 *
 * <p>参考常见设置组件（Toggle / Slider / Keybind ...）的共同外观：
 * 圆角卡片 + hover 高亮 + 右侧状态区。这里先给出最基础的卡片，后续的开关、滑块等可以在它上面扩展。
 */
public class PhiCard extends PhiComponent {

    private static final float CORNER_RADIUS = 8.0F;
    private static final float PADDING = 10.0F;

    private final String title;
    private final String description;
    private final String status;

    public PhiCard(float x, float y, float width, float height, String title, String description, String status) {
        super(x, y, width, height);

        this.title = title;
        this.description = description;
        this.status = status;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        // 卡片底
        nvg.drawRoundedRect(this.x, this.y, this.width, this.height, CORNER_RADIUS,
                isHovered() ? PhiTheme.CARD_BORDER : PhiTheme.CARD);

        // 左侧主色竖条
        nvg.drawRoundedRect(this.x, this.y, 2.0F, this.height, 1.0F, PhiTheme.ACCENT);

        // 标题
        nvg.drawText(this.title, this.x + PADDING, this.y + PADDING - 1.0F, PhiTheme.TEXT, 11.0F, Fonts.REGULAR);

        // 说明
        if (this.description != null && !this.description.isEmpty()) {
            nvg.drawText(this.description, this.x + PADDING, this.y + PADDING + 14.0F, PhiTheme.TEXT_DIM, 9.5F, Fonts.REGULAR);
        }

        // 右侧状态
        if (this.status != null && !this.status.isEmpty()) {
            float textWidth = nvg.textWidth(this.status, 9.5F, Fonts.REGULAR);
            nvg.drawText(this.status, this.x + this.width - textWidth - PADDING, this.y + PADDING - 1.0F,
                    PhiTheme.ACCENT, 9.5F, Fonts.REGULAR);
        }
    }
}
