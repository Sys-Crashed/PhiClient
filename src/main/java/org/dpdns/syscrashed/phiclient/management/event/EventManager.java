package org.dpdns.syscrashed.phiclient.management.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Self-contained event bus (package {@code org.dpdns.syscrashed.phiclient.management.event}).
 *
 * <p>Responsible for registering / unregistering listeners and dispatching events.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>Listeners are discovered by reflection, scanning every {@link EventTarget} method and bucketing
 *       them by event type;</li>
 *   <li>within a bucket, listeners are sorted ascending by {@code priority} (lower runs first);</li>
 *   <li>an exception thrown by one listener never affects the others or the main game loop
 *       (exception isolation).</li>
 * </ul>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #register(Object)} - scans the object for {@link EventTarget} methods and registers them.</li>
 *   <li>{@link #unregister(Object)} - removes every listener bound to the given object.</li>
 *   <li>{@link #call(PhiEvent)} - dispatches an event to all matching listeners, returning it for chaining.</li>
 * </ul>
 */
public class EventManager {

    /** Internal holder binding a listener instance, its handler method and its priority. */
    private static final class Listener {

        private final Object instance;
        private final Method method;
        private final int priority;

        private Listener(Object instance, Method method, int priority) {
            this.instance = instance;
            this.method = method;
            this.priority = priority;
        }
    }

    /** Sorts listeners ascending by priority. */
    private static final Comparator<Listener> PRIORITY_COMPARATOR = new Comparator<Listener>() {
        @Override
        public int compare(Listener a, Listener b) {
            return Integer.compare(a.priority, b.priority);
        }
    };

    /** Event-type -&gt; ordered listener list. */
    private final Map<Class<?>, List<Listener>> registry = new ConcurrentHashMap<Class<?>, List<Listener>>();

    /**
     * Registers a listener object, scanning all of its {@link EventTarget} methods.
     *
     * @param listener the object whose annotated methods should receive events; ignored if {@code null}.
     */
    public void register(Object listener) {
        if (listener == null) {
            return;
        }

        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventTarget target = method.getAnnotation(EventTarget.class);
            if (target == null) {
                continue;
            }

            if (method.getParameterTypes().length != 1) {
                PhiLogger.warn("@EventTarget method must have exactly 1 parameter: " + describe(method));
                continue;
            }

            Class<?> eventType = method.getParameterTypes()[0];
            if (!PhiEvent.class.isAssignableFrom(eventType)) {
                PhiLogger.warn("@EventTarget method parameter must be a PhiEvent subclass: " + describe(method));
                continue;
            }

            method.setAccessible(true);

            List<Listener> bucket = registry.get(eventType);
            if (bucket == null) {
                bucket = new CopyOnWriteArrayList<Listener>();
                List<Listener> existing = registry.putIfAbsent(eventType, bucket);
                if (existing != null) {
                    bucket = existing;
                }
            }

            bucket.add(new Listener(listener, method, target.priority()));
            Collections.sort(bucket, PRIORITY_COMPARATOR);
        }
    }

    /**
     * Unregisters every listener bound to the given object.
     *
     * @param listener the object to remove; ignored if {@code null}.
     */
    public void unregister(Object listener) {
        if (listener == null) {
            return;
        }

        for (List<Listener> bucket : registry.values()) {
            for (Listener entry : bucket) {
                if (entry.instance == listener) {
                    bucket.remove(entry);
                }
            }
        }
    }

    /**
     * Dispatches an event to all listeners registered for its exact type.
     *
     * @param event the event to dispatch; ignored if {@code null}.
     * @param <T>   the event type.
     * @return the same event instance, for chaining.
     */
    @SuppressWarnings("unchecked")
    public <T extends PhiEvent> T call(T event) {
        if (event == null) {
            return null;
        }

        List<Listener> bucket = registry.get(event.getClass());
        if (bucket == null || bucket.isEmpty()) {
            return event;
        }

        for (Listener entry : bucket) {
            try {
                entry.method.invoke(entry.instance, event);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                PhiLogger.error("Failed to dispatch event: " + describe(entry.method), cause);
            } catch (Throwable t) {
                PhiLogger.error("Failed to dispatch event: " + describe(entry.method), t);
            }
        }

        return event;
    }

    /**
     * Builds a human-readable description of a method for log messages.
     *
     * @param method the method to describe.
     * @return a {@code declaringClass#name} string.
     */
    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
