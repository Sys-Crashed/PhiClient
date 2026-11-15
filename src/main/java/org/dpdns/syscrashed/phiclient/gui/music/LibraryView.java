package org.dpdns.syscrashed.phiclient.gui.music;

/**
 * The list view that defines the effective playlist of the music playback
 * (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The view selects which subset of the library the transport controls and the random mode act on:
 * all tracks, only the favorite tracks, or the tracks of one named playlist.</p>
 */
public enum LibraryView {

    /** The whole library. */
    ALL,

    /** Only the tracks marked as favorite. */
    FAVORITE,

    /** The tracks of one named playlist (the name is carried separately). */
    PLAYLIST
}
