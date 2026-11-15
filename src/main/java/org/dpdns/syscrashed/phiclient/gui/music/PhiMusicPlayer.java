package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.Minecraft;

/**
 * The music playback engine of the client (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The engine scans the folder {@code PhiClient/music} inside the game directory and plays the
 * found files through the {@link PhiAudioDecoder}, so every format FFmpeg understands plays and the
 * native formats (WAV, AU, AIFF) keep working without FFmpeg. The playlist is the list of the files
 * of that folder sorted by name.</p>
 *
 * <p>The engine supports three repeat modes ({@link PlayMode}), seeking, the volume control and the
 * manual transport (play, pause, next, previous). All state is read and mutated on the client tick
 * thread - the decode thread of the decoder only maintains the stream itself. Instances of this class
 * are plain engines: the singleton that drives one shared engine is {@link PhiMusicManager}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #play()} / {@link #pause()} / {@link #toggle()} / {@link #next()} / {@link #previous()} / {@link #playIndex(int)} - transport.</li>
 *   <li>{@link #seek(float)} / {@link #setVolume(float)} / {@link #setPlayMode(PlayMode)} - playback settings.</li>
 *   <li>{@link #update()} - per-tick housekeeping (folder rescan, auto-advance).</li>
 *   <li>{@link #stopAndRelease()} - releases the decoder and the audio line.</li>
 *   <li>{@link #getProgress()} / {@link #getPositionSeconds()} / {@link #getDurationSeconds()} - snapshots.</li>
 * </ul>
 */
public class PhiMusicPlayer {

    /** The folder with the music files, relative to the game directory. */
    private static final String FOLDER = "PhiClient" + File.separator + "music";

    /** How often the music folder is checked for new files (in milliseconds). */
    private static final long RELOAD_INTERVAL = 4000L;

    private final File musicFolder;

    private final List<File> tracks = new ArrayList<File>();

    private final PhiAudioDecoder decoder = new PhiAudioDecoder();

    private final Random random = new Random();

    private int index = -1;

    private boolean playing;

    private long lastReload;

    private PlayMode playMode = PlayMode.SEQUENTIAL_LOOP;

    private float volume = 1.0F;

    /**
     * Creates the engine and reads the music folder.
     */
    public PhiMusicPlayer() {
        this.musicFolder = new File(Minecraft.getMinecraft().mcDataDir, FOLDER);
        this.lastReload = 0L;

        reload();
    }

    /**
     * @return the folder the music files are read from.
     */
    public File getMusicFolder() {
        return this.musicFolder;
    }

    /**
     * @return the files of the playlist.
     */
    public List<File> getTracks() {
        return this.tracks;
    }

    /**
     * @return the number of tracks of the playlist.
     */
    public int getTrackCount() {
        return this.tracks.size();
    }

    /**
     * Rebuilds the playlist from the music folder. The folder is only read again when something
     * changed in it, so the call is cheap enough for every frame.
     */
    public void reload() {
        long now = System.currentTimeMillis();

        if (now - this.lastReload < RELOAD_INTERVAL) {
            return;
        }

        this.lastReload = now;

        List<File> found = new ArrayList<File>();
        File[] files = this.musicFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isSupported(file)) {
                    found.add(file);
                }
            }
        }

        Collections.sort(found, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
        });

        this.tracks.clear();
        this.tracks.addAll(found);

        if (this.index >= this.tracks.size()) {
            this.index = this.tracks.size() - 1;
        }
    }

    /**
     * @param file an audio file.
     * @return true if the file can be decoded: either a native Java runtime format or any file when
     *         FFmpeg is available.
     */
    private boolean isSupported(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');

        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }

        String extension = name.substring(dot + 1).toLowerCase();

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return this.decoder.isFfmpegAvailable();
    }

    /**
     * @return the file of the current track (null when the playlist is empty).
     */
    public File getCurrentTrack() {
        if (this.index < 0 || this.index >= this.tracks.size()) {
            return null;
        }

        return this.tracks.get(this.index);
    }

    /**
     * @return the index of the current track, or {@code -1} when nothing is selected.
     */
    public int getCurrentIndex() {
        return this.index;
    }

    /**
     * @return the title of the current track.
     */
    public String getTitle() {
        File track = getCurrentTrack();

        if (track == null) {
            return PhiI18n.get("music.not_found");
        }

        String name = track.getName();
        int dot = name.lastIndexOf('.');

        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * @return the artist of the current track.
     */
    public String getArtist() {
        File track = getCurrentTrack();

        if (track == null) {
            // No track: the artist field shows where the music files have to be put.
            File parent = this.musicFolder.getParentFile();

            if (parent == null) {
                return this.musicFolder.getName();
            }

            return parent.getName() + "/" + this.musicFolder.getName();
        }

        return PhiI18n.get("music.local");
    }

    /**
     * @return true if a track is loaded and the playback is running.
     */
    public boolean isPlaying() {
        return this.playing && this.decoder.isOpen();
    }

    /**
     * Starts the playback of the current track (or of the first one).
     */
    public void play() {
        reload();

        if (this.tracks.isEmpty()) {
            return;
        }

        if (this.index < 0) {
            this.index = 0;
        }

        if (this.decoder.isOpen()) {
            // The track is already loaded - just continue where it stopped.
            this.decoder.resume();
            this.playing = true;
            return;
        }

        loadCurrentTrack();
    }

    /**
     * Pauses the playback. The position of the track is kept.
     */
    public void pause() {
        this.decoder.pause();
        this.playing = false;
    }

    /**
     * Starts or pauses the playback.
     */
    public void toggle() {
        if (this.playing) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Plays the next track according to the current {@link PlayMode}.
     */
    public void next() {
        if (this.tracks.isEmpty()) {
            return;
        }

        int size = this.tracks.size();

        switch (this.playMode) {
            case SINGLE_LOOP:
                // Replays the current track from the start.
                if (this.index < 0) {
                    this.index = 0;
                }
                break;
            case PSEUDO_RANDOM:
                if (size == 1) {
                    this.index = 0;
                } else {
                    int picked;
                    do {
                        picked = this.random.nextInt(size);
                    } while (picked == this.index);
                    this.index = picked;
                }
                break;
            case SEQUENTIAL_LOOP:
            default:
                this.index = (this.index + 1) % size;
                break;
        }

        this.playing = false;
        loadCurrentTrack();
    }

    /**
     * Plays the previous track of the playlist. All modes step back in playlist order.
     */
    public void previous() {
        if (this.tracks.isEmpty()) {
            return;
        }

        this.index = (this.index - 1 + this.tracks.size()) % this.tracks.size();
        this.playing = false;

        loadCurrentTrack();
    }

    /**
     * Plays the track at the given index of the playlist.
     *
     * @param trackIndex the 0-based index (clamped to the playlist bounds).
     */
    public void playIndex(int trackIndex) {
        if (this.tracks.isEmpty() || trackIndex < 0 || trackIndex >= this.tracks.size()) {
            return;
        }

        this.index = trackIndex;
        this.playing = false;

        loadCurrentTrack();
    }

    /**
     * Closes the current track and opens the next one.
     */
    private void loadCurrentTrack() {
        File track = getCurrentTrack();

        if (track == null) {
            return;
        }

        if (this.decoder.open(track, 0L)) {
            this.decoder.setVolume(this.volume);
            this.playing = true;
        } else {
            PhiLogger.error("PhiMusicPlayer - cannot play the track " + track.getName());
            this.playing = false;
        }
    }

    /**
     * @return the progress of the current track in the range {@code [0, 1]}.
     */
    public float getProgress() {
        long total = this.decoder.getTotalFrames();
        long position = this.decoder.getPositionFrames();

        if (total <= 0L) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, (float) position / (float) total));
    }

    /**
     * @return the elapsed time of the current track in seconds.
     */
    public float getPositionSeconds() {
        return this.decoder.getPositionFrames() / (float) PhiAudioDecoder.SAMPLE_RATE;
    }

    /**
     * @return the length of the current track in seconds.
     */
    public float getDurationSeconds() {
        return this.decoder.getTotalFrames() / (float) PhiAudioDecoder.SAMPLE_RATE;
    }

    /**
     * Seeks to the given ratio of the current track.
     *
     * @param ratio the target position in {@code [0, 1]} (clamped).
     */
    public void seek(float ratio) {
        long total = this.decoder.getTotalFrames();

        if (total <= 0L) {
            return;
        }

        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        boolean wasPlaying = this.playing;

        this.decoder.seek(Math.round(clamped * total));

        if (!wasPlaying) {
            // The decoder restarts its stream on seek - keep the paused state.
            this.decoder.pause();
        }
    }

    /**
     * Sets the playback volume.
     *
     * @param linear the volume as a linear gain in {@code [0, 1]}.
     */
    public void setVolume(float linear) {
        this.volume = Math.max(0.0F, Math.min(1.0F, linear));
        this.decoder.setVolume(this.volume);
    }

    /**
     * @return the playback volume in percent {@code [0, 100]}.
     */
    public int getVolumePercent() {
        return Math.round(this.volume * 100.0F);
    }

    /**
     * @return the current repeat mode.
     */
    public PlayMode getPlayMode() {
        return this.playMode;
    }

    /**
     * Sets the repeat mode. The current playback position is not touched.
     *
     * @param mode the new repeat mode.
     */
    public void setPlayMode(PlayMode mode) {
        this.playMode = mode != null ? mode : PlayMode.SEQUENTIAL_LOOP;
    }

    /**
     * Stops the playback and releases the decoder (the track index is kept).
     */
    public void stopAndRelease() {
        this.playing = false;
        this.decoder.close();
    }

    /**
     * Checks whether the current track has ended and starts the next one. Called once per client
     * tick (also from the draw loop of the home page for backwards compatibility).
     */
    public void update() {
        reload();

        if (!this.playing || !this.decoder.isOpen()) {
            return;
        }

        if (this.decoder.hasEnded()) {
            next();
        }
    }
}
