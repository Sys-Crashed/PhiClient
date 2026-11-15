package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.Minecraft;

/**
 * Decodes audio files to PCM with an external FFmpeg process and plays the PCM through a
 * {@link SourceDataLine} (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>FFmpeg is the single decoding backend: {@code ffmpeg -i <file> -f s16le -acodec pcm_s16le
 * -ar 44100 -ac 2 -} streams 44.1 kHz 16-bit stereo PCM to stdout and a dedicated decode thread
 * writes it into the line. The Java side never parses an audio format, so every format FFmpeg
 * understands (mp3, flac, ogg, opus, m4a, aac, wma, wav, ...) plays. The binary is searched in
 * {@code <game dir>/PhiClient/bin/ffmpeg(.exe)} and on the system PATH.</p>
 *
 * <p>When FFmpeg is missing or cannot start, the decoder falls back to the {@code Clip} playback
 * of the Java runtime, which only understands the native formats (WAV, AU, AIFF). Files of any
 * other format cannot be opened in that fallback mode.</p>
 *
 * <p>All blocking IO runs on the decode thread; the caller thread only reads the volatile state
 * snapshots. Pausing stops the line, which blocks the decode thread through back-pressure and
 * suspends the ffmpeg process for free; seeking destroys the process and restarts it with an
 * input-side {@code -ss} offset.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #open(File, long)} - starts decoding a file at the given frame.</li>
 *   <li>{@link #seek(long)} - restarts the decode stream at the given frame.</li>
 *   <li>{@link #pause()} / {@link #resume()} / {@link #close()} - playback state transitions.</li>
 *   <li>{@link #getPositionFrames()} / {@link #getTotalFrames()} / {@link #hasEnded()} - snapshots.</li>
 * </ul>
 */
public final class PhiAudioDecoder {

    /** The sample rate of the PCM written to the line. */
    public static final int SAMPLE_RATE = 44100;

    /** The number of channels of the PCM written to the line (stereo). */
    public static final int CHANNELS = 2;

    /** The size of one PCM frame in bytes (16-bit stereo little-endian). */
    public static final int BYTES_PER_FRAME = 4;

    /** The size of the internal buffer of the {@link SourceDataLine}. */
    private static final int LINE_BUFFER_BYTES = 65536;

    /** The size of the chunks read from the ffmpeg stdout pipe. */
    private static final int READ_CHUNK_BYTES = 16384;

    /** How long {@link #close()} waits for the process and the decode thread to die. */
    private static final int SHUTDOWN_TIMEOUT_MS = 1000;

    private static final String FFMPEG_COMMAND = "ffmpeg";

    private static final String BIN_FOLDER = "PhiClient" + File.separator + "bin";

    /** Cached result of the ffmpeg availability check; {@code null} until first probed. */
    private static volatile Boolean ffmpegWorks;

    /** The resolved ffmpeg executable, or {@code null} when only the PATH name can be used. */
    private final File ffmpegPath;

    /** The file currently opened, or {@code null}. */
    private File trackFile;

    /** true while the fallback Clip playback is active. */
    private boolean clipMode;

    private Clip clip;

    private SourceDataLine line;

    private Process process;

    private Thread decodeThread;

    /** true while the decode thread should give up writing to the line. */
    private volatile boolean closed;

    /** The frames already written into the line (decode stream position). */
    private volatile long positionFrames;

    private long totalFrames;

    private volatile boolean ended;

    /** The playback volume as a linear gain in {@code [0, 1]} (1 = unchanged). */
    private float volume = 1.0F;

    /**
     * Creates a decoder and resolves the bundled ffmpeg executable, if present.
     */
    public PhiAudioDecoder() {
        this.ffmpegPath = resolveBinary();
    }

    /**
     * Opens a file and starts decoding it.
     *
     * @param file       the audio file to play.
     * @param startFrame the frame to start at (0-based); only respected by the ffmpeg path.
     * @return true when the file is now playing.
     */
    public boolean open(File file, long startFrame) {
        close();
        if (file == null || !file.isFile()) {
            return false;
        }
        this.trackFile = file;
        this.positionFrames = Math.max(0L, startFrame);
        this.ended = false;
        this.closed = false;

        if (isNativeFormat(file)) {
            return openWithClip(file, startFrame);
        }
        if (!isFfmpegAvailable()) {
            PhiLogger.error("PhiAudioDecoder - cannot decode " + file.getName()
                    + ": format not supported by the Java runtime and no ffmpeg available");
            return false;
        }
        return openWithFfmpeg(file, startFrame);
    }

    /**
     * Restarts the decode stream of the current file at the given frame.
     *
     * @param targetFrame the frame to jump to (clamped to the track length when known).
     */
    public void seek(long targetFrame) {
        if (this.trackFile == null) {
            return;
        }
        long target = Math.max(0L, targetFrame);
        if (this.totalFrames > 0L) {
            target = Math.min(target, this.totalFrames);
        }
        if (this.clipMode) {
            if (this.clip != null) {
                this.clip.setMicrosecondPosition(target * 1000000L / SAMPLE_RATE);
                this.positionFrames = this.clip.getMicrosecondPosition() * SAMPLE_RATE / 1000000L;
            }
            return;
        }
        // Restart the ffmpeg stream at the target position.
        open(this.trackFile, target);
    }

    /**
     * Pauses the playback. The position is kept and resume continues where it stopped.
     */
    public void pause() {
        if (this.clipMode) {
            if (this.clip != null) {
                this.clip.stop();
            }
        } else if (this.line != null) {
            this.line.stop();
        }
    }

    /**
     * Resumes the playback after {@link #pause()}.
     */
    public void resume() {
        if (this.clipMode) {
            if (this.clip != null) {
                this.clip.start();
            }
        } else if (this.line != null) {
            this.line.start();
        }
    }

    /**
     * Stops the playback and releases the process, the thread and the line.
     */
    public void close() {
        this.closed = true;
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
            this.clip = null;
        }
        if (this.line != null) {
            this.line.start();
        }
        if (this.process != null) {
            this.process.destroy();
            waitQuietly(this.process, SHUTDOWN_TIMEOUT_MS);
            this.process.destroyForcibly();
            this.process = null;
        }
        if (this.decodeThread != null) {
            joinQuietly(this.decodeThread, SHUTDOWN_TIMEOUT_MS);
            this.decodeThread = null;
        }
        if (this.line != null) {
            this.line.stop();
            this.line.flush();
            this.line.close();
            this.line = null;
        }
        this.trackFile = null;
        this.clipMode = false;
        this.positionFrames = 0L;
        this.totalFrames = 0L;
        this.ended = false;
    }

    /**
     * @return true when a file is open (either through ffmpeg or through the Clip fallback).
     */
    public boolean isOpen() {
        return this.clip != null || this.line != null;
    }

    /**
     * @return true when the native Clip fallback is active (FFmpeg not involved).
     */
    public boolean usesNativeClip() {
        return this.clipMode && this.clip != null;
    }

    /**
     * @return the position of the decode stream in frames.
     */
    public long getPositionFrames() {
        if (this.clipMode) {
            if (this.clip == null) {
                return 0L;
            }
            this.positionFrames = this.clip.getMicrosecondPosition() * SAMPLE_RATE / 1000000L;
        }
        return this.positionFrames;
    }

    /**
     * @return the total frames of the current file, or {@code 0} when unknown.
     */
    public long getTotalFrames() {
        return this.totalFrames;
    }

    /**
     * @return true when the decode stream has reached the end of the file.
     */
    public boolean hasEnded() {
        if (this.clipMode) {
            if (this.clip == null) {
                return false;
            }
            long length = this.clip.getMicrosecondLength();
            return length > 0L && this.clip.getMicrosecondPosition() >= length;
        }
        return this.ended;
    }

    /**
     * Opens a native-format file through the {@link Clip} fallback.
     */
    private boolean openWithClip(File file, long startFrame) {
        try {
            Clip newClip = AudioSystem.getClip();
            newClip.open(AudioSystem.getAudioInputStream(file));
            if (startFrame > 0L) {
                newClip.setMicrosecondPosition(startFrame * 1000000L / SAMPLE_RATE);
            }
            newClip.start();
            this.clip = newClip;
            this.clipMode = true;
            this.totalFrames = newClip.getMicrosecondLength() * SAMPLE_RATE / 1000000L;
            this.positionFrames = newClip.getMicrosecondPosition() * SAMPLE_RATE / 1000000L;
            applyVolume();
            return true;
        } catch (Throwable t) {
            PhiLogger.error("PhiAudioDecoder - cannot play " + file.getName(), t);
            this.clipMode = false;
            return false;
        }
    }

    /**
     * Starts the ffmpeg process, opens the line and starts the decode thread.
     */
    private boolean openWithFfmpeg(File file, long startFrame) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
        SourceDataLine newLine;
        try {
            newLine = AudioSystem.getSourceDataLine(format);
            newLine.open(format, LINE_BUFFER_BYTES);
        } catch (LineUnavailableException e) {
            PhiLogger.error("PhiAudioDecoder - audio line unavailable", e);
            return false;
        }

        List<String> command = buildCommand(file, startFrame);
        Process newProcess;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(resolveWorkingDirectory());
            newProcess = builder.start();
        } catch (IOException e) {
            newLine.close();
            PhiLogger.error("PhiAudioDecoder - cannot start ffmpeg for " + file.getName());
            return false;
        }

        this.line = newLine;
        this.process = newProcess;
        this.clipMode = false;
        this.ended = false;
        newLine.start();
        this.totalFrames = readTotalFrames(file);
        applyVolume();

        startDrainThread(newProcess);
        startDecodeThread(newProcess, newLine);
        return true;
    }

    /**
     * Sets the playback volume of the current stream.
     *
     * @param linear the volume as a linear gain in {@code [0, 1]} (1 = unchanged, 0 = muted).
     */
    public void setVolume(float linear) {
        this.volume = Math.max(0.0F, Math.min(1.0F, linear));
        applyVolume();
    }

    /**
     * Applies the stored volume to the active line (best effort - the master gain control may not
     * exist on every platform, in which case the volume stays at the system level).
     */
    private void applyVolume() {
        if (this.clip != null) {
            applyGain(this.clip, this.volume);
        } else if (this.line != null) {
            applyGain(this.line, this.volume);
        }
    }

    /**
     * Maps a linear gain to the master gain control of a line.
     *
     * @param line   the line to control.
     * @param linear the linear gain in {@code [0, 1]}.
     */
    private static void applyGain(Line line, float linear) {
        if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels;
        if (linear <= 0.0001F) {
            decibels = control.getMinimum();
        } else {
            decibels = 20.0F * (float) Math.log10(linear);
        }
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)));
    }

    /**
     * Builds the ffmpeg command line for streaming PCM.
     */
    private List<String> buildCommand(File file, long startFrame) {
        List<String> command = new ArrayList<String>();
        command.add(this.ffmpegPath != null ? this.ffmpegPath.getAbsolutePath() : FFMPEG_COMMAND);
        command.add("-hide_banner");
        command.add("-nostdin");
        command.add("-loglevel");
        command.add("error");
        if (startFrame > 0L) {
            command.add("-ss");
            command.add(String.format(Locale.US, "%.3f", startFrame / (double) SAMPLE_RATE));
        }
        command.add("-i");
        command.add(file.getAbsolutePath());
        command.add("-map");
        command.add("a:0");
        command.add("-f");
        command.add("s16le");
        command.add("-acodec");
        command.add("pcm_s16le");
        command.add("-ar");
        command.add(String.valueOf(SAMPLE_RATE));
        command.add("-ac");
        command.add(String.valueOf(CHANNELS));
        command.add("-");
        return command;
    }

    /**
     * Starts the thread that copies the ffmpeg stdout into the line.
     */
    private void startDecodeThread(final Process process, final SourceDataLine line) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream stream = process.getInputStream();
                byte[] buffer = new byte[READ_CHUNK_BYTES];
                try {
                    int read;
                    while (!PhiAudioDecoder.this.closed && (read = stream.read(buffer)) >= 0) {
                        int offset = 0;
                        while (offset < read && !PhiAudioDecoder.this.closed) {
                            int written = line.write(buffer, offset, read - offset);
                            offset += written;
                            PhiAudioDecoder.this.positionFrames += written / BYTES_PER_FRAME;
                        }
                    }
                } catch (IOException ignored) {
                    // stream closed by the shutdown - the caller already decided
                } finally {
                    PhiAudioDecoder.this.ended = true;
                    try {
                        process.destroy();
                    } catch (Throwable ignored) {
                        // already dead
                    }
                }
            }
        }, "PhiMusicDecode");
        thread.setDaemon(true);
        thread.start();
        this.decodeThread = thread;
    }

    /**
     * Starts the thread that silently drains the ffmpeg stderr so the process never blocks.
     */
    private void startDrainThread(final Process process) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream stream = process.getErrorStream();
                byte[] buffer = new byte[1024];
                try {
                    while (stream.read(buffer) >= 0) {
                        // discarded: -loglevel error keeps this silent in normal runs
                    }
                } catch (IOException ignored) {
                    // process died
                }
            }
        }, "PhiMusicStderr");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Reads the total frames of the current file from its duration (ffprobe).
     */
    private long readTotalFrames(File file) {
        PhiTrackMetadata metadata = PhiTrackMetadata.probe(file);
        if (metadata != null && metadata.durationSeconds > 0.0D) {
            return Math.round(metadata.durationSeconds * SAMPLE_RATE);
        }
        return 0L;
    }

    /**
     * @param file an audio file.
     * @return true when the Java runtime itself can decode the file (WAV, AU, AIFF).
     */
    private static boolean isNativeFormat(File file) {
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
     * Checks once whether an ffmpeg binary can actually run, and caches the result.
     *
     * @return true when ffmpeg is usable (and therefore every format it understands can play).
     */
    public boolean isFfmpegAvailable() {
        return isFfmpegAvailableStatic();
    }

    /**
     * Decides whether a file can be decoded at all: either through the native Java runtime formats
     * (WAV, AU, AIFF) or through FFmpeg (any format it understands).
     *
     * @param file the candidate file.
     * @return true when the file could be played by the decoder.
     */
    public static boolean isSupportedFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
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
        return isFfmpegAvailableStatic();
    }

    /**
     * Checks the cached ffmpeg availability.
     */
    private static boolean isFfmpegAvailableStatic() {
        Boolean cached = ffmpegWorks;
        if (cached != null) {
            return cached.booleanValue();
        }
        synchronized (PhiAudioDecoder.class) {
            if (ffmpegWorks == null) {
                ffmpegWorks = Boolean.valueOf(checkFfmpegVersionStatic());
            }
            return ffmpegWorks.booleanValue();
        }
    }

    /**
     * Runs {@code ffmpeg -version} to verify the binary.
     */
    private static boolean checkFfmpegVersionStatic() {
        File binary = resolveBinary();
        List<String> command = new ArrayList<String>();
        command.add(binary != null ? binary.getAbsolutePath() : FFMPEG_COMMAND);
        command.add("-version");
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();
            if (!waitQuietly(process, SHUTDOWN_TIMEOUT_MS)) {
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            PhiLogger.warn("PhiAudioDecoder - ffmpeg not found, playback falls back to native formats");
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Resolves the bundled ffmpeg executable inside the game directory.
     *
     * @return the ffmpeg file, or {@code null} when only the PATH name can be used.
     */
    private static File resolveBinary() {
        try {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            if (gameDir == null) {
                return null;
            }
            File folder = new File(gameDir, BIN_FOLDER);
            File windows = new File(folder, FFMPEG_COMMAND + ".exe");
            if (windows.isFile()) {
                return windows;
            }
            File plain = new File(folder, FFMPEG_COMMAND);
            return plain.isFile() ? plain : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * @return the working directory used for the spawned process.
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
     * Waits for a process to exit, destroying it when the timeout expires.
     *
     * @param process the process.
     * @param timeout the timeout in milliseconds.
     * @return true when the process exited in time.
     */
    private static boolean waitQuietly(Process process, long timeout) {
        try {
            long deadline = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() < deadline) {
                try {
                    process.exitValue();
                    return true;
                } catch (IllegalThreadStateException notYet) {
                    Thread.sleep(20L);
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Joins a thread, ignoring interrupts.
     *
     * @param thread  the thread.
     * @param timeout the timeout in milliseconds.
     */
    private static void joinQuietly(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
