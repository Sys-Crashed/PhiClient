package org.dpdns.syscrashed.phiclient.gui.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A module shown on the modules page of the PhiHub
 * (package {@code org.dpdns.syscrashed.phiclient.gui.module}).
 *
 * <p>A module has a name, a short description and a toggle state. There are two kinds of modules:</p>
 * <ul>
 *   <li>a module <b>without</b> settings is only a switch - it shows no "more settings" entry;</li>
 *   <li>a module <b>with</b> settings can additionally adjust its parameters in the "more settings"
 *       panel, which is opened by the gear icon on the right side of the module row (the same way the
 *       Soar client does it).</li>
 * </ul>
 *
 * <p>This is the placeholder data model that tests the GUI; the real module system replaces it later.</p>
 */
public class PhiModule {

    /** The name of the module. */
    private final String name;

    /** The short description below the name. */
    private final String description;

    /** Whether the module is enabled. */
    private boolean toggled;

    /** The adjustable parameters of the module (empty = switch only). */
    private final List<PhiModuleSetting> settings;

    /**
     * Creates a module.
     *
     * @param name        the name of the module.
     * @param description the short description below the name.
     * @param toggled     whether the module starts enabled.
     */
    public PhiModule(String name, String description, boolean toggled) {
        this.name = name;
        this.description = description;
        this.toggled = toggled;
        this.settings = new ArrayList<PhiModuleSetting>();
    }

    /**
     * Adds a parameter to the module.
     *
     * @param setting the parameter to add.
     */
    public void addSetting(PhiModuleSetting setting) {
        this.settings.add(setting);
    }

    /**
     * Restores the default values of every parameter.
     */
    public void reset() {
        for (PhiModuleSetting setting : this.settings) {
            setting.reset();
        }
    }

    /**
     * @return true if the module has adjustable parameters and therefore shows the "more settings"
     *         entry.
     */
    public boolean hasSettings() {
        return !this.settings.isEmpty();
    }

    /**
     * @return the name of the module.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the short description below the name.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return true if the module is enabled.
     */
    public boolean isToggled() {
        return this.toggled;
    }

    /**
     * @param toggled the new state of the module.
     */
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    /**
     * @return an unmodifiable view of the adjustable parameters of the module.
     */
    public List<PhiModuleSetting> getSettings() {
        return Collections.unmodifiableList(this.settings);
    }
}
