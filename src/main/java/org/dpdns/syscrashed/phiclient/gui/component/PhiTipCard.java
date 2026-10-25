package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.tips.PhiTips;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiToast;
import org.dpdns.syscrashed.phiclient.utils.PhiGameInfo;
import org.lwjgl.nanovg.NanoVG;

/**
 * The tip card of the home page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The card is split into three rows:</p>
 * <ol>
 *   <li>the <b>headline row</b> with the title of the card and the number of the tip;</li>
 *   <li>the <b>action row</b> with three interactive chips: copy the address of the server, copy the
 *       position of the player and switch the theme;</li>
 *   <li>the <b>value row</b> with three chips that only show a value: the time, the weather and the
 *       dimension of the game world.</li>
 * </ol>
 *
 * <p>The tip itself is written out character by character below the headline - it is always complete
 * after one second. A click on the text completes it immediately, a second click switches to the next
 * tip.</p>
 *
 * <p>The chips are upright rectangles: a row of leaning chips would not look even. They share the
 * content column of the card, so they start at the same x and have the same width.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the card.</li>
 * </ul>
 */
public class PhiTipCard extends PhiComponent {

    /** The time the tip needs to be written out completely. */
    private static final float REVEAL_DURATION = 1000.0F;

    private final float uiScale;

    private final List<String> tips = PhiTips.getTips();

    /** The wrapped lines of the tip that is currently shown. */
    private final List<String> tipLines = new ArrayList<String>();

    /** The index of the last character of every wrapped line inside the tip. */
    private final List<Integer> tipLineEnds = new ArrayList<Integer>();

    /** The three interactive chips. */
    private final List<PhiChip> actionChips = new ArrayList<PhiChip>();

    /** The three chips that only show a value. */
    private final List<PhiChip> valueChips = new ArrayList<PhiChip>();

    /** The tip the wrapped lines belong to. */
    private int wrappedIndex = -1;

    /** The width the wrapped lines were built for. */
    private float wrappedWidth = -1.0F;

    private int index;

    /** The number of characters that are currently visible. */
    private float revealed;

    /** The number of characters that are written per second. */
    private float charactersPerSecond;

    private long lastFrame;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiTipCard(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.lastFrame = System.currentTimeMillis();

        // Row 2: the interactive chips.
        this.actionChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Copy IP", true));
        this.actionChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Copy Coord", true));
        this.actionChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Theme", true));

        // Row 3: the chips that only show a value.
        this.valueChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Time", false));
        this.valueChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Weather", false));
        this.valueChips.add(new PhiChip(0.0F, 0.0F, 0.0F, 0.0F, uiScale, "Dimension", false));
    }
    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        String tip = this.tips.get(this.index);

        // The tip is always written out completely after one second.
        if (this.charactersPerSecond <= 0.0F) {
            this.charactersPerSecond = tip.length() * 1000.0F / REVEAL_DURATION;
        }

        this.revealed += deltaMs * this.charactersPerSecond / 1000.0F;
        if (this.revealed > tip.length()) {
            this.revealed = tip.length();
        }

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float lineHeight = bodySize * 1.35F;

        float chipHeight = PhiMetrics.CHIP_HEIGHT * this.uiScale;
        float chipGap = PhiMetrics.CHIP_GAP * this.uiScale;

        // The one content column of the card.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = contentRight - contentLeft;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        // Row 1: the headline and the number of the tip.
        float titleY = this.y + padding * 0.9F;

        nvg.drawText("Tip", contentLeft, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        String counter = (this.index + 1) + " / " + this.tips.size();

        nvg.drawTextAligned(counter, contentRight, titleY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // The two rows of chips at the bottom of the card.
        float valueRowY = this.y + this.height - padding - chipHeight;
        float actionRowY = valueRowY - chipGap - chipHeight;
        float dividerY = actionRowY - padding * 0.7F;

        float chipWidth = (contentWidth - chipGap * 2.0F) / 3.0F;

        // The values of the game world change while the card is open.
        this.valueChips.get(0).setLabel("Time " + PhiGameInfo.getTime());
        this.valueChips.get(1).setLabel("Weather " + PhiGameInfo.getWeather());
        this.valueChips.get(2).setLabel("Dimension " + PhiGameInfo.getDimension());

        for (int i = 0; i < this.actionChips.size(); i++) {
            PhiChip chip = this.actionChips.get(i);

            chip.setX(contentLeft + i * (chipWidth + chipGap));
            chip.setY(actionRowY);
            chip.setWidth(chipWidth);
            chip.setHeight(chipHeight);
        }

        for (int i = 0; i < this.valueChips.size(); i++) {
            PhiChip chip = this.valueChips.get(i);

            chip.setX(contentLeft + i * (chipWidth + chipGap));
            chip.setY(valueRowY);
            chip.setWidth(chipWidth);
            chip.setHeight(chipHeight);
        }

        // Divider above the rows of chips.
        nvg.drawRect(contentLeft, dividerY, contentWidth, Math.max(1.0F, this.uiScale), PhiTheme.level2());

        // The tip itself, written out character by character.
        float tipY = titleY + titleSize * 1.5F;
        int maxLines = Math.max(1, (int) ((dividerY - bodySize * 0.5F - tipY) / lineHeight));

        wrapTip(nvg, contentWidth, bodySize);

        int revealedChars = (int) this.revealed;
        float cursorX = -1.0F;
        float cursorY = tipY;

        for (int line = 0; line < this.tipLines.size() && line < maxLines; line++) {
            String text = this.tipLines.get(line);
            int lineStart = line == 0 ? 0 : this.tipLineEnds.get(line - 1) + 1;
            int lineEnd = this.tipLineEnds.get(line);

            if (revealedChars <= lineStart) {
                break;
            }

            if (revealedChars < lineEnd) {
                // The line is still being written.
                text = text.substring(0, Math.max(0, revealedChars - lineStart));
            }

            float lineY = tipY + line * lineHeight;

            nvg.drawText(text, contentLeft, lineY, PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

            cursorX = contentLeft + nvg.textWidth(text, bodySize, Fonts.REGULAR);
            cursorY = lineY;
        }

        // The blinking cursor at the end of the text that was written so far.
        if (cursorX >= 0.0F && revealedChars < tip.length()) {
            nvg.drawRect(cursorX, cursorY, Math.max(1.0F, bodySize * 0.08F), bodySize, PhiTheme.accent());
        }

        // The chips of the two rows.
        for (PhiChip chip : this.actionChips) {
            chip.draw(nvg, mouseX, mouseY, partialTicks);
        }

        for (PhiChip chip : this.valueChips) {
            chip.draw(nvg, mouseX, mouseY, partialTicks);
        }
    }
    /**
     * Binds the actions of the three interactive chips of the action row. The order of the list is the
     * order of the chips: copy the address, copy the position, switch the theme.
     *
     * @param actions the actions of the chips.
     */
    public void setActions(List<PhiChip.Action> actions) {
        for (int i = 0; i < this.actionChips.size() && i < actions.size(); i++) {
            this.actionChips.get(i).setAction(actions.get(i));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        // The chips have the priority - they are inside the card as well.
        for (PhiChip chip : this.actionChips) {
            chip.mouseClicked(mouseX, mouseY, mouseButton);
        }

        for (PhiChip chip : this.actionChips) {
            if (chip.isInside(mouseX, mouseY)) {
                return;
            }
        }

        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return;
        }

        String tip = this.tips.get(this.index);

        if (this.revealed < tip.length()) {
            // The first click completes the text.
            this.revealed = tip.length();
            return;
        }

        this.index = (this.index + 1) % this.tips.size();
        this.revealed = 0.0F;
        this.charactersPerSecond = 0.0F;
    }

    /**
     * Wraps the tip that is currently shown into lines that fit into the content column. The result is
     * cached, so the lines do not jump while the tip is written out.
     *
     * @param nvg      the NanoVG drawing context.
     * @param width    the width of the content column.
     * @param bodySize the font size of the tip.
     */
    private void wrapTip(NanoVGManager nvg, float width, float bodySize) {
        if (this.wrappedIndex == this.index && this.wrappedWidth == width) {
            return;
        }

        this.wrappedIndex = this.index;
        this.wrappedWidth = width;
        this.tipLines.clear();
        this.tipLineEnds.clear();

        String tip = this.tips.get(this.index);
        int position = 0;
        int lineStart = 0;
        StringBuilder line = new StringBuilder();

        while (position < tip.length()) {
            int wordEnd = tip.indexOf(' ', position);

            if (wordEnd < 0) {
                wordEnd = tip.length();
            }

            String word = tip.substring(position, wordEnd);
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (nvg.textWidth(candidate, bodySize, Fonts.REGULAR) > width && line.length() > 0) {
                this.tipLines.add(line.toString());
                this.tipLineEnds.add(lineStart + line.length());

                lineStart = position;
                line = new StringBuilder(word);
            } else if (line.length() == 0) {
                // A single word that is longer than the column is shortened.
                line = new StringBuilder(nvg.limitText(word, bodySize, Fonts.REGULAR, width));
            } else {
                line = new StringBuilder(candidate);
            }

            position = wordEnd + 1;
        }

        if (line.length() > 0) {
            this.tipLines.add(line.toString());
            this.tipLineEnds.add(lineStart + line.length());
        }
    }

    /**
     * @return the toast of the interface.
     */
    protected static PhiToast toast() {
        return PhiClient.getInstance().getGuiManager().getToast();
    }
}