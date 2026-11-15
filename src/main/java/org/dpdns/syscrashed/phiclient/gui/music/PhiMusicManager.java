package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicImporter.ImportResult;

import net.minecraft.client.Minecraft;

/**
 * The global singleton facade of the music subsystem
 * (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The manager owns the one shared {@link PhiMusicPlayer} engine, the {@link PhiMusicLibrary}, the
 * {@link PhiMusicConfig} and the {@link PhiMusicImporter}, so the home page music card, the music
 * page and the per-tick housekeeping all observe the same playback state. Closing the hub screen and
 * returning to the main menu do not stop the music: {@link #update()} is driven by the client tick
 * and the playback only stops when the client shuts down ({@link #stopAndRelease()}).</p>
 *
 * <p>Selecting a library view ({@link #setView(LibraryView, String)}) only previews it: the visible
 * list changes, the playback queue does not. The queue is bound to the view that was last started
 * explicitly ({@link #playView(LibraryView, String)} or {@link #playTrack(String)}), so next/previous
 * and the pseudo-random mode always act on the list the user actually plays. Settings and user data
 * (favorites, playlists) are persisted through {@link PhiMusicConfig} on a debounced schedule and
 * immediately on {@link #stopAndRelease()}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #get()} - returns the singleton instance.</li>
 *   <li>{@link #update()} - per-tick housekeeping, called from the GUI manager.</li>
 *   <li>{@link #stopAndRelease()} - stops, releases and persists (shutdown).</li>
 *   <li>{@link #play()} / {@link #pause()} / {@link #toggle()} / {@link #next()} / {@link #previous()} - transport.</li>
 *   <li>{@link #seek(float)} / {@link #setVolume(int)} / {@link #setPlayMode(PlayMode)} - settings.</li>
 *   <li>{@link #setView(LibraryView, String)} / {@link #getView()} / {@link #getPlaylistName()} - the previewed library view.</li>
 *   <li>{@link #playView(LibraryView, String)} / {@link #playTrack(String)} - explicit playback starts.</li>
 *   <li>{@link #setFavorite(String, boolean)} / {@link #isFavorite(String)} - favorites.</li>
 *   <li>{@link #createPlaylist(String)} / {@link #renamePlaylist(String, String)} / {@link #deletePlaylist(String)} / {@link #addToPlaylist(String, String)} / {@link #removeFromPlaylist(String, String)} / {@link #moveInPlaylist(String, String, boolean)} - named playlists.</li>
 *   <li>{@link #importViaDialog()} / {@link #openMusicFolder()} / {@link #installDropTarget()} / {@link #setDropEnabled(boolean)} / {@link #pollImportResult()} - imports.</li>
 *   <li>{@link #getProgress()} / {@link #getTracks()} / {@link #getPlaylists()} / {@link #getMusicFolder()} / {@link #getPlayer()} - snapshots for the UI.</li>
 * </ul>
 */
public final class PhiMusicManager {

    /** How often the dirty config is written at most (in milliseconds). */
    private static final long SAVE_INTERVAL = 5000L;

    private static final PhiMusicManager INSTANCE = new PhiMusicManager();

    private final PhiMusicPlayer player = new PhiMusicPlayer();

    private final PhiMusicLibrary library;

    private final PhiMusicConfig config;

    private final PhiMusicImporter importer;

    /** The current library view (the list the UI previews; the playback queue is not touched). */
    private LibraryView view = LibraryView.ALL;

    /** The name of the selected named playlist when {@link #view} is {@link LibraryView#PLAYLIST}. */
    private String playlistName = "";

    /** The library view the playback queue is bound to (set by the explicit play actions). */
    private LibraryView playingView = LibraryView.ALL;

    /** The playlist name of {@link #playingView} when it is {@link LibraryView#PLAYLIST}. */
    private String playingPlaylistName = "";

    /** The library revision the engine list was last synced with. */
    private long syncedRevision = -1L;

    /** True when settings or user data changed since the last write. */
    private boolean dirty;

    /** The timestamp of the last config write. */
    private long lastSave;

    private PhiMusicManager() {
        File gameDir = Minecraft.getMinecraft().mcDataDir;
        File musicFolder = new File(gameDir, "PhiClient" + File.separator + "music");
        this.library = new PhiMusicLibrary(musicFolder);
        this.config = new PhiMusicConfig(gameDir);
        this.importer = new PhiMusicImporter(musicFolder);

        // Restore the persisted state.
        PhiMusicConfig.State state = this.config.load();
        this.player.setVolume(state.volume / 100.0F);
        this.player.setPlayMode(state.playMode);
        this.library.restoreFavorites(state.favorites);
        this.library.restorePlaylists(state.playlists);
        this.view = state.view;
        this.playlistName = state.playlistName;
        if (this.view == LibraryView.PLAYLIST && this.library.getPlaylist(this.playlistName) == null) {
            // The persisted playlist no longer exists - fall back to the full library.
            this.view = LibraryView.ALL;
            this.playlistName = "";
        }
        this.playingView = this.view;
        this.playingPlaylistName = this.playlistName;

        // Build the track list and restore the last played track (without starting it).
        this.library.rescanNow();
        syncTrackList(true);
        if (!state.lastTrackId.isEmpty()) {
            List<PhiTrack> effective = this.library.getEffectiveTracks(this.playingView, this.playingPlaylistName);
            for (int i = 0; i < effective.size(); i++) {
                if (effective.get(i).getId().equals(state.lastTrackId)) {
                    this.player.selectIndex(i);
                    break;
                }
            }
        }

        this.lastSave = System.currentTimeMillis();
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
     * @return the track library.
     */
    public PhiMusicLibrary getLibrary() {
        return this.library;
    }

    /**
     * @return the folder the music files live in.
     */
    public File getMusicFolder() {
        return this.library.getMusicFolder();
    }

    /**
     * Per-tick housekeeping: rescans the music folder (throttled), applies the probed metadata,
     * keeps the engine list in sync with the library view, advances to the next track when the
     * current one ended and persists the debounced config. Called from the client tick so the
     * playback survives closing the hub.
     */
    public void update() {
        this.library.rescan();
        if (this.importer.consumePendingImport()) {
            this.library.rescanNow();
        }
        syncTrackList(false);
        this.library.drainMetadata();
        this.player.update();
        saveIfDirty();
    }

    /**
     * Stops the playback, releases the audio resources and persists the current state. Idempotent -
     * called on client shutdown only; returning to the main menu keeps the playback running.
     */
    public void stopAndRelease() {
        this.player.stopAndRelease();
        saveNow();
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
     * @return the ratio of the seek that the engine has not applied yet, or -1 when no seek is
     *         pending.
     */
    public float getPendingSeekRatio() {
        return this.player.getPendingSeekRatio();
    }

    /**
     * Sets the playback volume.
     *
     * @param percent the volume in percent {@code [0, 100]} (clamped).
     */
    public void setVolume(int percent) {
        this.player.setVolume(Math.max(0, Math.min(100, percent)) / 100.0F);
        markDirty();
    }

    /**
     * @return the playback volume in percent {@code [0, 100]}.
     */
    public int getVolumePercent() {
        return this.player.getVolumePercent();
    }

    /**
     * Sets the repeat mode. The current playback position is not touched.
     *
     * @param mode the new repeat mode.
     */
    public void setPlayMode(PlayMode mode) {
        this.player.setPlayMode(mode);
        markDirty();
    }

    /**
     * @return the current repeat mode.
     */
    public PlayMode getPlayMode() {
        return this.player.getPlayMode();
    }

    /**
     * Selects the current library view for previewing only. The visible list changes, the playback
     * queue is not touched - the transport keeps acting on the list that was last started
     * explicitly (see {@link #playView}).
     *
     * @param view         the view (all tracks, favorites or one named playlist).
     * @param playlistName the name of the named playlist when {@code view} is
     *                     {@link LibraryView#PLAYLIST}, ignored otherwise.
     */
    public void setView(LibraryView view, String playlistName) {
        this.view = view != null ? view : LibraryView.ALL;
        this.playlistName = playlistName != null ? playlistName : "";
        if (this.view == LibraryView.PLAYLIST && this.library.getPlaylist(this.playlistName) == null) {
            this.view = LibraryView.ALL;
            this.playlistName = "";
        }
        markDirty();
    }

    /**
     * Starts the playback of the given library view: the playback queue is replaced with the
     * effective files of the view and the first track starts immediately, interrupting the current
     * one. This is the explicit "play this playlist" action - unlike {@link #setView}, which only
     * previews.
     *
     * @param view         the view to play (all tracks, favorites or one named playlist).
     * @param playlistName the name of the named playlist when {@code view} is
     *                     {@link LibraryView#PLAYLIST}, ignored otherwise.
     * @return true when the view is not empty and its first track started.
     */
    public boolean playView(LibraryView view, String playlistName) {
        setView(view, playlistName);
        this.playingView = this.view;
        this.playingPlaylistName = this.playlistName;
        syncTrackList(true);
        if (this.player.getTrackCount() == 0) {
            return false;
        }
        this.player.playIndex(0);
        return true;
    }

    /**
     * Starts the playback of the given track of the currently previewed view. The playback queue is
     * first bound to the previewed view, so the index is resolved inside the queue the track belongs
     * to (clicking a song of a previewed playlist plays exactly that song).
     *
     * @param trackId the track identifier.
     * @return true when the track exists in the previewed view and started.
     */
    public boolean playTrack(String trackId) {
        if (this.playingView != this.view || !this.playingPlaylistName.equals(this.playlistName)) {
            this.playingView = this.view;
            this.playingPlaylistName = this.playlistName;
            syncTrackList(true);
        }
        List<File> queue = this.player.getTracks();
        for (int i = 0; i < queue.size(); i++) {
            if (PhiMusicLibrary.toId(this.library.getMusicFolder(), queue.get(i)).equals(trackId)) {
                this.player.playIndex(i);
                return true;
            }
        }
        return false;
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
     * @return the tracks of the current library view.
     */
    public List<PhiTrack> getTracks() {
        return this.library.getEffectiveTracks(this.view, this.playlistName);
    }

    /**
     * Marks a track as favorite or not.
     *
     * @param trackId the track identifier.
     * @param value   true to favorite, false to unfavorite.
     */
    public void setFavorite(String trackId, boolean value) {
        this.library.setFavorite(trackId, value);
        markDirty();
    }

    /**
     * @param trackId a track identifier.
     * @return true when the track is a favorite.
     */
    public boolean isFavorite(String trackId) {
        return this.library.isFavorite(trackId);
    }

    /**
     * @return the named playlists in creation order.
     */
    public List<PhiPlaylist> getPlaylists() {
        return this.library.getPlaylists();
    }

    /**
     * Creates a named playlist.
     *
     * @param name the display name (must be unique and non-empty).
     * @return true when the playlist was created.
     */
    public boolean createPlaylist(String name) {
        if (this.library.createPlaylist(name)) {
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Renames a named playlist.
     *
     * @param oldName the current name.
     * @param newName the new name (must be unique and non-empty).
     * @return true when the playlist was renamed.
     */
    public boolean renamePlaylist(String oldName, String newName) {
        if (this.library.renamePlaylist(oldName, newName)) {
            if (this.playlistName.equals(oldName)) {
                this.playlistName = newName;
            }
            if (this.playingPlaylistName.equals(oldName)) {
                this.playingPlaylistName = newName;
            }
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Deletes a named playlist.
     *
     * @param name the playlist name.
     * @return true when the playlist existed.
     */
    public boolean deletePlaylist(String name) {
        if (this.library.deletePlaylist(name)) {
            boolean playingIt = this.playingPlaylistName.equals(name);
            if (this.playlistName.equals(name)) {
                this.playlistName = "";
                this.view = LibraryView.ALL;
            }
            if (playingIt) {
                this.playingView = LibraryView.ALL;
                this.playingPlaylistName = "";
            }
            markDirty();
            if (playingIt) {
                syncTrackList(true);
            }
            return true;
        }
        return false;
    }

    /**
     * Physically deletes a track from the music folder (irreversible - the caller is expected to
     * confirm with the user first). When the track is the current one of the playback engine, the
     * decoder is stopped and released beforehand, so the open file handle cannot block the deletion;
     * the playback queue is rebuilt afterwards.
     *
     * @param trackId the track identifier.
     * @return true when the track file is gone and the queue was rebuilt.
     */
    public boolean deleteTrack(String trackId) {
        File current = this.player.getCurrentTrack();
        if (current != null && PhiMusicLibrary.toId(this.library.getMusicFolder(), current).equals(trackId)) {
            this.player.stopAndRelease();
        }
        if (!this.library.deleteTrack(trackId)) {
            return false;
        }
        syncTrackList(true);
        markDirty();
        return true;
    }

    /**
     * Appends a track to a named playlist.
     *
     * @param playlistName the playlist name.
     * @param trackId      the track identifier.
     * @return true when the track was added.
     */
    public boolean addToPlaylist(String playlistName, String trackId) {
        if (this.library.addToPlaylist(playlistName, trackId)) {
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Removes a track from a named playlist.
     *
     * @param playlistName the playlist name.
     * @param trackId      the track identifier.
     * @return true when the track was present and got removed.
     */
    public boolean removeFromPlaylist(String playlistName, String trackId) {
        if (this.library.removeFromPlaylist(playlistName, trackId)) {
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Moves a track one position up or down inside a named playlist.
     *
     * @param playlistName the playlist name.
     * @param trackId      the track identifier.
     * @param up           true to move towards the beginning.
     * @return true when the track moved.
     */
    public boolean moveInPlaylist(String playlistName, String trackId, boolean up) {
        if (this.library.moveInPlaylist(playlistName, trackId, up)) {
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Opens the system file dialog (multi-select) and imports the picked files into the library.
     * Blocks until the dialog closes.
     *
     * @return the import result, or {@code null} when the dialog was cancelled or unavailable.
     */
    public ImportResult importViaDialog() {
        ImportResult result = this.importer.importViaDialog();
        if (result != null && result.getImported() > 0) {
            this.library.rescanNow();
            syncTrackList(true);
        }
        return result;
    }

    /**
     * Opens the music folder in the system file manager.
     *
     * @return true when the folder could be opened.
     */
    public boolean openMusicFolder() {
        return this.importer.openMusicFolder();
    }

    /**
     * Installs the drag-and-drop import on the game window. Idempotent.
     *
     * @return true when drag and drop is available.
     */
    public boolean installDropTarget() {
        return this.importer.installDropTarget();
    }

    /**
     * Enables or disables the drop import (toggle it while the hub is on screen).
     *
     * @param enabled true to accept files.
     */
    public void setDropEnabled(boolean enabled) {
        this.importer.setDropEnabled(enabled);
    }

    /**
     * @return the last import result and clears it (null when nothing is pending) - used by the UI
     *         to show an import toast.
     */
    public ImportResult pollImportResult() {
        return this.importer.pollLastResult();
    }

    /**
     * @return true when import collisions are waiting for the user's decision (overwrite/cancel).
     */
    public boolean hasPendingImportConflicts() {
        return this.importer.hasPendingConflicts();
    }

    /**
     * @return the next import collision to decide, or {@code null} when none is pending.
     */
    public PhiMusicImporter.ImportConflict pollNextImportConflict() {
        return this.importer.pollNextConflict();
    }

    /**
     * Applies the user's decision for an import collision: overwrite the existing file or cancel the
     * import. Must be called on the client thread.
     *
     * @param conflict  the collision (from {@link #pollNextImportConflict()}).
     * @param overwrite true to replace the existing file, false to cancel the import.
     * @return true when the decision was applied (overwritten or skipped), false on copy failure.
     */
    public boolean resolveImportConflict(PhiMusicImporter.ImportConflict conflict, boolean overwrite) {
        boolean applied = this.importer.resolveConflict(conflict, overwrite);
        if (applied && overwrite) {
            this.library.rescanNow();
            syncTrackList(true);
        }
        return applied;
    }

    /**
     * @return the progress of the current track in the range {@code [0, 1]}.
     */
    public float getProgress() {
        return this.player.getProgress();
    }

    /**
     * Keeps the engine track list in sync with the current library view.
     *
     * @param force true to replace the list even when the library revision did not change.
     */
    private void syncTrackList(boolean force) {
        long revision = this.library.getRevision();
        if (!force && revision == this.syncedRevision) {
            return;
        }
        this.syncedRevision = revision;
        this.player.replaceTracks(this.library.getEffectiveFiles(this.playingView, this.playingPlaylistName));
    }

    /**
     * Marks the config dirty (written within {@link #SAVE_INTERVAL} by the tick).
     */
    private void markDirty() {
        this.dirty = true;
    }

    /**
     * Writes the config when it is dirty and the debounce window passed.
     */
    private void saveIfDirty() {
        if (!this.dirty || System.currentTimeMillis() - this.lastSave < SAVE_INTERVAL) {
            return;
        }
        saveNow();
    }

    /**
     * Writes the config immediately.
     */
    private void saveNow() {
        this.lastSave = System.currentTimeMillis();
        this.dirty = false;

        String lastTrackId = "";
        File current = this.player.getCurrentTrack();
        if (current != null) {
            lastTrackId = PhiMusicLibrary.toId(this.library.getMusicFolder(), current);
        }

        this.config.save(
                this.player.getVolumePercent(),
                this.player.getPlayMode(),
                this.view,
                this.playlistName,
                this.library.getFavorites(),
                this.library.getPlaylists(),
                lastTrackId);
    }
}
