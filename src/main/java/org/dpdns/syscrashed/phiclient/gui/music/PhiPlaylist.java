package org.dpdns.syscrashed.phiclient.gui.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named playlist of the music library (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>A playlist keeps an ordered list of track identifiers (see {@link PhiTrack#getId()}); the
 * identifiers are resolved against the library when the playlist is displayed, so tracks removed
 * from disk simply disappear instead of breaking the list. Mutations go through
 * {@link PhiMusicLibrary} so the persistence stays consistent - this class is the plain container.</p>
 */
public final class PhiPlaylist {

    /** The display name of the playlist. */
    private String name;

    /** The ordered track identifiers. */
    private final List<String> tracks = new ArrayList<String>();

    /**
     * Creates an empty playlist.
     *
     * @param name the display name.
     */
    public PhiPlaylist(String name) {
        this.name = name;
    }

    /**
     * @return the display name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Renames the playlist.
     *
     * @param name the new display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the ordered track identifiers.
     */
    public List<String> getTracks() {
        return this.tracks;
    }

    /**
     * @param trackId a track identifier.
     * @return true when the playlist contains the track.
     */
    public boolean contains(String trackId) {
        return this.tracks.contains(trackId);
    }

    /**
     * Appends a track to the end of the playlist.
     *
     * @param trackId the track identifier.
     * @return true when the track was added, false when it already exists.
     */
    public boolean addTrack(String trackId) {
        if (this.tracks.contains(trackId)) {
            return false;
        }
        return this.tracks.add(trackId);
    }

    /**
     * Removes a track from the playlist.
     *
     * @param trackId the track identifier.
     * @return true when the track was present and got removed.
     */
    public boolean removeTrack(String trackId) {
        return this.tracks.remove(trackId);
    }

    /**
     * Moves a track one position up or down.
     *
     * @param trackId the track identifier.
     * @param up      true to move towards the beginning, false towards the end.
     * @return true when the track moved (false when it is already at the border).
     */
    public boolean moveTrack(String trackId, boolean up) {
        int index = this.tracks.indexOf(trackId);
        if (index < 0) {
            return false;
        }
        int target = up ? index - 1 : index + 1;
        if (target < 0 || target >= this.tracks.size()) {
            return false;
        }
        Collections.swap(this.tracks, index, target);
        return true;
    }

    /**
     * Removes every identifier that no longer exists in the library.
     *
     * @param knownIds the identifiers currently present in the library.
     */
    public void retainKnown(List<String> knownIds) {
        this.tracks.retainAll(knownIds);
    }
}
