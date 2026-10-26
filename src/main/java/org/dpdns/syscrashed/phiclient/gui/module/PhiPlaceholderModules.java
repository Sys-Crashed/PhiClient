package org.dpdns.syscrashed.phiclient.gui.module;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;

/**
 * The one hundred placeholder modules that test the modules page of the PhiHub
 * (package {@code org.dpdns.syscrashed.phiclient.gui.module}).
 *
 * <p>Ten of the modules are meaningful placeholders and ninety are generated test entries - one
 * hundred entries in total, so the dial style scrolling of the module page can be verified over a
 * long list. Every third generated module additionally owns adjustable parameters, so both kinds of
 * rows are exercised. The values are placeholders - the real module system replaces them in a later
 * stage.</p>
 */
public final class PhiPlaceholderModules {

    /** The icon pool the placeholder modules are filled with. */
    private static final PhiIcons[] ICONS = {
        PhiIcons.MODULES, PhiIcons.MUSIC, PhiIcons.HUD, PhiIcons.SETTINGS,
        PhiIcons.PLAY, PhiIcons.CONTRAST, PhiIcons.MORE_SETTINGS, PhiIcons.RESET
    };

    /** How many modules the placeholder page holds. */
    private static final int TOTAL_MODULES = 100;

    /** The index of the first generated test module. */
    private static final int FIRST_GENERATED = 11;

    private PhiPlaceholderModules() {
    }

    /**
     * @return a fresh list of the one hundred placeholder modules.
     */
    public static List<PhiModule> create() {
        List<PhiModule> modules = new ArrayList<PhiModule>();

        // ------------------------------------------------------------------ switch only

        modules.add(new PhiModule("Fullbright", "Makes every block visible in the dark.", icon(0), true));
        modules.add(new PhiModule("Auto Sprint", "Keeps the sprint of the player active.", icon(1), true));
        modules.add(new PhiModule("Toggle Sneak", "Sneaks without holding the key.", icon(2), false));
        modules.add(new PhiModule("Coordinates", "Shows the position of the player.", icon(3), true));
        modules.add(new PhiModule("Armor HUD", "Shows the equipped armor below the hotbar.", icon(4), false));

        // ------------------------------------------------------------------ with settings

        PhiModule zoom = new PhiModule("Zoom", "Magnifies the camera on a key press.", icon(5), true);
        zoom.addSetting(new PhiModuleSetting("Zoom Factor", 3.0F, 1.0F, 8.0F, 0.5F));
        zoom.addSetting(new PhiModuleSetting("Smooth Camera", true));
        modules.add(zoom);

        PhiModule keystrokes = new PhiModule("Keystrokes", "Renders the pressed keys on the screen.", icon(6), true);
        keystrokes.addSetting(new PhiModuleSetting("Size", 100.0F, 60.0F, 140.0F, 5.0F));
        keystrokes.addSetting(new PhiModuleSetting("Show CPS", false));
        keystrokes.addSetting(new PhiModuleSetting("Style", new String[] { "WASD", "Arrows", "Mouse" }, 0));
        modules.add(keystrokes);

        PhiModule fps = new PhiModule("FPS Booster", "Optimizes the game for more frames.", icon(7), true);
        fps.addSetting(new PhiModuleSetting("Optimization", 60.0F, 0.0F, 100.0F, 5.0F));
        fps.addSetting(new PhiModuleSetting("Preset", new String[] { "Fast", "Balanced", "Quality" }, 1));
        fps.addSetting(new PhiModuleSetting("Lazy Chunk Loading", true));
        modules.add(fps);

        PhiModule particles = new PhiModule("Particles", "Multiplies the amount of particles.", icon(0), false);
        particles.addSetting(new PhiModuleSetting("Multiplier", 2.0F, 0.0F, 4.0F, 0.5F));
        particles.addSetting(new PhiModuleSetting("Hide Critical Hits", false));
        modules.add(particles);

        PhiModule timeChanger = new PhiModule("Time Changer", "Changes the time of day client side.", icon(1), false);
        timeChanger.addSetting(new PhiModuleSetting("Time", new String[] { "Day", "Sunset", "Night", "Sunrise" }, 0));
        timeChanger.addSetting(new PhiModuleSetting("Speed", 1.0F, 0.0F, 4.0F, 0.5F));
        modules.add(timeChanger);

        // ------------------------------------------------------------------ generated test entries

        for (int i = FIRST_GENERATED; i <= TOTAL_MODULES; i++) {
            PhiModule module = new PhiModule("Test Module " + i,
                    "Generated placeholder #" + i + " for scroll testing.", icon(i), i % 2 == 0);

            if (i % 3 == 0) {
                module.addSetting(new PhiModuleSetting("Power", 50.0F, 0.0F, 100.0F, 5.0F));
                module.addSetting(new PhiModuleSetting("Mode", new String[] { "Fast", "Balanced", "Quality" }, i % 3));
            }

            modules.add(module);
        }

        return modules;
    }

    /**
     * Picks the icon of a placeholder module.
     *
     * @param index the index of the module.
     * @return the icon of the module.
     */
    private static PhiIcons icon(int index) {
        return ICONS[index % ICONS.length];
    }
}
