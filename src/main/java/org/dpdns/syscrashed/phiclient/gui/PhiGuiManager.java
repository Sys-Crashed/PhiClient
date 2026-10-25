package org.dpdns.syscrashed.phiclient.gui;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.render.BlurRenderer;
import org.dpdns.syscrashed.phiclient.gui.screen.GuiPhiHub;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiToast;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.management.event.EventTarget;
import org.dpdns.syscrashed.phiclient.management.event.impl.EventTick;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Facade for the GUI subsystem (package {@code org.dpdns.syscrashed.phiclient.gui}).
 *
 * <p>Holds the NanoVG manager and the blur renderer, and owns opening / closing the central hub
 * screen. The hub can only be opened "in game" - there must be a world, a player, and no other
 * {@code GuiScreen} active (title screen / inventory / settings do not count as in-game).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getNanoVGManager()} / {@link #getBlurRenderer()} - access the drawing subsystems.</li>
 *   <li>{@link #init()} - registers the manager on the event bus (the NanoVG context stays lazily created).</li>
 *   <li>{@link #shutdown()} - releases the blur and NanoVG resources.</li>
 *   <li>{@link #onTick(EventTick)} - detects the rising edge of the Right-Shift key to open the hub.</li>
 *   <li>{@link #isInGame()} - whether the game is currently in a playable state.</li>
 *   <li>{@link #open()} / {@link #close()} - show / hide the hub screen.</li>
 * </ul>
 */
public final class PhiGuiManager {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NanoVGManager nanoVGManager = new NanoVGManager();
    private final BlurRenderer blurRenderer = new BlurRenderer();

    private final PhiToast toast = new PhiToast();

    private boolean rightShiftDown;

    /**
     * @return the NanoVG drawing manager.
     */
    public NanoVGManager getNanoVGManager() {
        return nanoVGManager;
    }

    /**
     * @return the toast that confirms actions at the top of the game window.
     */
    public PhiToast getToast() {
        return toast;
    }

    /**
     * @return the background blur renderer.
     */
    public BlurRenderer getBlurRenderer() {
        return blurRenderer;
    }

    /**
     * Registers this manager on the event bus.
     *
     * <p>Note: the NanoVG context is created <b>lazily</b> - on the first frame that actually draws.
     * See {@link NanoVGManager} for the reason: {@code nvgCreate} binds its own shader program, while
     * MC's startup still uses the fixed pipeline to build the font atlas, so no program may be bound then.</p>
     */
    public void init() {
        PhiLogger.info("GUI subsystem ready (NanoVG context is created lazily on first draw).");
    }

    /**
     * Releases the GUI resources (called on client shutdown).
     */
    public void shutdown() {
        blurRenderer.delete();
        nanoVGManager.shutdown();
    }

    /**
     * Detects the rising edge of the Right-Shift key. Reads the key state directly via
     * {@link Keyboard#isKeyDown} rather than the event queue (which is consumed by the screen once open).
     *
     * @param event the per-frame tick event.
     */
    @EventTarget
    public void onTick(EventTick event) {
        boolean down = Keyboard.isCreated() && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (down && !rightShiftDown) {
            open();
        }

        rightShiftDown = down;
    }

    /**
     * @return true if the game is currently in a playable state (world + player, no active screen).
     */
    public boolean isInGame() {
        return mc.theWorld != null && mc.thePlayer != null && mc.currentScreen == null;
    }

    /**
     * Opens the hub screen. Does nothing unless {@link #isInGame()} is true. Clears all key states on
     * open so a held movement key does not keep walking the player.
     */
    public void open() {
        if (!isInGame()) {
            return;
        }
        // Drop key state immediately, otherwise "hold W to open the GUI" would keep the player moving forward.
        KeyBinding.unPressAllKeys();
        mc.displayGuiScreen(new GuiPhiHub());
    }

    /**
     * Closes the hub screen if it is currently shown.
     */
    public void close() {
        if (mc.currentScreen instanceof GuiPhiHub) {
            mc.displayGuiScreen(null);
        }
    }
}
