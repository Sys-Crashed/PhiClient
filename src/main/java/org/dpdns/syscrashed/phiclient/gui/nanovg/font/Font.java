package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.nio.ByteBuffer;

/**
 * A single NanoVG font (TTF) (package {@code org.dpdns.syscrashed.phiclient.gui.nanovg.font}).
 *
 * <p>The resource is referenced by a classpath path (not a Minecraft {@code ResourceLocation}) -
 * see {@link org.dpdns.syscrashed.phiclient.utils.ResourceUtils} for the reason. Once registered
 * with NanoVG, the raw buffer must be kept alive for the lifetime of the font.</p>
 *
 * <p>Provided accessors:</p>
 * <ul>
 *   <li>{@link #getName()} - the registered font name.</li>
 *   <li>{@link #getClasspath()} - the TTF location on the classpath, e.g. {@code /assets/phiclient/fonts/x.ttf}.</li>
 *   <li>{@link #isLoaded()} / {@link #setLoaded(boolean)} - registration state.</li>
 *   <li>{@link #getBuffer()} / {@link #setBuffer(ByteBuffer)} - the raw font data.</li>
 * </ul>
 */
public final class Font {

    private final String name;
    private final String classpath;

    private ByteBuffer buffer;
    private boolean loaded;

    public Font(String name, String classpath) {
        this.name = name;
        this.classpath = classpath;
    }

    /**
     * @return the registered font name, used as the NanoVG font face name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the font file location on the classpath, e.g. {@code /assets/phiclient/fonts/x.ttf}.
     */
    public String getClasspath() {
        return this.classpath;
    }

    /**
     * @return true if this font has been registered with a NanoVG context.
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Marks whether this font has been registered with a NanoVG context.
     *
     * @param loaded the new loaded state.
     */
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * The raw font data. <b>Must be kept referenced</b>: {@code nvgCreateFontMem(..., freeData = false)}
     * means NanoVG keeps an internal pointer to this memory, so reclaiming it would read freed memory.
     *
     * @return the direct buffer holding the TTF bytes.
     */
    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    /**
     * Stores the raw font data buffer (takes ownership; must not be freed while the font is in use).
     *
     * @param buffer the direct buffer holding the TTF bytes.
     */
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
