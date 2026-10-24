package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.nio.ByteBuffer;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.utils.ResourceUtils;
import org.lwjgl.nanovg.NanoVG;

/**
 * 把 {@link Fonts} 里的字体注册进 NanoVG 上下文。
 *
 * <p>把 {@link Fonts} 里的字体注册进 NanoVG 上下文。
 */
public final class FontManager {

    /**
     * 在给定上下文中注册全部字体。
     *
     * <p>必须在 NanoVG 上下文创建之后调用。
     */
    public void init(long context) {
        for (Font font : Fonts.getFonts()) {
            load(context, font);
        }
    }

    private void load(long context, Font font) {
        if (font.isLoaded()) {
            return;
        }

        ByteBuffer buffer = ResourceUtils.classpathToByteBuffer(font.getClasspath(), Font.class);
        if (buffer == null) {
            PhiLogger.error("Failed to read font resource: " + font.getClasspath());
            return;
        }

        // freeData = false：字体数据由 Font 持有，NanoVG 只借用指针
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
