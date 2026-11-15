package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;

/**
 * One track of the music library (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>A track is identified by its path relative to the music folder (with {@code /} separators).
 * The favorite flag and the playlist memberships are stored by {@link PhiMusicLibrary} and reference
 * this identifier, never absolute paths, so the library keeps working when the game directory moves.
 * The display title, the artist and the duration come from ffprobe when it is available and fall
 * back to the file name otherwise.</p>
 */
public final class PhiTrack {

    /** The stable identifier: the path relative to the music folder ({@code /} separated). */
    private final String id;

    /** The audio file on disk. */
    private final File file;

    /** The file length captured when the track was built (staleness check). */
    private final long fileLength;

    /** The file modification time captured when the track was built (staleness check). */
    private final long fileModified;

    /** The display title (never null). */
    private String title;

    /** The artist (may be null, then the UI shows the local-music fallback). */
    private String artist;

    /** The duration in seconds (0 when unknown). */
    private double durationSeconds;

    /**
     * Creates a track from a file, with the file-name fallback as the display title.
     *
     * @param id   the stable identifier (path relative to the music folder).
     * @param file the audio file.
     */
    public PhiTrack(String id, File file) {
        this.id = id;
        this.file = file;
        this.fileLength = file.length();
        this.fileModified = file.lastModified();
        this.title = stripExtension(file.getName());
        this.artist = null;
        this.durationSeconds = 0.0D;
    }

    /**
     * @param file the file found by a later rescan.
     * @return true when the file still matches the captured fingerprint (length and mtime), so the
     *         probed metadata stays valid.
     */
    public boolean matchesFile(File file) {
        return file.length() == this.fileLength && file.lastModified() == this.fileModified;
    }

    /**
     * @return the stable identifier (path relative to the music folder).
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the audio file on disk.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * @return the display title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @return the artist, or {@code null} when unknown.
     */
    public String getArtist() {
        return this.artist;
    }

    /**
     * @return the duration in seconds (0 when unknown).
     */
    public double getDurationSeconds() {
        return this.durationSeconds;
    }

    /**
     * @return true when the metadata has been read (and the title is not the file-name fallback).
     */
    public boolean hasMetadata() {
        return this.artist != null || this.durationSeconds > 0.0D;
    }

    /**
     * Applies the probed metadata.
     *
     * @param metadata the ffprobe result, or {@code null} to keep the fallbacks.
     */
    public void applyMetadata(PhiTrackMetadata metadata) {
        if (metadata == null) {
            return;
        }
        if (metadata.title != null && !metadata.title.isEmpty()) {
            this.title = metadata.title;
        }
        if (metadata.artist != null && !metadata.artist.isEmpty()) {
            this.artist = metadata.artist;
        }
        if (metadata.durationSeconds > 0.0D) {
            this.durationSeconds = metadata.durationSeconds;
        }
    }

    /**
     * @return a companion image file next to the audio file (cover.jpg / cover.png), or {@code null}.
     */
    public File getCoverFile() {
        File folder = this.file.getParentFile();
        if (folder == null) {
            return null;
        }
        File jpg = new File(folder, "cover.jpg");
        if (jpg.isFile()) {
            return jpg;
        }
        File png = new File(folder, "cover.png");
        return png.isFile() ? png : null;
    }

    /**
     * Strips the extension of a file name.
     *
     * @param name the file name.
     * @return the name without its extension.
     */
    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
