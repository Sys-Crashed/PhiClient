package org.dpdns.syscrashed.phiclient.gui.music;

/**
 * The repeat mode of the music playback (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <ul>
 *   <li>{@link #SEQUENTIAL_LOOP} - plays the playlist in order and wraps to the first track.</li>
 *   <li>{@link #PSEUDO_RANDOM} - picks a random track after the current one (the current track is
 *       excluded, but a track can repeat within a round).</li>
 *   <li>{@link #SINGLE_LOOP} - replays the current track forever.</li>
 * </ul>
 */
public enum PlayMode {

    /** Sequential playback in playlist order, wrapping at the end. */
    SEQUENTIAL_LOOP,

    /** Random track selection after each track, excluding only the current one. */
    PSEUDO_RANDOM,

    /** Repeats the current track forever. */
    SINGLE_LOOP
}
