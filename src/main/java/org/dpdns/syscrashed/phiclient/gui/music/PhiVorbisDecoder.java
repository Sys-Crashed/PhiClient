package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * Decodes an OGG Vorbis file to PCM with the pure-Java JOrbis library that ships with
 * Minecraft 1.8.9 (package {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>This is the offline fallback of {@link PhiAudioDecoder}: when no FFmpeg binary exists, the
 * Java runtime itself cannot decode OGG, so this class parses the container, synthesizes the
 * Vorbis packets and converts the samples into the fixed output format of the audio pipeline
 * (44.1 kHz, 16-bit, stereo, little-endian, interleaved). The output format is identical to the
 * PCM that FFmpeg would produce, so the rest of the player is agnostic to the backend.</p>
 *
 * <p>Source formats with a different rate or channel count are converted on the fly: mono is
 * duplicated to both channels, more than two channels are down-mixed to the first two, and
 * non-44.1 kHz material is resampled by linear interpolation. The decoder assumes a single
 * logical bitstream (chained OGG files stop after the first stream).</p>
 *
 * <p>Usage: {@link #open(File)} parses the headers and scans the file once for its exact length,
 * then {@link #skipFrames(long)} (optional) and {@link #read(byte[], int, int)} decode the audio.
 * {@link #probe(File)} is the static variant that only reads the tags and the duration and is used
 * by the metadata layer. A decoder instance is not thread-safe and must be driven by one thread.</p>
 */
public final class PhiVorbisDecoder {

    /** The output sample rate; matches {@link PhiAudioDecoder#SAMPLE_RATE}. */
    public static final int OUTPUT_RATE = 44100;

    /** One output frame: 16-bit stereo little-endian. */
    public static final int BYTES_PER_FRAME = 4;

    /** How many bytes are buffered per OGG read. */
    private static final int SYNC_BUFFER_BYTES = 4096;

    /** The result of {@link #probe(File)}: the tags and the duration of a Vorbis file. */
    public static final class ProbeResult {

        /** The TITLE tag, or {@code null}. */
        public final String title;

        /** The ARTIST tag, or {@code null}. */
        public final String artist;

        /** The duration in seconds computed from the last granule position (0 when unknown). */
        public final double durationSeconds;

        private ProbeResult(String title, String artist, double durationSeconds) {
            this.title = title;
            this.artist = artist;
            this.durationSeconds = durationSeconds;
        }
    }

    private InputStream in;

    private final SyncState sync = new SyncState();

    /**
     * Created fresh in every {@link #readHeaders()} call: the old JOrbis release shipped with
     * Minecraft has a {@code StreamState.clear()} that does not reset the fill counters, so a
     * reused instance keeps stale state and crashes in {@code pagein}.
     */
    private StreamState stream;

    private final Page page = new Page();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final Comment comment = new Comment();
    private DspState dsp;
    private Block block;

    /** The source sample rate from the identification header. */
    private int rate;

    /** The source channel count from the identification header. */
    private int channels;

    /** The decoded samples of the current packet (channel-major). */
    private float[][] pcm;

    /** How many samples of {@link #pcm} are valid. */
    private int pcmSamples;

    /** The source frame index of the first sample of the current packet. */
    private long srcBase;

    /** The last sample of the previous packet per channel (kept for interpolation continuity). */
    private float prevL;
    private float prevR;

    /** The source position (in source frames, fractional) of the next output frame. */
    private double srcPos;

    /** The total output frames of the stream, or 0 while unknown. */
    private long totalFrames;

    private boolean eof;
    private boolean closed;

    /**
     * Creates an empty decoder; call {@link #open(File)} before reading.
     */
    public PhiVorbisDecoder() {
        this.sync.init();
    }

    /**
     * Opens the file, parses the Vorbis headers, scans the file for its exact length and rewinds
     * to the first audio sample. Blocks while the file is scanned (I/O bound, not decoding).
     *
     * @param file the OGG Vorbis file to decode.
     * @throws IOException when the file cannot be read or is not a valid Vorbis stream.
     */
    public void open(File file) throws IOException {
        close();
        this.in = new FileInputStream(file);
        this.closed = false;
        this.eof = false;
        readHeaders();

        long lastGranule = scanGranule();
        if (lastGranule > 0L) {
            this.totalFrames = Math.round(lastGranule * OUTPUT_RATE / (double) this.rate);
        } else {
            this.totalFrames = 0L;
        }

        // Rewind: decoding starts at the first audio sample with fresh synthesis state.
        this.in.close();
        this.in = new FileInputStream(file);
        readHeaders();
    }

    /**
     * Reads Vorbis comments of the currently opened file. Only valid while the file is open.
     *
     * @param key the tag name (case-insensitive, e.g. {@code "TITLE"}).
     * @return the tag value without the trailing null byte, or {@code null}.
     */
    public String getTag(String key) {
        if (this.comment.user_comments == null) {
            return null;
        }
        String upper = key.toUpperCase();
        for (byte[] entry : this.comment.user_comments) {
            if (entry == null) {
                continue;
            }
            int separator = indexOf(entry, (byte) '=');
            if (separator < 0) {
                continue;
            }
            String tagKey = new String(entry, 0, separator, StandardCharsets.UTF_8);
            if (upper.equals(tagKey.toUpperCase())) {
                int end = entry.length;
                while (end > separator + 1 && entry[end - 1] == 0) {
                    end--;
                }
                return new String(entry, separator + 1, end - separator - 1, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * @return the total output frames, or 0 when unknown (e.g. the scan found no granule position).
     */
    public long getTotalFrames() {
        return this.totalFrames;
    }

    /**
     * @return the source sample rate of the stream.
     */
    public int getRate() {
        return this.rate;
    }

    /**
     * @return the source channel count of the stream.
     */
    public int getChannels() {
        return this.channels;
    }

    /**
     * Decodes and discards audio until the given output frame is reached (or the stream ends).
     *
     * @param frames the output frame to skip to.
     * @throws IOException when the underlying file read fails.
     */
    public void skipFrames(long frames) throws IOException {
        byte[] discard = new byte[8192];
        long remaining = Math.max(0L, frames);
        while (remaining > 0L) {
            int want = (int) Math.min((long) discard.length, remaining * BYTES_PER_FRAME);
            int read = read(discard, 0, want);
            if (read < 0) {
                return;
            }
            remaining -= read / BYTES_PER_FRAME;
        }
    }

    /**
     * Decodes audio and writes it as 16-bit stereo little-endian PCM into the buffer.
     *
     * @param buffer the destination buffer.
     * @param offset the write offset.
     * @param length the maximum number of bytes to write (rounded down to whole frames).
     * @return the number of bytes written, or {@code -1} at the end of the stream.
     * @throws IOException when the underlying file read fails.
     */
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int written = 0;
        int maxFrames = (length / BYTES_PER_FRAME) * BYTES_PER_FRAME;
        while (written < maxFrames) {
            if (this.srcPos >= this.srcBase + this.pcmSamples && !decodePacket()) {
                break;
            }
            float left;
            float right;
            if (this.rate == OUTPUT_RATE && this.channels == 2) {
                // Exact-rate fast path: no interpolation, direct sample conversion.
                int index = (int) (this.srcPos - this.srcBase);
                left = this.pcm[0][index];
                right = this.pcm[1][index];
                this.srcPos += 1.0D;
            } else {
                double integer = Math.floor(this.srcPos);
                int index = (int) (integer - this.srcBase);
                double fraction = this.srcPos - integer;
                left = interpolate(0, index, fraction);
                right = interpolate(this.channels > 1 ? 1 : 0, index, fraction);
                this.srcPos += this.rate / (double) OUTPUT_RATE;
            }
            writeShort(buffer, offset + written, toShort(left));
            writeShort(buffer, offset + written + 2, toShort(right));
            written += BYTES_PER_FRAME;
        }
        return written == 0 ? -1 : written;
    }

    /**
     * Releases the file handle. The decoder can be reopened afterwards.
     */
    public void close() {
        this.closed = true;
        if (this.in != null) {
            try {
                this.in.close();
            } catch (IOException ignored) {
                // already closed
            }
            this.in = null;
        }
    }

    /**
     * Reads the tags and the duration of an OGG Vorbis file without decoding any audio.
     *
     * @param file the file to probe.
     * @return the probe result, or {@code null} when the file is not a valid Vorbis stream.
     */
    public static ProbeResult probe(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        PhiVorbisDecoder decoder = new PhiVorbisDecoder();
        try {
            decoder.open(file);
            String title = decoder.getTag("TITLE");
            String artist = decoder.getTag("ARTIST");
            long frames = decoder.getTotalFrames();
            double duration = frames > 0L ? frames / (double) OUTPUT_RATE : 0.0D;
            return new ProbeResult(title, artist, duration);
        } catch (Throwable t) {
            return null;
        } finally {
            decoder.close();
        }
    }

    /**
     * @param file a file.
     * @return true when the extension marks the file as an OGG container.
     */
    public static boolean isVorbisFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String extension = name.substring(dot + 1).toLowerCase();
        return "ogg".equals(extension) || "oga".equals(extension);
    }

    /**
     * Parses the three Vorbis headers and initializes the synthesis state.
     */
    private void readHeaders() throws IOException {
        this.info.init();
        this.comment.init();
        this.sync.reset();
        this.stream = new StreamState();
        this.dsp = new DspState();
        this.block = new Block(this.dsp);
        this.rate = 0;
        this.channels = 0;
        this.pcm = null;
        this.pcmSamples = 0;
        this.srcBase = 0L;
        this.srcPos = 0.0D;

        if (!syncPage()) {
            throw new IOException("empty OGG file");
        }
        this.stream.init(this.page.serialno());
        this.stream.pagein(this.page);

        int headers = 0;
        while (headers < 3) {
            int result = this.stream.packetout(this.packet);
            if (result == 1) {
                if (this.info.synthesis_headerin(this.comment, this.packet) < 0) {
                    throw new IOException("corrupt vorbis header");
                }
                headers++;
            } else if (result == 0) {
                if (!feedPage()) {
                    throw new IOException("truncated vorbis header");
                }
            } else {
                throw new IOException("lost sync in the vorbis header");
            }
        }

        this.dsp.synthesis_init(this.info);
        this.block.init(this.dsp);
        this.rate = this.info.rate;
        this.channels = this.info.channels;
    }

    /**
     * Waits until a complete OGG page is available at the sync head and stores it in {@link #page}.
     * The page is not submitted to the stream state.
     *
     * @return false at the end of the file.
     */
    private boolean syncPage() throws IOException {
        while (this.sync.pageout(this.page) != 1) {
            int index = this.sync.buffer(SYNC_BUFFER_BYTES);
            int bytes = this.in.read(this.sync.data, index, SYNC_BUFFER_BYTES);
            if (bytes <= 0) {
                return false;
            }
            this.sync.wrote(bytes);
        }
        return true;
    }

    /**
     * Syncs the next page and submits it to the stream state without decoding packets.
     * Returns {@code false} at the end of the file or when a chained logical stream starts
     * (the decoder only understands the first stream).
     */
    private boolean feedPage() throws IOException {
        if (!syncPage()) {
            return false;
        }
        return this.stream.pagein(this.page) >= 0;
    }

    /**
     * Walks the remaining pages and returns the largest granule position (= the total source
     * samples of the stream). The file must be positioned after the headers.
     */
    private long scanGranule() throws IOException {
        long maxGranule = -1L;
        // Only page boundaries are needed; the pages are not submitted to the stream state.
        while (syncPage()) {
            long granule = this.page.granulepos();
            if (granule > maxGranule) {
                maxGranule = granule;
            }
        }
        return maxGranule;
    }

    /**
     * Decodes the next packet with available PCM and stores it as the current packet.
     *
     * @return true when new samples are available, false at the end of the stream.
     */
    private boolean decodePacket() throws IOException {
        while (!this.closed) {
            int result = this.stream.packetout(this.packet);
            if (result == 1) {
                if (this.block.synthesis(this.packet) == 0) {
                    this.dsp.synthesis_blockin(this.block);
                }
                float[][][] decoded = new float[1][][];
                int[] index = new int[this.channels];
                int samples = this.dsp.synthesis_pcmout(decoded, index);
                if (samples > 0) {
                    // Consume the PCM from the synthesis buffer, otherwise the decoder state
                    // never advances and the same samples repeat forever.
                    this.dsp.synthesis_read(samples);
                    // Keep the last sample of the previous packet for the interpolation at the
                    // boundary, then install the new packet. The global source index of the first
                    // sample of the new packet is the end of the previous packet.
                    if (this.pcm != null && this.pcmSamples > 0) {
                        this.prevL = this.pcm[0][this.pcmSamples - 1];
                        this.prevR = this.pcm[this.channels > 1 ? 1 : 0][this.pcmSamples - 1];
                        this.srcBase += this.pcmSamples;
                    }
                    this.pcm = new float[this.channels][];
                    for (int channel = 0; channel < this.channels; channel++) {
                        this.pcm[channel] = new float[samples];
                        System.arraycopy(decoded[0][channel], index[channel],
                                this.pcm[channel], 0, samples);
                    }
                    this.pcmSamples = samples;
                    return true;
                }
                // The packet contributed no samples yet (another packet must follow).
            } else if (result == 0) {
                if (!feedPage()) {
                    this.eof = true;
                    return false;
                }
            } else {
                this.eof = true;
                return false;
            }
        }
        this.eof = true;
        return false;
    }

    /**
     * Reads one source sample with linear interpolation for the resampling path.
     *
     * @param channel  the source channel.
     * @param index    the local packet index of the integer part.
     * @param fraction the fractional part in {@code [0, 1)}.
     */
    private float interpolate(int channel, int index, double fraction) {
        float current;
        float next;
        if (index < 0) {
            current = channel == 0 ? this.prevL : this.prevR;
            next = this.pcm[channel][0];
        } else if (index + 1 < this.pcmSamples) {
            current = this.pcm[channel][index];
            next = this.pcm[channel][index + 1];
        } else {
            current = this.pcm[channel][index];
            next = current;
        }
        return (float) (current * (1.0D - fraction) + next * fraction);
    }

    /**
     * Converts a {@code [-1, 1]} float sample to a 16-bit signed sample with clipping.
     */
    private static short toShort(float sample) {
        float clamped = Math.max(-1.0F, Math.min(1.0F, sample));
        return (short) (clamped * 32767.0F);
    }

    /**
     * Writes a short as little-endian bytes.
     */
    private static void writeShort(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * Locates a byte inside an array.
     */
    private static int indexOf(byte[] array, byte target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
}
