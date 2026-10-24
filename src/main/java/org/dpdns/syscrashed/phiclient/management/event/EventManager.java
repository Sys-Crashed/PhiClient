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
 * 自研事件总线：负责监听器的注册/注销与事件派发。
 *
 * 设计要点：
 * - 用反射扫描 {@link EventTarget} 方法，按事件类型分桶；
 * - 桶内按 priority 升序排序（数值越小越先执行）；
 * - 单个监听器抛异常不会影响其它监听器与游戏主流程（异常隔离）。
 */
public class EventManager {

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

    private static final Comparator<Listener> PRIORITY_COMPARATOR = new Comparator<Listener>() {
        @Override
        public int compare(Listener a, Listener b) {
            return Integer.compare(a.priority, b.priority);
        }
    };

    private final Map<Class<?>, List<Listener>> registry = new ConcurrentHashMap<Class<?>, List<Listener>>();

    /**
     * 注册一个监听对象，扫描其全部 {@link EventTarget} 方法。
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
     * 注销一个监听对象的全部监听器。
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
     * 派发事件给对应类型的全部监听器。
     *
     * @return 事件自身，便于链式调用
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

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
