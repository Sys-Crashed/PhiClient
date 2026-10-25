package org.dpdns.syscrashed.phiclient.gui.component;

/**
 * A component that can be scrolled with the mouse wheel
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Components that implement this interface receive the wheel events of the hub before the
 * navigation band does, so a list inside a page can be scrolled independently.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #onWheel(float, int, int)} - handles a wheel event.</li>
 * </ul>
 */
public interface PhiScrollable {

    /**
     * Handles a wheel event.
     *
     * @param notches the number of wheel notches (positive scrolls towards the end of the content).
     * @param mouseX  the pointer x.
     * @param mouseY  the pointer y.
     * @return true if the wheel was consumed by the component.
     */
    boolean onWheel(float notches, int mouseX, int mouseY);
}
