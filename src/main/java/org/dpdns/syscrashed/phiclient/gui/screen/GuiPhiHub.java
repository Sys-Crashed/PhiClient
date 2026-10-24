package org.dpdns.syscrashed.phiclient.gui.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.PhiGuiManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.screen.category.HomeCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.ModulesCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.PhiCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.SettingsCategory;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.utils.animation.Animation;
import org.dpdns.syscrashed.phiclient.utils.animation.Direction;
import org.dpdns.syscrashed.phiclient.utils.animation.EaseOutCubic;
import org.dpdns.syscrashed.phiclient.utils.mouse.MouseUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;

/**
 * PhiClient 的中心界面 —— 按右 Shift 打开。
 *
 * <p>架构：
 * <ul>
 *   <li>背景是 MC shader 管线做的模糊（{@code BlurRenderer}）；</li>
 *   <li>面板、侧栏、卡片、文字全部由 NanoVG 自绘 —— <b>不</b>用 MC 的 FontRenderer，
 *       所以 MC 自己的字体完全不受影响；</li>
 *   <li>左侧 32px 侧栏放分类，右边是当前分类的内容区（组件树由 {@link PhiCategory} 组织）；</li>
 *   <li>打开 / 关闭都有缩放动画（{@link Animation}）。</li>
 * </ul>
 *
 * <p>之所以做成真正的 {@link GuiScreen}：Minecraft 在 {@code currentScreen != null} 时
 * 会跳过 {@code runTick} 里的鼠标 / 键盘主循环（{@code while (Mouse.next())} 和
 * {@code while (Keyboard.next())} 都在 {@code currentScreen == null || allowUserInput} 分支里），
 * 于是玩家在游戏本体里的移动、攻击、视角等操作会被天然屏蔽 —— 这正是「打开界面后不能操作游戏」的实现方式。
 */
public class GuiPhiHub extends GuiScreen {

    private static final int PANEL_WIDTH = 450;
    private static final int PANEL_HEIGHT = 280;

    private static final float SIDEBAR_WIDTH = 32.0F;
    private static final float CORNER_RADIUS = 12.0F;
    private static final float ITEM_SIZE = 21.0F;
    private static final float ITEM_SPACING = 30.0F;
    private static final float ITEM_TOP = 38.5F;
    private static final float CONTENT_PADDING = 10.0F;

    private final Minecraft mc = Minecraft.getMinecraft();

    private final List<PhiCategory> categories = new ArrayList<PhiCategory>();

    private PhiCategory currentCategory;

    private final Animation introAnimation = new EaseOutCubic(260, 1.0D);

    private int panelX;
    private int panelY;

    /**
     * 打开界面的那一次右 Shift 按下可能以「重复事件」的形式漏进来，
     * 所以在收到一次抬起之前，忽略所有右 Shift 按下。
     */
    private boolean awaitingRightShiftRelease = true;

    public GuiPhiHub() {
        this.categories.add(new HomeCategory());
        this.categories.add(new ModulesCategory());
        this.categories.add(new SettingsCategory());
        this.currentCategory = this.categories.get(0);
    }

    @Override
    public void initGui() {
        super.initGui();

        ScaledResolution sr = new ScaledResolution(this.mc);
        this.panelX = (sr.getScaledWidth() / 2) - (PANEL_WIDTH / 2);
        this.panelY = (sr.getScaledHeight() / 2) - (PANEL_HEIGHT / 2);

        this.introAnimation.reset();
        this.introAnimation.setDirection(Direction.FORWARDS);

        float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PADDING * 2.0F;
        for (PhiCategory category : this.categories) {
            category.init(contentWidth);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PhiGuiManager gui = PhiClient.getInstance().getGuiManager();

        // 关闭动画播完 → 真正关掉界面
        if (this.introAnimation.isDone(Direction.BACKWARDS)) {
            this.mc.displayGuiScreen(null);
            return;
        }

        float intro = (float) Math.min(1.0D, this.introAnimation.getValue());

        // 1) 背景虚化（直接写回 MC 主帧缓冲，覆盖掉清晰画面）
        gui.getBlurRenderer().render(partialTicks, 1.0F + intro * 19.0F);

        // 2) 用 NanoVG 自绘整个界面
        NanoVGManager nvg = gui.getNanoVGManager();
        if (!nvg.ensureReady()) {
            return;
        }

        final int mouseXFinal = mouseX;
        final int mouseYFinal = mouseY;
        nvg.setupAndDraw(new Runnable() {
            @Override
            public void run() {
                drawPanel(nvg, mouseXFinal, mouseYFinal, partialTicks, intro);
            }
        }, true);
    }

    /**
     * 画整个面板。坐标系是 GuiScreen 的缩放后坐标系。
     */
    private void drawPanel(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks, float intro) {
        float scale = 0.94F + 0.06F * intro;
        float centerX = this.panelX + PANEL_WIDTH / 2.0F;
        float centerY = this.panelY + PANEL_HEIGHT / 2.0F;

        nvg.save();
        nvg.translate(centerX, centerY);
        nvg.scale(scale, scale);
        nvg.translate(-centerX, -centerY);

        // 面板主体
        nvg.drawRoundedRect(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, PhiTheme.PANEL);

        // 侧栏（左边圆角、右边补方角，避免圆角处透出主体）
        nvg.drawRoundedRect(this.panelX, this.panelY, SIDEBAR_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, PhiTheme.PANEL_DARK);
        nvg.drawRect(this.panelX + SIDEBAR_WIDTH - CORNER_RADIUS, this.panelY, CORNER_RADIUS, PANEL_HEIGHT, PhiTheme.PANEL_DARK);

        // 侧栏顶部的 logo
        nvg.drawGradientRoundedRect(this.panelX + 5.0F, this.panelY + 7.0F, 22.0F, 22.0F, 11.0F, PhiTheme.ACCENT, PhiTheme.ACCENT_DARK);
        nvg.drawText("P", this.panelX + 12.5F, this.panelY + 11.0F, PhiTheme.TEXT, 14.0F, Fonts.REGULAR);

        // 侧栏分类项
        for (int i = 0; i < this.categories.size(); i++) {
            PhiCategory category = this.categories.get(i);
            float itemX = this.panelX + 5.5F;
            float itemY = this.panelY + ITEM_TOP + i * ITEM_SPACING;
            boolean selected = category == this.currentCategory;

            if (selected) {
                nvg.drawGradientRoundedRect(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 4.0F, PhiTheme.ACCENT, PhiTheme.ACCENT_DARK);
            }

            float markerWidth = nvg.textWidth(category.getMarker(), 9.5F, Fonts.REGULAR);
            nvg.drawText(category.getMarker(), itemX + (ITEM_SIZE - markerWidth) / 2.0F, itemY + 5.0F,
                    selected ? PhiTheme.TEXT : PhiTheme.TEXT_DIM, 9.5F, Fonts.REGULAR);
        }

        // 顶栏分隔线与标题
        nvg.drawRect(this.panelX + SIDEBAR_WIDTH, this.panelY + 30.0F, PANEL_WIDTH - SIDEBAR_WIDTH, 1.0F, PhiTheme.DIVIDER);
        nvg.drawText(this.currentCategory.getName(),
                this.panelX + SIDEBAR_WIDTH + CONTENT_PADDING, this.panelY + 40.0F, PhiTheme.TEXT, 13.0F, Fonts.REGULAR);

        // 内容区（用 scissor 裁掉超出面板的部分）
        float contentX = this.panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
        float contentY = this.panelY + 62.0F;
        float contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PADDING * 2.0F;
        float contentHeight = PANEL_HEIGHT - 62.0F - CONTENT_PADDING;

        nvg.save();
        nvg.scissor(contentX, contentY, contentWidth, contentHeight);
        this.currentCategory.draw(nvg, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, partialTicks);
        nvg.resetScissor();
        nvg.restore();

        nvg.restore();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            for (int i = 0; i < this.categories.size(); i++) {
                float itemX = this.panelX + 5.5F;
                float itemY = this.panelY + ITEM_TOP + i * ITEM_SPACING;
                if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, ITEM_SIZE, ITEM_SIZE)) {
                    this.currentCategory = this.categories.get(i);
                    return;
                }
            }
        }

        this.currentCategory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.currentCategory.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        this.currentCategory.keyTyped(typedChar, keyCode);
    }

    /**
     * 吃掉全部鼠标 / 键盘事件：打开期间玩家不能对游戏本体做任何操作。
     * 只识别 ESC 与右 Shift 作为「关闭」。
     */
    @Override
    public void handleInput() throws IOException {
        if (Mouse.isCreated()) {
            while (Mouse.next()) {
                // 丢弃
            }
        }

        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                int key = Keyboard.getEventKey();
                boolean pressed = Keyboard.getEventKeyState();

                if (key == Keyboard.KEY_ESCAPE && pressed) {
                    close();
                    return;
                }

                if (key == Keyboard.KEY_RSHIFT) {
                    if (pressed) {
                        if (!this.awaitingRightShiftRelease) {
                            close();
                            return;
                        }
                    } else {
                        this.awaitingRightShiftRelease = false;
                    }
                }
            }
        }
    }

    /** 界面打开时游戏照常运行（不暂停）。 */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        // 打开界面时可能正按着 W / Shift 之类，清掉按键状态，避免关掉界面后角色自己动
        KeyBinding.unPressAllKeys();
    }

    /**
     * 关闭：播放退场动画，动画结束后由 drawScreen 真正切回游戏。
     */
    private void close() {
        this.introAnimation.setDirection(Direction.BACKWARDS);
    }
}
