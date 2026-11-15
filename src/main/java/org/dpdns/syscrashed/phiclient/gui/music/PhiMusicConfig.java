package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Persists the music subsystem settings and the user data (package
 * {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The config is a UTF-8 {@link Properties} file at {@code <game dir>/PhiClient/music.properties}
 * (the same pattern as {@code PhiLanguageConfig}). It stores the volume, the repeat mode, the library
 * view, the favorite identifiers, the named playlists with their track order and the last played
 * track. Track identifiers are {@code ;} separated; the playlist names never appear as property keys,
 * so arbitrary display names stay round-trip safe.</p>
 */
public final class PhiMusicConfig {

    /** The config file path relative to the game directory. */
    private static final String CONFIG_PATH = "PhiClient" + File.separator + "music.properties";

    /** Separator between track identifiers inside a property value. */
    private static final String SEPARATOR = ";";

    private final File configFile;

    /**
     * Creates the config access for the given game directory.
     *
     * @param gameDir the game directory.
     */
    public PhiMusicConfig(File gameDir) {
        this.configFile = new File(gameDir, CONFIG_PATH);
    }

    /**
     * A snapshot of the persisted state.
     */
    public static final class State {

        /** The volume in percent {@code [0, 100]}. */
        public int volume = 100;

        /** The repeat mode. */
        public PlayMode playMode = PlayMode.SEQUENTIAL_LOOP;

        /** The library view. */
        public LibraryView view = LibraryView.ALL;

        /** The selected playlist name (empty when the view is not a playlist). */
        public String playlistName = "";

        /** The favorite track identifiers. */
        public final Set<String> favorites = new LinkedHashSet<String>();

        /** The named playlists in stored order. */
        public final List<PhiPlaylist> playlists = new ArrayList<PhiPlaylist>();

        /** The identifier of the last played track (empty when none). */
        public String lastTrackId = "";
    }

    /**
     * Reads the persisted state. Missing or broken files yield the defaults.
     *
     * @return the persisted state (never null).
     */
    public State load() {
        State state = new State();
        if (!this.configFile.isFile()) {
            return state;
        }

        Properties properties = new Properties();
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(this.configFile), StandardCharsets.UTF_8);
            properties.load(reader);
        } catch (IOException e) {
            PhiLogger.warn("PhiMusicConfig - cannot read " + this.configFile.getName());
            return state;
        } finally {
            closeQuietly(reader);
        }

        state.volume = parseInt(properties.getProperty("music.volume"), state.volume);
        state.volume = Math.max(0, Math.min(100, state.volume));
        state.playMode = parseMode(properties.getProperty("music.playmode"), state.playMode);
        state.view = parseView(properties.getProperty("music.view"), state.view);
        state.playlistName = properties.getProperty("music.playlistname", "");
        state.lastTrackId = properties.getProperty("music.lasttrack", "");
        readFavorites(properties, state.favorites);
        readPlaylists(properties, state.playlists);
        return state;
    }

    /**
     * Writes the persisted state.
     *
     * @param volume       the volume in percent.
     * @param mode         the repeat mode.
     * @param view         the library view.
     * @param playlistName the selected playlist name.
     * @param favorites    the favorite track identifiers.
     * @param playlists    the named playlists.
     * @param lastTrackId  the identifier of the last played track.
     */
    public void save(int volume, PlayMode mode, LibraryView view, String playlistName,
                     Set<String> favorites, List<PhiPlaylist> playlists, String lastTrackId) {
        Properties properties = new Properties();
        properties.setProperty("music.volume", String.valueOf(Math.max(0, Math.min(100, volume))));
        properties.setProperty("music.playmode", mode != null ? mode.name() : PlayMode.SEQUENTIAL_LOOP.name());
        properties.setProperty("music.view", view != null ? view.name() : LibraryView.ALL.name());
        properties.setProperty("music.playlistname", playlistName != null ? playlistName : "");
        properties.setProperty("music.lasttrack", lastTrackId != null ? lastTrackId : "");
        properties.setProperty("music.favorites", join(favorites));
        properties.setProperty("music.playlist.count", String.valueOf(playlists.size()));
        for (int i = 0; i < playlists.size(); i++) {
            PhiPlaylist playlist = playlists.get(i);
            properties.setProperty("music.playlist." + i + ".name", playlist.getName());
            properties.setProperty("music.playlist." + i + ".tracks", join(playlist.getTracks()));
        }

        File parent = this.configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(this.configFile), StandardCharsets.UTF_8);
            properties.store(writer, null);
        } catch (IOException e) {
            PhiLogger.error("PhiMusicConfig - cannot write " + this.configFile.getName());
        } finally {
            closeQuietly(writer);
        }
    }

    /**
     * Reads the favorite identifiers.
     */
    private void readFavorites(Properties properties, Set<String> target) {
        String value = properties.getProperty("music.favorites", "");
        for (String id : value.split("\\Q" + SEPARATOR + "\\E")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }

    /**
     * Reads the playlists by index.
     */
    private void readPlaylists(Properties properties, List<PhiPlaylist> target) {
        int count = parseInt(properties.getProperty("music.playlist.count"), 0);
        for (int i = 0; i < count; i++) {
            String name = properties.getProperty("music.playlist." + i + ".name", "");
            if (name.isEmpty()) {
                continue;
            }
            PhiPlaylist playlist = new PhiPlaylist(name);
            String value = properties.getProperty("music.playlist." + i + ".tracks", "");
            for (String id : value.split("\\Q" + SEPARATOR + "\\E")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    playlist.addTrack(trimmed);
                }
            }
            target.add(playlist);
        }
    }

    /**
     * Joins identifiers with the separator.
     */
    private static String join(Iterable<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static PlayMode parseMode(String value, PlayMode fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return PlayMode.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static LibraryView parseView(String value, LibraryView fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return LibraryView.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static void closeQuietly(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private static void closeQuietly(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }
}
