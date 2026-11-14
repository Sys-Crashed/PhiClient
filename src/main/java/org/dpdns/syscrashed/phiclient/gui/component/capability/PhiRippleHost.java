package org.dpdns.syscrashed.phiclient.gui.component.capability;

/**
 * A component that owns a hover state machine
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.capability}).
 *
 * <p>The machine drives the tint of the hover feedback (the sheen owns the sweep itself). Components
 * that keep their own highlight - for example the navigation band, whose highlight belongs to a list
 * entry instead of a component - implement this interface so that every drawing site can read the
 * same machine instead of reimplementing it.</p>
 */
public interface PhiRippleHost {

    /**
     * @return the unified hover state machine of the component.
     */
    PhiHoverRipple getRipple();
}
