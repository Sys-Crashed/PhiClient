package org.dpdns.syscrashed.phiclient.gui.component;

/**
 * The scrollable text capability of a component
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Components that show a text list longer than their viewport implement this interface - it is
 * the explicit counterpart of the non scrollable text. The page dispatches the wheel events through
 * this interface before the navigation band receives them, so a list inside a page can be scrolled
 * independently. The signature must stay in this package, because the dispatch relies on
 * {@code instanceof PhiScrollable}.</p>
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
