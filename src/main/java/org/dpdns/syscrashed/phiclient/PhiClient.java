package org.dpdns.syscrashed.phiclient;

import org.dpdns.syscrashed.phiclient.compat.lwjgl.Lwjgl3NativeInstaller;
import org.dpdns.syscrashed.phiclient.compat.optifine.OptiFineCompat;
import org.dpdns.syscrashed.phiclient.gui.PhiGuiManager;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.management.event.EventManager;

/**
 * PhiClient singleton - the orchestration center for business-logic initialization
 * (package {@code org.dpdns.syscrashed.phiclient}).
 *
 * <p>Startup chain: LaunchWrapper -&gt; {@code PhiTweaker} (Mixin bootstrap)
 * -&gt; {@code MixinMinecraft#startGame} injection -&gt; {@link #start()}.</p>
 *
 * <p>Convention: any new manager (account / module / config / GUI ...) is mounted in {@link #start()}
 * and saved / released in {@link #stop()}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getInstance()} - returns the singleton instance.</li>
 *   <li>{@link #getEventManager()} / {@link #getGuiManager()} - access the core managers.</li>
 *   <li>{@link #isRunning()} - whether the client has finished initialization.</li>
 *   <li>{@link #start()} - the main initialization routine (invoked by {@code MixinMinecraft#startGame}).</li>
 *   <li>{@link #stop()} - the shutdown routine (invoked by {@code MixinMinecraft#shutdownMinecraftApplet}).</li>
 * </ul>
 */
public final class PhiClient {

    public static final String NAME = "PhiClient";
    public static final String VERSION = "0.1.0";
    public static final String MC_VERSION = "1.8.9";

    /** Game window title, written to the LWJGL Display after the window is created by {@code MixinMinecraft#createDisplay}. */
    public static final String WINDOW_TITLE = NAME + " v" + VERSION + " for Minecraft " + MC_VERSION;

    private static final PhiClient INSTANCE = new PhiClient();

    private final EventManager eventManager = new EventManager();
    private final PhiGuiManager guiManager = new PhiGuiManager();

    private boolean running;

    private PhiClient() {
    }

    /**
     * @return the singleton instance.
     */
    public static PhiClient getInstance() {
        return INSTANCE;
    }

    /**
     * @return the event bus manager.
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * @return the GUI subsystem manager.
     */
    public PhiGuiManager getGuiManager() {
        return guiManager;
    }

    /**
     * @return true once {@link #start()} has completed.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Main client initialization, triggered by {@code MixinMinecraft#startGame} after the ingameGUI
     * is initialized.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        printBanner();
        PhiLogger.info("Initializing " + NAME + " v" + VERSION + " (Minecraft " + MC_VERSION + ")...");
        logOptiFineStatus();

        // LWJGL 3 native libraries must be installed before any NanoVG class is loaded:
        //   LWJGL 3 prioritizes java.library.path when resolving lwjgl.dll, which in MC 1.8.9 points at
        //   LWJGL 2's 32-bit file; loading the wrong library corrupts the JVM heap (0xC0000374). See that class.
        Lwjgl3NativeInstaller.install();

        // GUI subsystem: NanoVG context + central screen (toggled with Right-Shift).
        guiManager.init();
        eventManager.register(guiManager);

        // TODO: mount the remaining managers here (EventManager is ready, just register listeners).

        PhiLogger.info(NAME + " initialized successfully.");
    }

    /**
     * Client shutdown, triggered by {@code MixinMinecraft#shutdownMinecraftApplet}.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;

        guiManager.shutdown();

        // TODO: save config and release resources here.
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
