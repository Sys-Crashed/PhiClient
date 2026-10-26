package org.dpdns.syscrashed.phiclient.gui.module;

import java.util.ArrayList;
import java.util.List;

/**
 * The ten placeholder modules that test the modules page of the PhiHub
 * (package {@code org.dpdns.syscrashed.phiclient.gui.module}).
 *
 * <p>Five modules are plain switches and five have adjustable parameters, so both kinds of rows of
 * the module page are exercised. The values are placeholders - the real module system replaces them
 * in a later stage.</p>
 */
public final class PhiPlaceholderModules {

    private PhiPlaceholderModules() {
    }

    /**
     * @return a fresh list of the ten placeholder modules.
     */
    public static List<PhiModule> create() {
        List<PhiModule> modules = new ArrayList<PhiModule>();

        // ------------------------------------------------------------------ switch only

        modules.add(new PhiModule("Fullbright", "Makes every block visible in the dark.", true));
        modules.add(new PhiModule("Auto Sprint", "Keeps the sprint of the player active.", true));
        modules.add(new PhiModule("Toggle Sneak", "Sneaks without holding the key.", false));
        modules.add(new PhiModule("Coordinates", "Shows the position of the player.", true));
        modules.add(new PhiModule("Armor HUD", "Shows the equipped armor below the hotbar.", false));

        // ------------------------------------------------------------------ with settings

        PhiModule zoom = new PhiModule("Zoom", "Magnifies the camera on a key press.", true);
        zoom.addSetting(new PhiModuleSetting("Zoom Factor", 3.0F, 1.0F, 8.0F, 0.5F));
        zoom.addSetting(new PhiModuleSetting("Smooth Camera", true));
        modules.add(zoom);

        PhiModule keystrokes = new PhiModule("Keystrokes", "Renders the pressed keys on the screen.", true);
        keystrokes.addSetting(new PhiModuleSetting("Size", 100.0F, 60.0F, 140.0F, 5.0F));
        keystrokes.addSetting(new PhiModuleSetting("Show CPS", false));
        keystrokes.addSetting(new PhiModuleSetting("Style", new String[] { "WASD", "Arrows", "Mouse" }, 0));
        modules.add(keystrokes);

        PhiModule fps = new PhiModule("FPS Booster", "Optimizes the game for more frames.", true);
        fps.addSetting(new PhiModuleSetting("Optimization", 60.0F, 0.0F, 100.0F, 5.0F));
        fps.addSetting(new PhiModuleSetting("Preset", new String[] { "Fast", "Balanced", "Quality" }, 1));
        fps.addSetting(new PhiModuleSetting("Lazy Chunk Loading", true));
        modules.add(fps);

        PhiModule particles = new PhiModule("Particles", "Multiplies the amount of particles.", false);
        particles.addSetting(new PhiModuleSetting("Multiplier", 2.0F, 0.0F, 4.0F, 0.5F));
        particles.addSetting(new PhiModuleSetting("Hide Critical Hits", false));
        modules.add(particles);

        PhiModule timeChanger = new PhiModule("Time Changer", "Changes the time of day client side.", false);
        timeChanger.addSetting(new PhiModuleSetting("Time", new String[] { "Day", "Sunset", "Night", "Sunrise" }, 0));
        timeChanger.addSetting(new PhiModuleSetting("Speed", 1.0F, 0.0F, 4.0F, 0.5F));
        modules.add(timeChanger);

        return modules;
    }
}
