package org.dpdns.syscrashed.phiclient.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.Minecraft;

/**
 * Persists the selected language in {@code <game dir>/PhiClient/config.properties}
 * (package {@code org.dpdns.syscrashed.phiclient.i18n}).
 *
 * <p>The config file is a plain {@link Properties} file written and read with an explicit UTF-8
 * encoding, following the same {@code PhiClient/} game-directory convention as
 * {@code PhiMusicPlayer}. A missing, empty or corrupt file simply falls back to
 * {@link PhiLanguage#EN_US}; failures never throw, so startup can never be blocked by the config.</p>
 */
public final class PhiLanguageConfig {

    /** The config file, relative to the game directory (same convention as the music folder). */
    private static final String FILE = "PhiClient" + File.separator + "config.properties";

    /** The property key holding the language id. */
    private static final String LANGUAGE_KEY = "language";

    private PhiLanguageConfig() {
    }

    /**
     * Reads the persisted language from the config file.
     *
     * @return the stored language, or {@link PhiLanguage#EN_US} when the file is missing, unreadable
     *         or contains an unknown id.
     */
    public static PhiLanguage load() {
        File file = resolveFile();
        if (file == null || !file.isFile()) {
            return PhiLanguage.EN_US;
        }
        Reader reader = null;
        try {
            Properties properties = new Properties();
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            properties.load(reader);
            String id = properties.getProperty(LANGUAGE_KEY);
            return PhiLanguage.fromId(id);
        } catch (Exception e) {
            PhiLogger.warn("PhiI18n - failed to read language config, falling back to en_US");
            return PhiLanguage.EN_US;
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Writes the selected language to the config file, creating the parent directory if needed.
     * Failures are logged and ignored - the in-memory language is always the source of truth for
     * the running session.
     *
     * @param language the language to persist.
     */
    public static void save(PhiLanguage language) {
        File file = resolveFile();
        if (file == null) {
            return;
        }
        Writer writer = null;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                PhiLogger.warn("PhiI18n - could not create config directory: " + parent);
                return;
            }
            Properties properties = new Properties();
            properties.setProperty(LANGUAGE_KEY, language.getId());
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            properties.store(writer, "PhiClient language selection");
        } catch (IOException e) {
            PhiLogger.warn("PhiI18n - failed to save language config");
        } finally {
            closeQuietly(writer);
        }
    }

    /**
     * Resolves the config file inside the game directory. When the game directory is unavailable
     * (e.g. very early startup) the current working directory is used as a best effort.
     *
     * @return the config file, or {@code null} if it cannot be resolved.
     */
    private static File resolveFile() {
        try {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            if (gameDir == null) {
                gameDir = new File(".").getAbsoluteFile();
            }
            return new File(gameDir, FILE);
        } catch (Throwable t) {
            PhiLogger.warn("PhiI18n - game directory unavailable, using working directory");
            return new File(FILE);
        }
    }

    /**
     * Closes a reader or writer and swallows any failure.
     *
     * @param closeable the resource to close, may be {@code null}.
     */
    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
