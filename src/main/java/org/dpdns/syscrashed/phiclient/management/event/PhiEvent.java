package org.dpdns.syscrashed.phiclient.management.event;

import org.dpdns.syscrashed.phiclient.PhiClient;

/**
 * Base class for all events (package {@code org.dpdns.syscrashed.phiclient.management.event}).
 *
 * <p>Every concrete event (e.g. {@code EventTick}) extends this class. Dispatch is done with
 * {@code new EventXxx().call();} which forwards to the registered listeners of {@link EventManager}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #isCancelled()} - whether this event has been cancelled.</li>
 *   <li>{@link #setCancelled(boolean)} / {@link #cancel()} - cancel controls.</li>
 *   <li>{@link #call()} - dispatches this event to all registered listeners and returns itself.</li>
 * </ul>
 */
public abstract class PhiEvent {

    private boolean cancelled;

    /**
     * @return true if this event has been cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state.
     *
     * @param cancelled the new cancelled state.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Marks this event as cancelled.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Dispatches this event to every listener registered for its type.
     *
     * @return this event, for chaining.
     */
    public PhiEvent call() {
        return PhiClient.getInstance().getEventManager().call(this);
    }
}
