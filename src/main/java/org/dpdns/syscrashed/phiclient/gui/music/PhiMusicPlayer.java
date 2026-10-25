package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.Minecraft;

/**
 * The music player of the home page (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The player plays the audio files of the folder {@code PhiClient/music} inside the game directory.
 * The playlist is built from the files of that folder, so the title and the artist of the current track
 * are real - there is no demo data behind the interface. If the folder is empty, the interface says so
 * instead of pretending that something is playing.</p>
 *
 * <p>The playback uses the audio system of the Java runtime, so every format the runtime can decode
 * works (WAV, AU and AIFF; the MP3 codec is not part of a standard Java 8 installation).</p>
 */
public class PhiMusicPlayer {

    /** The folder with the music files, relative to the game directory. */
    private static final String FOLDER = "PhiClient" + File.separator + "music";

    /** How often the music folder is checked for new files (in milliseconds). */
    private static final long RELOAD_INTERVAL = 4000L;

    private final File musicFolder;

    private final List<File> tracks = new ArrayList<File>();

    private int index = -1;

    private Clip clip;

    private boolean playing;

    private long lastReload;

    /**
     * Creates the player and reads the music folder.
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
     * Rebuilds the playlist from the music folder. The folder is only read again when something changed
     * in it, so the call is cheap enough for every frame.
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
     * @return true if the runtime of the game can decode the file.
     */
    private static boolean isSupported(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');

        if (dot < 0) {
            return false;
        }

        String extension = name.substring(dot + 1).toLowerCase();

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
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
     * @return the title of the current track.
     */
    public String getTitle() {
        File track = getCurrentTrack();

        if (track == null) {
            return "No music found";
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

        return "Local music";
    }

    /**
     * @return true if a track is loaded and the playback is running.
     */
    public boolean isPlaying() {
        return this.playing && this.clip != null;
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

        if (this.clip != null) {
            // The track is already loaded - just continue where it stopped.
            this.clip.start();
            this.playing = true;
            return;
        }

        loadCurrentTrack();
    }

    /**
     * Pauses the playback. The position of the track is kept.
     */
    public void pause() {
        if (this.clip != null) {
            this.clip.stop();
        }

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
     * Plays the next track of the playlist.
     */
    public void next() {
        if (this.tracks.isEmpty()) {
            return;
        }

        this.index = (this.index + 1) % this.tracks.size();
        this.playing = false;

        loadCurrentTrack();
    }

    /**
     * Plays the previous track of the playlist.
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
     * Closes the current track and opens the next one.
     */
    private void loadCurrentTrack() {
        closeClip();

        File track = getCurrentTrack();

        if (track == null) {
            return;
        }

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(track);
            Clip newClip = AudioSystem.getClip();
            newClip.open(stream);
            newClip.start();

            this.clip = newClip;
            this.playing = true;
        } catch (Throwable t) {
            PhiLogger.error("Cannot play the track " + track.getName(), t);
            this.playing = false;
        }
    }

    /**
     * Closes the clip of the current track.
     */
    private void closeClip() {
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
            this.clip = null;
        }

        this.playing = false;
    }

    /**
     * @return the progress of the current track in the range {@code [0, 1]}.
     */
    public float getProgress() {
        if (this.clip == null) {
            return 0.0F;
        }

        long length = this.clip.getMicrosecondLength();
        long position = this.clip.getMicrosecondPosition();

        if (length <= 0L) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, (float) position / (float) length));
    }

    /**
     * @return the elapsed time of the current track in seconds.
     */
    public float getPositionSeconds() {
        if (this.clip == null) {
            return 0.0F;
        }

        return this.clip.getMicrosecondPosition() / 1000000.0F;
    }

    /**
     * @return the length of the current track in seconds.
     */
    public float getDurationSeconds() {
        if (this.clip == null) {
            return 0.0F;
        }

        return this.clip.getMicrosecondLength() / 1000000.0F;
    }

    /**
     * Checks whether the current track has ended and starts the next one.
     */
    public void update() {
        reload();

        if (this.clip == null || !this.playing) {
            return;
        }

        if (this.clip.getMicrosecondPosition() >= this.clip.getMicrosecondLength()
                && this.clip.getMicrosecondLength() > 0L) {
            next();
        }
    }
}
