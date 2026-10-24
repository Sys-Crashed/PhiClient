package org.dpdns.syscrashed.phiclient.management.event.impl;

import org.dpdns.syscrashed.phiclient.management.event.PhiEvent;

/**
 * 每帧（Minecraft#runTick 末尾）派发的事件。
 *
 * 是事件总线的首个具体事件，用于验证 Hook → 总线 → 监听器 的闭环。
 */
public class EventTick extends PhiEvent {
}
