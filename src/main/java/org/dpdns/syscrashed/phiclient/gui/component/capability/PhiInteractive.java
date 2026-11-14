package org.dpdns.syscrashed.phiclient.gui.component.capability;

/**
 * Marker of a component that reacts to the pointer
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.capability}).
 *
 * <p>Components are read-only by default: a read-only component (the time, the weather, the
 * dimension, a progress bar) shows a value and never enters the hover feedback or runs an action.
 * A component that can be clicked implements this interface explicitly, so the difference between
 * interactive and read-only blocks is part of the type and not a boolean flag.</p>
 */
public interface PhiInteractive {

    /**
     * @return true if the component reacts to a click of the primary mouse button.
     */
    boolean acceptsClick();
}
