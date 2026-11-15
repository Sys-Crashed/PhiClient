package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.changelog.PhiChangeLog;
import org.dpdns.syscrashed.phiclient.gui.changelog.PhiChangeLogManager;
import org.dpdns.syscrashed.phiclient.gui.component.text.PhiScrollableTextPanel;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.lwjgl.nanovg.NanoVG;

/**
 * The change log panel of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The panel shows the recent changes of the client: every entry has a badge with its type and a
 * description that is wrapped over several lines when it does not fit into one line. When the entries
 * do not fit into the panel, the list can be scrolled with the mouse wheel - the scroll ability and
 * its diagonal movement live in the base class {@link PhiScrollableTextPanel}.</p>
 *
 * <p><b>Layout rule</b>: the entries follow the slanted left edge of the panel - every entry owns
 * <b>one</b> anchor on that diagonal. The badge is placed on the anchor and the description starts
 * right behind it, so the distance between the badge and its text is always the same. Every further
 * line of a description is shifted to the left by the same slope the interface uses everywhere, so the
 * wrapped text stays parallel to the edge of the panel and never runs into the right border.</p>
 */
public class PhiChangeLogPanel extends PhiScrollableTextPanel {

    /** The minimum width of a type badge. */
    private static final float BADGE_MIN_WIDTH = 46.0F;

    /** The height of a type badge. */
    private static final float BADGE_HEIGHT = 16.0F;

    /** The horizontal distance between a badge and the text behind it. */
    private static final float BADGE_GAP = 8.0F;

    /** The vertical distance between two entries. */
    private static final float ENTRY_SPACING = 14.0F;

    private final List<PhiChangeLog> changeLogs = PhiChangeLogManager.getInstance().getChangeLogs();

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiChangeLogPanel(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height, uiScale);
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float deltaMs = advanceFrame();

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        // Title and number of entries. The whole panel uses one diagonal: the headline, the divider and
        // the entries all sit on the slanted left edge of the panel, so there is only one baseline.
        float titleY = this.y + padding * 0.9F;
        float titleLeft = PhiSkew.leftEdgeAt(this.x, this.y, this.height, titleY) + padding;
        float titleRight = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height, titleY) - padding;

        nvg.drawText(PhiI18n.get("changelog.panel.title"), titleLeft, titleY,
                PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        String counter = PhiI18n.tr("changelog.count", this.changeLogs.size());

        nvg.drawTextAligned(counter, titleRight, titleY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        float dividerY = titleY + titleSize * 1.25F;
        float dividerLeft = PhiSkew.leftEdgeAt(this.x, this.y, this.height, dividerY) + padding;
        float dividerRight = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height, dividerY) - padding;

        nvg.drawRect(dividerLeft, dividerY, dividerRight - dividerLeft, Math.max(1.0F, this.uiScale),
                PhiTheme.level2());

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
        // entry: the entries move together along the diagonal of the panel, so the distance to the
        // right edge of the panel stays the same.
        float textGap = BADGE_GAP * this.uiScale;
        float textWidth = this.width - padding * 2.0F - badgeWidth - textGap;

        if (textWidth < bodySize * 2.0F) {
            textWidth = Math.max(bodySize * 2.0F, (this.width - padding * 2.0F) * 0.5F);
        }

        // The lines of every entry, in the order of the list.
        List<List<String>> lines = new ArrayList<List<String>>();
        List<Float> anchors = new ArrayList<Float>();
        float contentHeight = 0.0F;
        float layoutCursor = listTop;

        for (PhiChangeLog entry : this.changeLogs) {
            List<String> wrapped = wrap(nvg, entry.getText(), bodySize, textWidth);

            lines.add(wrapped);
            anchors.add(PhiSkew.leftEdgeAt(this.x, this.y, this.height, layoutCursor) + padding);

            float entryHeight = Math.max(badgeHeight, wrapped.size() * lineHeight);
            contentHeight += entryHeight + ENTRY_SPACING * this.uiScale;

            layoutCursor += entryHeight + ENTRY_SPACING * this.uiScale;
        }

        float viewportHeight = this.y + this.height - listTop - padding * 0.5F;
        getScrollState().setContentSize(contentHeight, viewportHeight);
        getScrollState().update(deltaMs);

        float offset = getScrollState().getOffset();

        // The list does not scroll straight down, it scrolls along the diagonal of the panel: one
        // pixel up moves the entries the same distance to the right as the slant of the interface.
        // Every entry therefore stays on the same line as the slanted left edge of the panel and
        // never drifts away from it while the list is scrolled.
        float slide = getSkewSlide();

        // The list is clipped to the area below the divider, so an entry that is scrolled out of the
        // list cannot reach the headline of the panel - and it cannot reach the blocks of the other
        // column either.
        nvg.save();
        float shapeOffset = PhiSkew.offset(this.height);
        float listHeight = this.height - (listTop - this.y);

        nvg.translate(this.x + shapeOffset, this.y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);
        nvg.scissor(0.0F, listTop - this.y, this.width, listHeight);
        nvg.skewX(-PhiSkew.SHEAR_DEGREES);
        nvg.translate(-(this.x + shapeOffset), -this.y);

        float cursor = listTop - offset;

        for (int i = 0; i < this.changeLogs.size(); i++) {
            PhiChangeLog entry = this.changeLogs.get(i);
            List<String> wrapped = lines.get(i);

            float entryHeight = Math.max(badgeHeight, wrapped.size() * lineHeight);

            if (cursor + entryHeight > listTop && cursor < this.y + this.height) {
                // The anchor of the entry follows the diagonal, so the badge and its text keep the
                // same distance to each other and to the border of the panel.
                float anchorX = anchors.get(i) + slide;

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

                // The lean of the text is limited, so even a long description never leaves the
                // panel on the left.
                float maxLean = Math.max(0.0F, (padding + badgeWidth + textGap
                        - PhiSkew.SLOPE * (badgeHeight - bodySize) * 0.5F) * 0.5F);

                for (int line = 0; line < wrapped.size(); line++) {
                    float lean = Math.min(maxLean, lineShift * line);

                    nvg.drawText(wrapped.get(line), lineX - lean, lineY,
                            PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

                    lineY += lineHeight;
                }
            }

            cursor += entryHeight + ENTRY_SPACING * this.uiScale;
        }

        nvg.restore();
    }
}
