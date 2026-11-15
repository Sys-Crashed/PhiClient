package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * The track library of the music subsystem (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The library scans the music folder, keeps the tracks keyed by their stable identifier (the path
 * relative to the music folder) and maintains the favorite set and the named playlists. The effective
 * track list of the current {@link LibraryView} is resolved here and synced into the
 * {@link PhiMusicPlayer} by {@link PhiMusicManager}.</p>
 *
 * <p>Metadata probing (ffprobe) runs on a background thread; the results are applied on the client
 * tick via {@link #drainMetadata()}, so the UI never blocks and the track fields are only mutated on
 * the main thread. A track whose file did not change (length and modification time) keeps its probed
 * metadata across rescans.</p>
 */
public final class PhiMusicLibrary {

    /** How often the music folder is checked for changes (in milliseconds). */
    public static final long RESCAN_INTERVAL = 4000L;

    private final File musicFolder;

    /** All tracks of the library, keyed by their stable identifier (insertion order = sort order). */
    private final Map<String, PhiTrack> tracksById = new LinkedHashMap<String, PhiTrack>();

    /** The identifiers of the favorite tracks. */
    private final Set<String> favorites = new LinkedHashSet<String>();

    /** The named playlists, keyed by their name (insertion order = creation order). */
    private final Map<String, PhiPlaylist> playlists = new LinkedHashMap<String, PhiPlaylist>();

    /** Probed metadata waiting to be applied on the main thread. */
    private final Queue<MetadataJob> pendingMetadata = new ConcurrentLinkedQueue<MetadataJob>();

    /** The single background thread that runs ffprobe. */
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("PhiMusicProbe"));

    private long lastScan;

    /** Bumped on every change that may affect the effective track list. */
    private long revision;

    /**
     * Creates the library for the given music folder and makes sure the folder exists.
     *
     * @param musicFolder the folder with the audio files.
     */
    public PhiMusicLibrary(File musicFolder) {
        this.musicFolder = musicFolder;
        if (!this.musicFolder.exists()) {
            this.musicFolder.mkdirs();
        }
        this.lastScan = 0L;
    }

    /**
     * @return the folder with the audio files.
     */
    public File getMusicFolder() {
        return this.musicFolder;
    }

    /**
     * @return a revision counter that changes whenever the effective track list may have changed.
     */
    public long getRevision() {
        return this.revision;
    }

    /**
     * Rescans the music folder, throttled by {@link #RESCAN_INTERVAL}.
     */
    public void rescan() {
        if (System.currentTimeMillis() - this.lastScan < RESCAN_INTERVAL) {
            return;
        }
        rescanNow();
    }

    /**
     * Rescans the music folder immediately (used after an import).
     */
    public void rescanNow() {
        this.lastScan = System.currentTimeMillis();
        List<File> found = listAudioFiles();
        Collections.sort(found, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
        });

        boolean changed = false;
        Map<String, PhiTrack> rebuilt = new LinkedHashMap<String, PhiTrack>();
        for (File file : found) {
            String id = toId(this.musicFolder, file);
            PhiTrack existing = this.tracksById.get(id);
            if (existing != null && !existing.getFile().equals(file)) {
                existing = null; // defensive: same id but different location
            }
            if (existing != null && existing.matchesFile(file)) {
                // keep the object, so the probed metadata survives
                rebuilt.put(id, existing);
                continue;
            }
            PhiTrack track = new PhiTrack(id, file);
            rebuilt.put(id, track);
            changed = true;
            enqueueProbe(track, file);
        }
        if (rebuilt.size() != this.tracksById.size()) {
            changed = true;
        }
        this.tracksById.clear();
        this.tracksById.putAll(rebuilt);

        // Clean the references of the files that disappeared.
        this.favorites.retainAll(this.tracksById.keySet());
        List<String> known = new ArrayList<String>(this.tracksById.keySet());
        for (PhiPlaylist playlist : this.playlists.values()) {
            playlist.retainKnown(known);
        }

        if (changed) {
            this.revision++;
        }
    }

    /**
     * @return all tracks of the library in sort order.
     */
    public List<PhiTrack> getAllTracks() {
        return new ArrayList<PhiTrack>(this.tracksById.values());
    }

    /**
     * @param id a track identifier.
     * @return the track, or {@code null} when unknown.
     */
    public PhiTrack getTrack(String id) {
        return this.tracksById.get(id);
    }

    /**
     * Resolves the effective track list of a library view.
     *
     * @param view         the view.
     * @param playlistName the playlist name for {@link LibraryView#PLAYLIST}, ignored otherwise.
     * @return the tracks the transport acts on.
     */
    public List<PhiTrack> getEffectiveTracks(LibraryView view, String playlistName) {
        List<PhiTrack> all = getAllTracks();
        switch (view) {
            case FAVORITE:
                List<PhiTrack> favored = new ArrayList<PhiTrack>();
                for (PhiTrack track : all) {
                    if (this.favorites.contains(track.getId())) {
                        favored.add(track);
                    }
                }
                return favored;
            case PLAYLIST:
                PhiPlaylist playlist = this.playlists.get(playlistName);
                List<PhiTrack> resolved = new ArrayList<PhiTrack>();
                if (playlist != null) {
                    for (String id : playlist.getTracks()) {
                        PhiTrack track = this.tracksById.get(id);
                        if (track != null) {
                            resolved.add(track);
                        }
                    }
                }
                return resolved;
            case ALL:
            default:
                return all;
        }
    }

    /**
     * Resolves the effective file list of a library view (for the playback engine).
     *
     * @param view         the view.
     * @param playlistName the playlist name for {@link LibraryView#PLAYLIST}, ignored otherwise.
     * @return the files the transport acts on.
     */
    public List<File> getEffectiveFiles(LibraryView view, String playlistName) {
        List<PhiTrack> tracks = getEffectiveTracks(view, playlistName);
        List<File> files = new ArrayList<File>(tracks.size());
        for (PhiTrack track : tracks) {
            files.add(track.getFile());
        }
        return files;
    }

    /**
     * Applies the probed metadata that arrived since the last call. Called on the client tick.
     */
    public void drainMetadata() {
        MetadataJob job;
        while ((job = this.pendingMetadata.poll()) != null) {
            PhiTrack track = this.tracksById.get(job.trackId);
            if (track != null) {
                track.applyMetadata(job.metadata);
            }
        }
    }

    /**
     * Marks a track as favorite or not.
     *
     * @param trackId the track identifier.
     * @param value   true to favorite, false to unfavorite.
     */
    public void setFavorite(String trackId, boolean value) {
        if (!this.tracksById.containsKey(trackId)) {
            return;
        }
        if (value) {
            if (this.favorites.add(trackId)) {
                this.revision++;
            }
        } else {
            if (this.favorites.remove(trackId)) {
                this.revision++;
            }
        }
    }

    /**
     * @param trackId a track identifier.
     * @return true when the track is a favorite.
     */
    public boolean isFavorite(String trackId) {
        return this.favorites.contains(trackId);
    }

    /**
     * @return the identifiers of the favorite tracks.
     */
    public Set<String> getFavorites() {
        return this.favorites;
    }

    /**
     * Restores the favorite set from the persisted config (only used at startup).
     *
     * @param ids the persisted identifiers.
     */
    public void restoreFavorites(Set<String> ids) {
        this.favorites.clear();
        if (ids != null) {
            this.favorites.addAll(ids);
        }
    }

    /**
     * Restores the named playlists from the persisted config (only used at startup).
     *
     * @param loaded the persisted playlists.
     */
    public void restorePlaylists(List<PhiPlaylist> loaded) {
        this.playlists.clear();
        if (loaded != null) {
            for (PhiPlaylist playlist : loaded) {
                this.playlists.put(playlist.getName(), playlist);
            }
        }
    }

    /**
     * @return the named playlists in creation order.
     */
    public List<PhiPlaylist> getPlaylists() {
        return new ArrayList<PhiPlaylist>(this.playlists.values());
    }

    /**
     * @param name a playlist name.
     * @return the playlist, or {@code null} when unknown.
     */
    public PhiPlaylist getPlaylist(String name) {
        return this.playlists.get(name);
    }

    /**
     * Creates a named playlist.
     *
     * @param name the display name (must be unique and non-empty).
     * @return true when the playlist was created.
     */
    public boolean createPlaylist(String name) {
        if (name == null || name.trim().isEmpty() || this.playlists.containsKey(name)) {
            return false;
        }
        this.playlists.put(name, new PhiPlaylist(name));
        this.revision++;
        return true;
    }

    /**
     * Renames a named playlist.
     *
     * @param oldName the current name.
     * @param newName the new name (must be unique and non-empty).
     * @return true when the playlist was renamed.
     */
    public boolean renamePlaylist(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty() || this.playlists.containsKey(newName)) {
            return false;
        }
        PhiPlaylist playlist = this.playlists.remove(oldName);
        if (playlist == null) {
            return false;
        }
        playlist.setName(newName);
        this.playlists.put(newName, playlist);
        this.revision++;
        return true;
    }

    /**
     * Deletes a named playlist.
     *
     * @param name the playlist name.
     * @return true when the playlist existed.
     */
    public boolean deletePlaylist(String name) {
        if (this.playlists.remove(name) != null) {
            this.revision++;
            return true;
        }
        return false;
    }

    /**
     * Appends a track to a named playlist.
     *
     * @param playlistName the playlist name.
     * @param trackId      the track identifier.
     * @return true when the track was added.
     */
    public boolean addToPlaylist(String playlistName, String trackId) {
        if (!this.tracksById.containsKey(trackId)) {
            return false;
        }
        PhiPlaylist playlist = this.playlists.get(playlistName);
        if (playlist == null) {
            return false;
        }
        if (playlist.addTrack(trackId)) {
            this.revision++;
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
        PhiPlaylist playlist = this.playlists.get(playlistName);
        if (playlist == null) {
            return false;
        }
        if (playlist.removeTrack(trackId)) {
            this.revision++;
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
        PhiPlaylist playlist = this.playlists.get(playlistName);
        if (playlist == null) {
            return false;
        }
        if (playlist.moveTrack(trackId, up)) {
            this.revision++;
            return true;
        }
        return false;
    }

    /**
     * Physically deletes a track file from the music folder (irreversible) and cleans every
     * reference to it (track table, favorites, named playlists). The caller must release the
     * decoder handle first when the track is the one currently playing, so the file lock does not
     * block the deletion.
     *
     * @param trackId the track identifier.
     * @return true when the file is gone afterwards (deleted here or already missing).
     */
    public boolean deleteTrack(String trackId) {
        PhiTrack track = this.tracksById.get(trackId);
        File file = track != null ? track.getFile() : resolveTrackFile(trackId);
        if (file == null) {
            return false;
        }
        // Only delete inside the music folder - never trust an arbitrary id.
        try {
            File canonicalFolder = this.musicFolder.getCanonicalFile();
            File canonicalFile = file.getCanonicalFile();
            if (!canonicalFile.getPath().startsWith(canonicalFolder.getPath() + File.separator)) {
                PhiLogger.warn("PhiMusicLibrary - refusing to delete a file outside the music folder");
                return false;
            }
        } catch (IOException e) {
            PhiLogger.warn("PhiMusicLibrary - cannot resolve the track path for deletion");
            return false;
        }

        if (file.exists() && !file.delete()) {
            PhiLogger.warn("PhiMusicLibrary - cannot delete the track file (in use?)");
            return false;
        }

        this.tracksById.remove(trackId);
        this.favorites.remove(trackId);
        for (PhiPlaylist playlist : this.playlists.values()) {
            playlist.removeTrack(trackId);
        }
        this.revision++;
        return true;
    }

    /**
     * Resolves the file of a track id that is not in the track table, allowing only a direct child
     * name of the music folder.
     *
     * @param trackId the track identifier.
     * @return the file, or {@code null} when the id is not a plain file name.
     */
    private File resolveTrackFile(String trackId) {
        if (trackId == null || trackId.isEmpty() || trackId.indexOf('/') >= 0 || trackId.indexOf('\\') >= 0) {
            return null;
        }
        return new File(this.musicFolder, trackId);
    }

    /**
     * Lists the audio files of the music folder (recursively).
     */
    private List<File> listAudioFiles() {
        List<File> files = new ArrayList<File>();
        File[] children = this.musicFolder.listFiles();
        if (children == null) {
            return files;
        }
        for (File child : children) {
            if (child.isFile() && !isCoverFile(child)) {
                files.add(child);
            } else if (child.isDirectory() && !child.getName().startsWith(".")) {
                collectAudioFiles(child, files);
            }
        }
        return files;
    }

    private void collectAudioFiles(File folder, List<File> files) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && !isCoverFile(child)) {
                files.add(child);
            } else if (child.isDirectory() && !child.getName().startsWith(".")) {
                collectAudioFiles(child, files);
            }
        }
    }

    private static boolean isCoverFile(File file) {
        String name = file.getName().toLowerCase();
        return name.equals("cover.jpg") || name.equals("cover.png");
    }

    /**
     * Queues an ffprobe run for a newly discovered track.
     */
    private void enqueueProbe(final PhiTrack track, final File file) {
        this.probeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                PhiTrackMetadata metadata;
                try {
                    metadata = PhiTrackMetadata.probe(file);
                } catch (Throwable t) {
                    metadata = null;
                }
                if (metadata == null) {
                    return;
                }
                PhiMusicLibrary.this.pendingMetadata.add(new MetadataJob(track.getId(), metadata));
            }
        });
    }

    /**
     * Computes the stable identifier of a file: its path relative to the music folder.
     *
     * @param folder the music folder.
     * @param file   a file inside it.
     * @return the relative path with {@code /} separators.
     */
    public static String toId(File folder, File file) {
        try {
            URI folderUri = folder.toURI();
            URI fileUri = file.toURI();
            String relative = folderUri.relativize(fileUri).getPath();
            if (relative != null && !relative.isEmpty()) {
                return relative;
            }
        } catch (Throwable ignored) {
            // fall through to the plain name
        }
        return file.getName();
    }

    /**
     * A pending metadata result (probed on the background thread, applied on the main thread).
     */
    private static final class MetadataJob {
        private final String trackId;
        private final PhiTrackMetadata metadata;

        private MetadataJob(String trackId, PhiTrackMetadata metadata) {
            this.trackId = trackId;
            this.metadata = metadata;
        }
    }

    /**
     * A tiny thread factory that names the spawned threads.
     */
    private static final class NamedThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String name;

        private NamedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, this.name);
            thread.setDaemon(true);
            return thread;
        }
    }
}
