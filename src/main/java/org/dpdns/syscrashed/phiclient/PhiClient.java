package org.dpdns.syscrashed.phiclient;

import org.dpdns.syscrashed.phiclient.compat.lwjgl.Lwjgl3NativeInstaller;
import org.dpdns.syscrashed.phiclient.compat.optifine.OptiFineCompat;
import org.dpdns.syscrashed.phiclient.gui.PhiGuiManager;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.management.event.EventManager;

/**
 * PhiClient 客户端单例 —— 业务逻辑的初始化编排中心。
 *
 * 启动链路：LaunchWrapper → {@code PhiTweaker}（Mixin 引导）
 * → {@code MixinMinecraft#startGame} 注入 → {@link #start()}。
 *
 * 约定：后续新增的管理器（账号 / 模块 / 配置 / GUI ...）统一在 {@link #start()} 中挂载，
 * 在 {@link #stop()} 中保存与释放。
 */

public final class PhiClient {

    public static final String NAME = "PhiClient";
    public static final String VERSION = "0.1.0";
    public static final String MC_VERSION = "1.8.9";

    /** 游戏窗口标题，由 {@code MixinMinecraft#createDisplay} 在窗口创建后写入 LWJGL Display。 */
    public static final String WINDOW_TITLE = NAME + " v" + VERSION + " for Minecraft " + MC_VERSION;

    private static final PhiClient INSTANCE = new PhiClient();

    private final EventManager eventManager = new EventManager();
    private final PhiGuiManager guiManager = new PhiGuiManager();

    private boolean running;

    private PhiClient() {
    }

    public static PhiClient getInstance() {
        return INSTANCE;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public PhiGuiManager getGuiManager() {
        return guiManager;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 客户端初始化主流程，由 {@code MixinMinecraft#startGame} 在 ingameGUI 初始化后触发。
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        printBanner();
        PhiLogger.info("Initializing " + NAME + " v" + VERSION + " (Minecraft " + MC_VERSION + ")...");
        logOptiFineStatus();

        // LWJGL 3 原生库必须先于任何 NanoVG 类被加载：
        //   LWJGL 3 会优先从 java.library.path 找 lwjgl.dll，而那里是 LWJGL 2 的 32 位同名文件，
        //   加载错库会直接把 JVM 搞到堆损坏（0xC0000374）。详见该类的注释。
        Lwjgl3NativeInstaller.install();

        // GUI 子系统：NanoVG 上下文 + 中心界面（右 Shift 开关）
        guiManager.init();
        eventManager.register(guiManager);

        // TODO: 后续在此挂载其余管理器（EventManager 已就绪，可直接注册监听器）

        PhiLogger.info(NAME + " initialized successfully.");
    }

    /**
     * 客户端关闭流程，由 {@code MixinMinecraft#shutdownMinecraftApplet} 触发。
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;

        guiManager.shutdown();

        // TODO: 后续在此保存配置与释放资源
        PhiLogger.info(NAME + " stopped.");
    }

    private void logOptiFineStatus() {
        if (OptiFineCompat.isLoaded()) {
            PhiLogger.info("OptiFine detected: " + OptiFineCompat.getVersion());
        } else {
            PhiLogger.info("OptiFine not detected.");
        }
    }

    private void printBanner() {
        PhiLogger.info("=========================================");
        PhiLogger.info("  " + NAME + " v" + VERSION + "  /  Minecraft " + MC_VERSION);
        PhiLogger.info("  package: org.dpdns.syscrashed.phiclient");
        PhiLogger.info("=========================================");
    }
}
