package org.dpdns.syscrashed.phiclient.management.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener (package {@code org.dpdns.syscrashed.phiclient.management.event}).
 *
 * <p>The annotated method must satisfy:</p>
 * <ol>
 *   <li>it has exactly one parameter;</li>
 *   <li>that parameter is a subclass of {@link PhiEvent}.</li>
 * </ol>
 *
 * <p>Methods that do not satisfy these are skipped at registration time (with a warning logged).</p>
 *
 * <p>Provided members:</p>
 * <ul>
 *   <li>{@link #priority()} - the listener priority; lower values run first.</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventTarget {

    /**
     * Listener priority. Lower numbers are invoked earlier.
     *
     * @return the priority value.
     */
    int priority() default 0;
}
