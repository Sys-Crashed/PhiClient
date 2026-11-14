package org.dpdns.syscrashed.phiclient.gui.component.text;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.PhiComponent;
import org.dpdns.syscrashed.phiclient.gui.component.PhiScrollable;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiScrollState;

/**
 * Abstract base of a panel whose text list can be scrolled
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.text}).
 *
 * <p>The panel is one of the three text abilities of the interface: its list is longer than the
 * viewport and is scrolled with the mouse wheel. The base owns the scroll state (inertia and rubber
 * band) and the wheel dispatch - the page forwards the wheel events to the panel because it implements
 * {@link PhiScrollable}.</p>
 *
 * <p><b>Scrolling</b>: the list does not move straight down, it moves along the diagonal of the
 * panel. One pixel up shifts the entries by the same distance to the right as the slant of the
 * interface ({@link #getSkewSlide()}), so every entry stays on the line of the slanted left edge - no
 * matter how far the list was scrolled.</p>
 */
public abstract class PhiScrollableTextPanel extends PhiComponent implements PhiScrollable {

    /** The scroll distance of a single wheel notch. */
    private static final float SCROLL_PER_NOTCH = 30.0F;

    /** The design scale factor of the interface. */
    protected final float uiScale;

    private final PhiScrollState scrollState = new PhiScrollState();

    private long lastFrame;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    protected PhiScrollableTextPanel(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.lastFrame = System.currentTimeMillis();
    }

    /**
     * @return the milliseconds since the previous frame (capped at 100 ms).
     */
    protected final float advanceFrame() {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        return deltaMs;
    }

    /**
     * @return the scroll state of the list (inertia, rubber band and bounds).
     */
    protected final PhiScrollState getScrollState() {
        return this.scrollState;
    }

    /**
     * @return the horizontal shift that belongs to the current scroll offset - one pixel of scroll
     *         moves the list {@link PhiSkew#SLOPE} pixels to the right.
     */
    protected final float getSkewSlide() {
        return PhiSkew.SLOPE * this.scrollState.getOffset();
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY)) {
            return false;
        }

        this.scrollState.scrollBy(notches * SCROLL_PER_NOTCH * this.uiScale);
        return true;
    }

    /**
     * Splits a text into lines that fit into the given width.
     *
     * @param nvg   the NanoVG drawing context.
     * @param text  the text to split.
     * @param size  the font size.
     * @param width the width of a line.
     * @return the lines of the text.
     */
    protected static List<String> wrap(NanoVGManager nvg, String text, float size, float width) {
        List<String> result = new ArrayList<String>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (nvg.textWidth(candidate, size, Fonts.REGULAR) > width && line.length() > 0) {
                result.add(line.toString());
                line = new StringBuilder(word);
            } else if (line.length() == 0) {
                // A single word that is longer than the column is shortened.
                line = new StringBuilder(nvg.limitText(word, size, Fonts.REGULAR, width));
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            result.add(line.toString());
        }

        return result;
    }
}
