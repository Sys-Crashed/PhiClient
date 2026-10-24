package org.dpdns.syscrashed.phiclient.gui;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.render.BlurRenderer;
import org.dpdns.syscrashed.phiclient.gui.screen.GuiPhiHub;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.management.event.EventTarget;
import org.dpdns.syscrashed.phiclient.management.event.impl.EventTick;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * GUI 子系统的门面：持有 NanoVG 与虚化渲染器，并负责中心界面的开合。
 *
 * <p>开启条件：必须「已经在游戏里」——有世界、有玩家、且当前没有任何 GuiScreen
 * （标题界面 / 背包 / 设置这些都不算游戏内）。
 */
public final class PhiGuiManager {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NanoVGManager nanoVGManager = new NanoVGManager();
    private final BlurRenderer blurRenderer = new BlurRenderer();

    private boolean rightShiftDown;

    public NanoVGManager getNanoVGManager() {
        return nanoVGManager;
    }

    public BlurRenderer getBlurRenderer() {
        return blurRenderer;
    }

    /**
     * 注册进事件总线。
     *
     * <p>注意：NanoVG 上下文是【懒创建】的 —— 真正要画第一帧的时候才建。
     * 原因见 {@link NanoVGManager} 的注释：{@code nvgCreate} 会把自己的 shader program 绑上，
     * 而 MC 启动阶段还要用固定管线生成字体图集，那时不能有 program 处于绑定状态。
     */
    public void init() {
        PhiLogger.info("GUI subsystem ready (NanoVG context is created lazily on first draw).");
    }

    public void shutdown() {
        blurRenderer.delete();
        nanoVGManager.shutdown();
    }

    /**
     * 右 Shift 的上升沿检测。用 {@link Keyboard#isKeyDown} 直接读按键状态，
     * 不依赖事件队列（事件队列在界面打开后会被界面自己吃掉）。
     */
    @EventTarget
    public void onTick(EventTick event) {
        boolean down = Keyboard.isCreated() && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (down && !rightShiftDown) {
            open();
        }

        rightShiftDown = down;
    }

    public boolean isInGame() {
        return mc.theWorld != null && mc.thePlayer != null && mc.currentScreen == null;
    }

    public void open() {
        if (!isInGame()) {
            return;
        }
        // 打开瞬间清掉按键状态，避免「按着 W 打开界面 → 角色一直往前走」
        KeyBinding.unPressAllKeys();
        mc.displayGuiScreen(new GuiPhiHub());
    }

    public void close() {
        if (mc.currentScreen instanceof GuiPhiHub) {
            mc.displayGuiScreen(null);
        }
    }
}
