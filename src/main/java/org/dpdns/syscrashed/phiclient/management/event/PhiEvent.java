package org.dpdns.syscrashed.phiclient.management.event;

import org.dpdns.syscrashed.phiclient.PhiClient;

/**
 * 事件基类。所有具体事件（如 {@code EventTick}）都必须继承本类。
 * <p>
 * 派发方式：{@code new EventXxx().call();}
 */
public abstract class PhiEvent {

    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 把本事件派发给 {@link EventManager} 中注册的全部监听器。
     *
     * @return 事件自身，便于链式调用
     */
    public PhiEvent call() {
        return PhiClient.getInstance().getEventManager().call(this);
    }
}
