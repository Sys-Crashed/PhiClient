package org.dpdns.syscrashed.phiclient.gui.module;

/**
 * A single adjustable parameter of a module (package {@code org.dpdns.syscrashed.phiclient.gui.module}).
 *
 * <p>A module either has no settings at all - then it is only a toggle switch - or it owns a list of
 * settings, which are shown in the "more settings" panel of the module page. Three kinds of settings
 * exist, mirroring the ones of the Soar client:</p>
 * <ul>
 *   <li>{@link Type#TOGGLE} - a boolean switch;</li>
 *   <li>{@link Type#SLIDER} - a float value inside {@code [min, max]};</li>
 *   <li>{@link Type#COMBO} - one entry selected out of a fixed list of options.</li>
 * </ul>
 *
 * <p>Every value remembers its default, so the whole module can be reset with {@link PhiModule#reset()}.</p>
 */
public class PhiModuleSetting {

    /**
     * The three kinds of settings.
     */
    public enum Type {
        /** A boolean switch. */
        TOGGLE,
        /** A float value inside {@code [min, max]}. */
        SLIDER,
        /** One entry selected out of a fixed list of options. */
        COMBO
    }

    /** The kind of the setting. */
    private final Type type;

    /** The name of the parameter. */
    private final String name;

    // ------------------------------------------------------------------ TOGGLE

    /** The current state of a toggle setting. */
    private boolean booleanValue;

    /** The default state of a toggle setting. */
    private final boolean defaultBoolean;

    // ------------------------------------------------------------------ SLIDER

    /** The current value of a slider setting. */
    private float floatValue;

    /** The default value of a slider setting. */
    private final float defaultFloat;

    /** The inclusive lower bound of a slider setting. */
    private final float min;

    /** The inclusive upper bound of a slider setting. */
    private final float max;

    /** The step of a slider setting (every value is rounded to a multiple of it). */
    private final float step;

    // ------------------------------------------------------------------ COMBO

    /** The fixed options of a combo setting. */
    private final String[] options;

    /** The index of the selected option. */
    private int selectedIndex;

    /** The default index of a combo setting. */
    private final int defaultIndex;

    /**
     * Creates a toggle setting.
     *
     * @param name  the name of the parameter.
     * @param value the default state.
     */
    public PhiModuleSetting(String name, boolean value) {
        this.type = Type.TOGGLE;
        this.name = name;
        this.booleanValue = value;
        this.defaultBoolean = value;
        this.defaultFloat = 0.0F;
        this.min = 0.0F;
        this.max = 0.0F;
        this.step = 0.0F;
        this.options = null;
        this.selectedIndex = 0;
        this.defaultIndex = 0;
    }

    /**
     * Creates a slider setting.
     *
     * @param name  the name of the parameter.
     * @param value the default value.
     * @param min   the inclusive lower bound.
     * @param max   the inclusive upper bound.
     * @param step  the step every value is rounded to.
     */
    public PhiModuleSetting(String name, float value, float min, float max, float step) {
        this.type = Type.SLIDER;
        this.name = name;
        this.floatValue = value;
        this.defaultFloat = value;
        this.min = min;
        this.max = max;
        this.step = step;
        this.booleanValue = false;
        this.defaultBoolean = false;
        this.options = null;
        this.selectedIndex = 0;
        this.defaultIndex = 0;
    }

    /**
     * Creates a combo setting.
     *
     * @param name    the name of the parameter.
     * @param options the fixed options to choose from.
     * @param index   the index of the default option.
     */
    public PhiModuleSetting(String name, String[] options, int index) {
        this.type = Type.COMBO;
        this.name = name;
        this.options = options;
        this.selectedIndex = index;
        this.defaultIndex = index;
        this.booleanValue = false;
        this.defaultBoolean = false;
        this.floatValue = 0.0F;
        this.defaultFloat = 0.0F;
        this.min = 0.0F;
        this.max = 0.0F;
        this.step = 0.0F;
    }

    /**
     * Restores the default value of the setting.
     */
    public void reset() {
        switch (this.type) {
            case TOGGLE:
                this.booleanValue = this.defaultBoolean;
                break;
            case SLIDER:
                this.floatValue = this.defaultFloat;
                break;
            case COMBO:
                this.selectedIndex = this.defaultIndex;
                break;
            default:
                break;
        }
    }

    /**
     * @return the kind of the setting.
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @return the name of the parameter.
     */
    public String getName() {
        return this.name;
    }

    // ------------------------------------------------------------------ TOGGLE

    /**
     * @return the current state of a toggle setting.
     */
    public boolean getBoolean() {
        return this.booleanValue;
    }

    /**
     * @param value the new state of a toggle setting.
     */
    public void setBoolean(boolean value) {
        this.booleanValue = value;
    }

    // ------------------------------------------------------------------ SLIDER

    /**
     * @return the current value of a slider setting.
     */
    public float getFloat() {
        return this.floatValue;
    }

    /**
     * Sets the value of a slider setting, clamped to the bounds and rounded to the step.
     *
     * @param value the new value.
     */
    public void setFloat(float value) {
        this.floatValue = clamp(value);
    }

    /**
     * @return the inclusive lower bound of a slider setting.
     */
    public float getMin() {
        return this.min;
    }

    /**
     * @return the inclusive upper bound of a slider setting.
     */
    public float getMax() {
        return this.max;
    }

    /**
     * @param value the raw value.
     * @return the value clamped to the bounds and rounded to the step.
     */
    public float clamp(float value) {
        float clamped = Math.max(this.min, Math.min(this.max, value));

        if (this.step > 0.0F) {
            clamped = Math.round((clamped - this.min) / this.step) * this.step + this.min;
        }

        return clamped;
    }

    // ------------------------------------------------------------------ COMBO

    /**
     * @return the fixed options of a combo setting.
     */
    public String[] getOptions() {
        return this.options;
    }

    /**
     * @return the index of the selected option.
     */
    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    /**
     * @param index the index of the option to select.
     */
    public void setSelectedIndex(int index) {
        if (this.options == null) {
            return;
        }

        this.selectedIndex = Math.max(0, Math.min(this.options.length - 1, index));
    }
}
