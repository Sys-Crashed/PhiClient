package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.changelog.PhiChangeLog;
import org.dpdns.syscrashed.phiclient.gui.changelog.PhiChangeLogManager;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiScrollState;
import org.lwjgl.nanovg.NanoVG;

/**
 * The change log panel of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The panel shows the recent changes of the client: every entry has a badge with its type and a
 * description that is wrapped over several lines when it does not fit into one line. When the entries
 * do not fit into the panel, the list can be scrolled with the mouse wheel.</p>
 *
 * <p><b>Layout rule</b>: the entries follow the slanted left edge of the panel - every entry owns
 * <b>one</b> anchor on that diagonal. The badge is placed on the anchor and the description starts
 * right behind it, so the distance between the badge and its text is always the same. Every further
 * line of a description is shifted to the left by the same slope the interface uses everywhere, so the
 * wrapped text stays parallel to the edge of the panel and never runs into the right border.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the panel.</li>
 *   <li>{@link #onWheel(float, int, int)} - scrolls the list.</li>
 * </ul>
 */
public class PhiChangeLogPanel extends PhiComponent implements PhiScrollable {

    /** The minimum width of a type badge. */
    private static final float BADGE_MIN_WIDTH = 46.0F;

    /** The height of a type badge. */
    private static final float BADGE_HEIGHT = 16.0F;

    /** The horizontal distance between a badge and the text behind it. */
    private static final float BADGE_GAP = 8.0F;

    /** The vertical distance between two entries. */
    private static final float ENTRY_SPACING = 14.0F;

    /** The scroll distance of a single wheel notch. */
    private static final float SCROLL_PER_NOTCH = 30.0F;

    private final float uiScale;

    private final List<PhiChangeLog> changeLogs = PhiChangeLogManager.getInstance().getChangeLogs();

    private final PhiScrollState scrollState = new PhiScrollState();

    private long lastFrame;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiChangeLogPanel(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.lastFrame = System.currentTimeMillis();
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        // The content column of the panel.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = contentRight - contentLeft;

        // Title and number of entries.
        float titleY = this.y + padding * 0.9F;

        nvg.drawText("ChangeLog", contentLeft, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        String counter = this.changeLogs.size() + " changes";

        nvg.drawTextAligned(counter, contentRight, titleY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        float dividerY = titleY + titleSize * 1.25F;

        nvg.drawRect(contentLeft, dividerY, contentWidth, Math.max(1.0F, this.uiScale), PhiTheme.level2());

        float badgeHeight = BADGE_HEIGHT * this.uiScale;
        float badgeTextSize = bodySize * 0.85F;
        float lineHeight = bodySize * 1.35F;
        float listTop = dividerY + padding * 0.9F;

        // All badges share one width, so the descriptions of the entries are aligned with each other.
        // The width is large enough for the longest label ("Improved").
        float badgeWidth = BADGE_MIN_WIDTH * this.uiScale;

        for (PhiChangeLog entry : this.changeLogs) {
            float needed = nvg.textWidth(entry.getType().getLabel(), badgeTextSize, Fonts.REGULAR) + padding * 1.1F;
            badgeWidth = Math.max(badgeWidth, needed);
        }

        // The text of an entry starts behind its badge. The width of the column is the same for every
        // entry, because the badge and the text move together along the diagonal.
        float textGap = BADGE_GAP * this.uiScale;
        float textWidth = contentWidth - badgeWidth - textGap;

        if (textWidth < bodySize * 2.0F) {
            textWidth = Math.max(bodySize * 2.0F, contentWidth * 0.5F);
        }

        // The lines of every entry, in the order of the list.
        List<List<String>> lines = new ArrayList<List<String>>();
        float contentHeight = 0.0F;

        for (PhiChangeLog entry : this.changeLogs) {
            List<String> wrapped = wrap(nvg, entry.getText(), bodySize, textWidth);

            lines.add(wrapped);

            float entryHeight = Math.max(badgeHeight, wrapped.size() * lineHeight);
            contentHeight += entryHeight + ENTRY_SPACING * this.uiScale;
        }

        float viewportHeight = this.y + this.height - listTop - padding * 0.5F;
        this.scrollState.setContentSize(contentHeight, viewportHeight);
        this.scrollState.update(deltaMs);

        float cursor = listTop - this.scrollState.getOffset();

        for (int i = 0; i < this.changeLogs.size(); i++) {
            PhiChangeLog entry = this.changeLogs.get(i);
            List<String> wrapped = lines.get(i);

            float entryHeight = Math.max(badgeHeight, wrapped.size() * lineHeight);

            if (cursor + entryHeight > this.y && cursor < this.y + this.height) {
                // The anchor of the entry: the badge sits on the diagonal of the panel.
                float anchorX = PhiSkew.leftEdgeAt(this.x, this.y, this.height, cursor) + padding;

                nvg.drawSkewRect(anchorX, cursor, badgeWidth, badgeHeight, badgeHeight * 0.35F,
                        entry.getType().getBadgeColor());

                float badgeCenterX = anchorX + PhiSkew.offset(badgeHeight) * 0.5F + badgeWidth * 0.5F;
                float badgeTextY = cursor + (badgeHeight - badgeTextSize) * 0.5F;

                nvg.drawTextAligned(entry.getType().getLabel(), badgeCenterX, badgeTextY,
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP,
                        entry.getType().getTextColor(), badgeTextSize, Fonts.REGULAR);

                // The description starts behind the badge and follows the diagonal.
                float lineX = anchorX + badgeWidth + textGap;
                float lineY = cursor + (badgeHeight - bodySize) * 0.5F;
                float lineShift = PhiSkew.SLOPE * lineHeight;

                for (int line = 0; line < wrapped.size(); line++) {
                    nvg.drawText(wrapped.get(line), lineX - lineShift * line, lineY,
                            PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

                    lineY += lineHeight;
                }
            }

            cursor += entryHeight + ENTRY_SPACING * this.uiScale;
        }
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
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
    private List<String> wrap(NanoVGManager nvg, String text, float size, float width) {
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
