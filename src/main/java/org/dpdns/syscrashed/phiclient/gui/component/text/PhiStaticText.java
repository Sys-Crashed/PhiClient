package org.dpdns.syscrashed.phiclient.gui.component.text;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Font;

/**
 * A non scrollable static text of a component
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.text}).
 *
 * <p>The text is one of the three text abilities of the interface: it is shortened to the width of
 * its column ({@link NanoVGManager#limitText}) and the result is cached until the text, the font, the
 * size or the width changes, so the draw path does not shorten an unchanged text in every frame.</p>
 */
public final class PhiStaticText {

    private String content = "";

    /** The content the cached line was built from. */
    private String limitedContent;

    private String limited;

    private float limitedSize = -1.0F;

    private Font limitedFont;

    private float limitedWidth = -1.0F;

    /**
     * Replaces the text. The cached line is rebuilt on the next call of
     * {@link #getLimited(NanoVGManager, float, Font, float)}.
     *
     * @param text the new text (null is treated as an empty text).
     */
    public void setContent(String text) {
        this.content = text == null ? "" : text;
    }

    /**
     * @return the current text.
     */
    public String getContent() {
        return this.content;
    }

    /**
     * @param nvg      the NanoVG drawing context.
     * @param size     the font size.
     * @param font     the font.
     * @param maxWidth the width the text must fit into.
     * @return the text shortened to the width.
     */
    public String getLimited(NanoVGManager nvg, float size, Font font, float maxWidth) {
        if (this.limited != null && this.limitedContent == this.content
                && size == this.limitedSize && font.equals(this.limitedFont)
                && maxWidth == this.limitedWidth) {
            return this.limited;
        }

        this.limitedContent = this.content;
        this.limited = nvg.limitText(this.content, size, font, maxWidth);
        this.limitedSize = size;
        this.limitedFont = font;
        this.limitedWidth = maxWidth;

        return this.limited;
    }
}
