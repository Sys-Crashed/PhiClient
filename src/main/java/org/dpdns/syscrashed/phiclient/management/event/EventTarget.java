package org.dpdns.syscrashed.phiclient.management.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为事件监听器。
 * <p>
 * 被标记的方法必须满足：
 * 1. 恰好有 1 个参数；
 * 2. 该参数是 {@link PhiEvent} 的子类。
 * <p>
 * 不满足条件的方法会在注册时被跳过（并输出一条警告）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventTarget {

    /**
     * 监听器优先级，数值越小越先执行。
     */
    int priority() default 0;
}
