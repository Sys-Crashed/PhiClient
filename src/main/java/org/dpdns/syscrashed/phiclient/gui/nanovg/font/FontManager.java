package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.nio.ByteBuffer;

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
}
