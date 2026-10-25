package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.tips.PhiTips;
import org.lwjgl.nanovg.NanoVG;

/**
 * The tip card of the home page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The card shows a single tip: the headline with the word "Tip" and the text of the tip, which is
 * written out character by character and is always complete after one second. A click on the text
 * completes it immediately, a second click switches to another tip - the tip is picked at random, so
 * the list never runs through in the same order twice. The hint in the corner of the card tells the
 * player that the card can be clicked.</p>
 *
 * <p>The card owns exactly one content column (see {@link PhiCard}), so the headline, the text of the
 * tip and the hint all share the same two x positions and cannot drift apart.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the card.</li>
 * </ul>
 */
public class PhiTipCard extends PhiComponent {

    /** The time the tip needs to be written out completely. */
    private static final float REVEAL_DURATION = 1000.0F;

    /** The hint that tells the player that the card can be clicked. */
    private static final String HINT = "Click to switch tip";

    private final float uiScale;

    private final List<String> tips = PhiTips.getTips();

    /** The random source that picks the next tip. */
    private final Random random = new Random();

    /** The wrapped lines of the tip that is currently shown. */
    private final List<String> tipLines = new ArrayList<String>();

    /** The index of the last character of every wrapped line inside the tip. */
    private final List<Integer> tipLineEnds = new ArrayList<Integer>();

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
        float hintSize = bodySize * 0.9F;
        float lineHeight = bodySize * 1.35F;

        // The one content column of the card.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = contentRight - contentLeft;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        // The headline of the card.
        float titleY = this.y + padding * 0.9F;

        nvg.drawText("Tip", contentLeft, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        // The hint in the lower right corner of the card.
        float hintY = this.y + this.height - padding * 0.9F - hintSize;

        nvg.drawTextAligned(HINT, contentRight, hintY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), hintSize, Fonts.REGULAR);

        // The tip itself, written out character by character.
        float tipY = titleY + titleSize * 1.5F;
        int maxLines = Math.max(1, (int) ((hintY - bodySize * 0.6F - tipY) / lineHeight));

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
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
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

        // The next tip is picked at random - the list never runs through in the same order twice.
        int next = this.index;

        while (next == this.index && this.tips.size() > 1) {
            next = this.random.nextInt(this.tips.size());
        }

        this.index = next;
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
}
