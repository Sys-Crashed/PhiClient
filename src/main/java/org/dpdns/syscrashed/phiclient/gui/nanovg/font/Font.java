package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.nio.ByteBuffer;

/**
 * 一个 NanoVG 字体（TTF）。
 *
 * <p>资源用 classpath 路径
 * （而不是 ResourceLocation）—— 原因见 {@link org.dpdns.syscrashed.phiclient.utils.ResourceUtils}。
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

    public String getName() {
        return this.name;
    }

    /**
     * 字体文件在 classpath 上的位置，例如 {@code /assets/phiclient/fonts/x.ttf}。
     */
    public String getClasspath() {
        return this.classpath;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * 字体原始数据。<b>必须一直持有</b>：{@code nvgCreateFontMem(..., freeData = false)}
     * 意味着 NanoVG 内部保存着这块内存的指针，回收掉就会读到野指针。
     */
    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
