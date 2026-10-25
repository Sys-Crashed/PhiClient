package org.dpdns.syscrashed.phiclient.management.event.impl;

import org.dpdns.syscrashed.phiclient.management.event.PhiEvent;

/**
 * Per-frame event, dispatched at the end of every {@code Minecraft#runTick}
 * (package {@code org.dpdns.syscrashed.phiclient.management.event.impl}).
 *
 * <p>This is the first concrete event of the bus, used to verify the Hook -&gt; bus -&gt; listener
 * loop is wired correctly.</p>
 */
public class EventTick extends PhiEvent {
}
