package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

/**
 * Reads tags and duration of an audio file through the external {@code ffprobe} binary
 * (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>The binary is looked up in {@code <game dir>/PhiClient/bin/ffprobe(.exe)} first and on the
 * system PATH second. The probe output uses the flat {@code KEY=value} format, so no JSON library
 * is needed. When the binary is missing or the probe fails, {@link #probe(File)} returns
 * {@code null} and the caller falls back to the file name.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #probe(File)} - reads title, artist and duration of a file.</li>
 * </ul>
 */
public final class PhiTrackMetadata {

    /** The display title of the track (never {@code null}, falls back to the file name). */
    public final String title;

    /** The artist of the track, or {@code null} when the file carries no artist tag. */
    public final String artist;

    /** The duration in seconds, or {@code 0} when unknown. */
    public final double durationSeconds;

    /** The folder the ffprobe executable is searched in, relative to the game directory. */
    private static final String BIN_FOLDER = "PhiClient" + File.separator + "bin";

    /** The upper bound of the ffprobe stdout accepted by {@link #probe(File)}. */
    private static final int PROBE_LIMIT_BYTES = 262144;

    private static final String PROBE_COMMAND = "ffprobe";

    /**
     * Creates an immutable metadata snapshot.
     *
     * @param title           the display title.
     * @param artist          the artist, may be {@code null}.
     * @param durationSeconds the duration in seconds, {@code 0} when unknown.
     */
    private PhiTrackMetadata(String title, String artist, double durationSeconds) {
        this.title = title;
        this.artist = artist;
        this.durationSeconds = durationSeconds;
    }

    /**
     * Probes a file with ffprobe and reads the title, artist and duration tags.
     *
     * @param file the audio file to probe.
     * @return the metadata, or {@code null} when ffprobe is unavailable or the probe fails.
     */
    public static PhiTrackMetadata probe(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        File binary = resolveBinary();
        List<String> command = new ArrayList<String>();
        command.add(binary != null ? binary.getAbsolutePath() : PROBE_COMMAND);
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration:format_tags");
        command.add("-of");
        command.add("default=noprint_wrappers=1");
        command.add(file.getAbsolutePath());

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(resolveWorkingDirectory());
            process = builder.start();
            byte[] output = readAll(process.getInputStream(), PROBE_LIMIT_BYTES);
            int exit = waitQuietly(process);
            if (exit != 0 || output.length == 0) {
                return null;
            }
            return parse(new String(output, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Resolves the ffprobe executable inside the game directory. Returns {@code null} when no
     * bundled binary exists - the caller then falls back to the PATH name.
     *
     * @return the ffprobe file, or {@code null}.
     */
    public static File resolveBinary() {
        try {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            if (gameDir == null) {
                return null;
            }
            File folder = new File(gameDir, BIN_FOLDER);
            File windows = new File(folder, PROBE_COMMAND + ".exe");
            if (windows.isFile()) {
                return windows;
            }
            File plain = new File(folder, PROBE_COMMAND);
            return plain.isFile() ? plain : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * @return the working directory used for the spawned process (the game directory or the
     *         current directory as a best effort).
     */
    private static File resolveWorkingDirectory() {
        try {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            return gameDir != null ? gameDir : new File(".").getAbsoluteFile();
        } catch (Throwable t) {
            return new File(".").getAbsoluteFile();
        }
    }

    /**
     * Parses the flat ffprobe output of the shape {@code duration=213.4} / {@code TAG:title=...}.
     *
     * @param output the raw stdout of ffprobe.
     * @return the parsed metadata, or {@code null} when the output carries no usable value.
     */
    private static PhiTrackMetadata parse(String output) {
        String title = null;
        String artist = null;
        double duration = 0.0D;

        for (String line : output.split("\\r?\\n")) {
            int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator);
            String value = line.substring(separator + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("duration".equals(key)) {
                try {
                    duration = Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    // keep 0
                }
            } else if ("TAG:title".equals(key) && title == null) {
                title = value;
            } else if ("TAG:artist".equals(key) && artist == null) {
                artist = value;
            }
        }

        if (title == null && duration <= 0.0D) {
            return null;
        }
        return new PhiTrackMetadata(title, artist, duration);
    }

    /**
     * Reads a stream fully up to a byte limit.
     *
     * @param stream the stream to read.
     * @param limit  the maximum number of bytes to accept.
     * @return the read bytes.
     */
    private static byte[] readAll(InputStream stream, int limit) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int total = 0;
        int read;
        while (total < limit && (read = stream.read(chunk)) >= 0) {
            int remaining = limit - total;
            buffer.write(chunk, 0, Math.min(read, remaining));
            total += Math.min(read, remaining);
        }
        return buffer.toByteArray();
    }

    /**
     * Waits for a process to exit without propagating an interrupt.
     *
     * @param process the process to wait for.
     * @return the exit code, or {@code -1} when the wait fails.
     */
    private static int waitQuietly(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
