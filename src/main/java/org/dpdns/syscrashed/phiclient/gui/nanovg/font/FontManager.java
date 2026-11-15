package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.utils.ResourceUtils;
import org.lwjgl.nanovg.NanoVG;

/**
 * Registers the fonts from {@link Fonts} into a NanoVG context
 * (package {@code org.dpdns.syscrashed.phiclient.gui.nanovg.font}).
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #init(long)} - registers every font in {@link Fonts} into the given NanoVG context.</li>
 * </ul>
 */
public final class FontManager {

    /** The NanoVG face name of the optional CJK fallback font. */
    private static final String CJK_FALLBACK_NAME = "cjk-fallback";

    /**
     * Keeps the raw font data of the CJK fallback alive: NanoVG borrows the memory
     * ({@code freeData = false}) and never copies it.
     */
    private static ByteBuffer cjkFallbackBuffer;

    /**
     * Registers all known fonts into the given NanoVG context.
     *
     * <p>Must be called after the NanoVG context has been created. Each font is read from the
     * classpath and registered with {@code nvgCreateFontMem(..., freeData = false)}, so the {@link Font}
     * keeps ownership of the buffer.</p>
     *
     * @param context the NanoVG context handle (as returned by {@code nvgCreate}).
     */
    public void init(long context) {
        for (Font font : Fonts.getFonts()) {
            load(context, font);
        }

        registerCjkFallback(context);
    }

    /**
     * Loads a single font into the context (skips it if already loaded).
     *
     * @param context the NanoVG context handle.
     * @param font    the font to register.
     */
    private void load(long context, Font font) {
        if (font.isLoaded()) {
            return;
        }

        ByteBuffer buffer = ResourceUtils.classpathToByteBuffer(font.getClasspath(), Font.class);
        if (buffer == null) {
            PhiLogger.error("Failed to read font resource: " + font.getClasspath());
            return;
        }

        // freeData = false: the font data is owned by Font and only borrowed by NanoVG.
        int id = NanoVG.nvgCreateFontMem(context, font.getName(), buffer, false);
        if (id < 0) {
            PhiLogger.error("Failed to create NanoVG font: " + font.getName());
            return;
        }

        font.setBuffer(buffer);
        font.setLoaded(true);
        PhiLogger.info("NanoVG font loaded: " + font.getName() + " (id=" + id + ")");
    }

    /**
     * Registers an optional CJK fallback for {@link Fonts#REGULAR}.
     *
     * <p>The bundled font is expected to cover Simplified Chinese and Japanese Kana, but if a glyph
     * is missing anyway NanoVG would render a tofu box. This registers a system CJK font (when one
     * exists) and chains it as fallback of the regular font: it is only used for glyphs the regular
     * font does not provide. Best effort only - when no system font is found the UI keeps working,
     * it just cannot substitute missing glyphs.</p>
     *
     * @param context the NanoVG context handle.
     */
    private void registerCjkFallback(long context) {
        if (cjkFallbackBuffer != null) {
            return;
        }

        int baseId = NanoVG.nvgFindFont(context, Fonts.REGULAR.getName());
        if (baseId < 0) {
            PhiLogger.warn("PhiI18n - regular font not registered, skipping CJK fallback");
            return;
        }

        File fontFile = findSystemCjkFont();
        if (fontFile == null) {
            PhiLogger.info("PhiI18n - no system CJK font found, relying on the bundled font coverage");
            return;
        }

        ByteBuffer buffer = readFileToNativeBuffer(fontFile);
        if (buffer == null) {
            PhiLogger.warn("PhiI18n - failed to read system CJK font: " + fontFile);
            return;
        }

        int fallbackId = NanoVG.nvgCreateFontMem(context, CJK_FALLBACK_NAME, buffer, false);
        if (fallbackId < 0) {
            PhiLogger.warn("PhiI18n - failed to register system CJK font: " + fontFile);
            return;
        }

        // Keep the buffer alive: NanoVG only borrows the memory.
        cjkFallbackBuffer = buffer;

        NanoVG.nvgAddFallbackFontId(context, baseId, fallbackId);
        PhiLogger.info("PhiI18n - CJK fallback font registered: " + fontFile.getName() + " (id=" + fallbackId + ")");
    }

    /**
     * Looks for a system font that covers CJK glyphs, per operating system.
     *
     * @return the first existing candidate file, or {@code null} when none exists.
     */
    private File findSystemCjkFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] candidates;

        if (os.contains("win")) {
            candidates = new String[] {
                    "C:\\Windows\\Fonts\\msyh.ttc",       // Microsoft YaHei
                    "C:\\Windows\\Fonts\\msyh.ttf",
                    "C:\\Windows\\Fonts\\simhei.ttf",     // SimHei
                    "C:\\Windows\\Fonts\\simsun.ttc",     // SimSun
                    "C:\\Windows\\Fonts\\meiryo.ttc",     // Meiryo (Japanese)
                    "C:\\Windows\\Fonts\\msgothic.ttc"    // MS Gothic (Japanese)
            };
        } else if (os.contains("mac")) {
            candidates = new String[] {
                    "/System/Library/Fonts/PingFang.ttc",
                    "/System/Library/Fonts/Hiragino Sans GB.ttc",
                    "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc"
            };
        } else {
            candidates = new String[] {
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
            };
        }

        for (String path : candidates) {
            File file = new File(path);
            if (file.isFile()) {
                return file;
            }
        }

        return null;
    }

    /**
     * Reads a font file into a direct, native-ordered buffer (the layout NanoVG expects for
     * {@code nvgCreateFontMem}).
     *
     * @param file the font file.
     * @return the direct buffer, or {@code null} when the read fails.
     */
    private ByteBuffer readFileToNativeBuffer(File file) {
        InputStream in = null;
        try {
            byte[] bytes = new byte[(int) file.length()];
            in = new FileInputStream(file);
            int offset = 0;

            while (offset < bytes.length) {
                int read = in.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(offset).order(ByteOrder.nativeOrder());
            buffer.put(bytes, 0, offset);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            PhiLogger.warn("PhiI18n - failed to read font file: " + file);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
}
