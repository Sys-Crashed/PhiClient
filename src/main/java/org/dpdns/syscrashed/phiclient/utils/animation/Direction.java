package org.dpdns.syscrashed.phiclient.utils.animation;

/**
 * Animation direction enum (package {@code org.dpdns.syscrashed.phiclient.utils.animation}).
 *
 * <p>{@code FORWARDS} means the tween is progressing toward its end value, {@code BACKWARDS} means
 * it is returning to the start. Animations use this to decide whether to map the eased progress to
 * {@code 0 -> endPoint} or the reverse.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #opposite()} - returns the opposite direction.</li>
 * </ul>
 */
public enum Direction {

    FORWARDS,
    BACKWARDS;

    /**
     * @return the opposite direction ({@code FORWARDS} <-> {@code BACKWARDS}).
     */
    public Direction opposite() {
        return this == FORWARDS ? BACKWARDS : FORWARDS;
    }
}
