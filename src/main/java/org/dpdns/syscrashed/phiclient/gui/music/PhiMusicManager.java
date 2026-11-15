package org.dpdns.syscrashed.phiclient.gui.music;

/**
 * The global singleton facade of the music subsystem
 * (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The manager owns the one shared {@link PhiMusicPlayer} engine, so the home page music card, the
 * music page and the per-tick housekeeping all observe the same playback state. Closing the hub
 * screen does not stop the music: {@link #update()} is driven by the client tick and the playback
 * only stops when the game returns to the main menu or the client shuts down (both call
 * {@link #stopAndRelease()}).</p>
 *
 * <p>All UI reads go through {@link #getPlayer()} snapshots or the delegating methods below; the
 * transport and the settings are delegated to the engine. The current library view
 * ({@link #setView(LibraryView, String)}) is stored here and applied to the track list by the
 * library layer.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #get()} - returns the singleton instance.</li>
 *   <li>{@link #update()} - per-tick housekeeping, called from the GUI manager.</li>
 *   <li>{@link #stopAndRelease()} - stops and releases the playback (main menu, shutdown).</li>
 *   <li>{@link #play()} / {@link #pause()} / {@link #toggle()} / {@link #next()} / {@link #previous()} - transport.</li>
 *   <li>{@link #seek(float)} / {@link #setVolume(int)} / {@link #setPlayMode(PlayMode)} - settings.</li>
 *   <li>{@link #setView(LibraryView, String)} / {@link #getView()} / {@link #getPlaylistName()} - the library view.</li>
 *   <li>{@link #getProgress()} - the progress of the current track in {@code [0, 1]}.</li>
 * </ul>
 */
public final class PhiMusicManager {

    private static final PhiMusicManager INSTANCE = new PhiMusicManager();

    private final PhiMusicPlayer player = new PhiMusicPlayer();

    /** The current library view (applied to the track list by the library layer). */
    private LibraryView view = LibraryView.ALL;

    /** The name of the selected named playlist when {@link #view} is {@link LibraryView#PLAYLIST}. */
    private String playlistName = "";

    private PhiMusicManager() {
    }

    /**
     * @return the singleton instance.
     */
    public static PhiMusicManager get() {
        return INSTANCE;
    }

    /**
     * @return the shared playback engine (read its snapshots, delegate the transport).
     */
    public PhiMusicPlayer getPlayer() {
        return this.player;
    }

    /**
     * Per-tick housekeeping: rescans the music folder and advances to the next track when the
     * current one ended. Called from the client tick so the playback survives closing the hub.
     */
    public void update() {
        this.player.update();
    }

    /**
     * Stops the playback and releases the audio resources. Idempotent - called when the game
     * returns to the main menu and on client shutdown.
     */
    public void stopAndRelease() {
        this.player.stopAndRelease();
    }

    /**
     * Starts the playback of the current track (or of the first one).
     */
    public void play() {
        this.player.play();
    }

    /**
     * Pauses the playback. The position of the track is kept.
     */
    public void pause() {
        this.player.pause();
    }

    /**
     * Starts or pauses the playback.
     */
    public void toggle() {
        this.player.toggle();
    }

    /**
     * Plays the next track according to the current {@link PlayMode}.
     */
    public void next() {
        this.player.next();
    }

    /**
     * Plays the previous track of the playlist.
     */
    public void previous() {
        this.player.previous();
    }

    /**
     * Seeks to the given ratio of the current track.
     *
     * @param ratio the target position in {@code [0, 1]} (clamped).
     */
    public void seek(float ratio) {
        this.player.seek(ratio);
    }

    /**
     * Sets the playback volume.
     *
     * @param percent the volume in percent {@code [0, 100]} (clamped).
     */
    public void setVolume(int percent) {
        this.player.setVolume(Math.max(0, Math.min(100, percent)) / 100.0F);
    }

    /**
     * Sets the repeat mode. The current playback position is not touched.
     *
     * @param mode the new repeat mode.
     */
    public void setPlayMode(PlayMode mode) {
        this.player.setPlayMode(mode);
    }

    /**
     * Sets the current library view.
     *
     * @param view         the view (all tracks, favorites or one named playlist).
     * @param playlistName the name of the named playlist when {@code view} is
     *                     {@link LibraryView#PLAYLIST}, ignored otherwise.
     */
    public void setView(LibraryView view, String playlistName) {
        this.view = view != null ? view : LibraryView.ALL;
        this.playlistName = playlistName != null ? playlistName : "";
    }

    /**
     * @return the current library view.
     */
    public LibraryView getView() {
        return this.view;
    }

    /**
     * @return the name of the selected named playlist (empty when the view is not a playlist).
     */
    public String getPlaylistName() {
        return this.playlistName;
    }

    /**
     * @return the progress of the current track in the range {@code [0, 1]}.
     */
    public float getProgress() {
        return this.player.getProgress();
    }
}
