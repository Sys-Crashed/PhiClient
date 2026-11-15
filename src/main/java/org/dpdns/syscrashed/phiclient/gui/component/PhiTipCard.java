package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.text.PhiTypewriterText;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.tips.PhiTips;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
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
 * tip and the hint all share the same two x positions and cannot drift apart. The written-out text is
 * delegated to {@link PhiTypewriterText}; hover and click share the same shape rule of the card.</p>
 */
public class PhiTipCard extends PhiComponent {

    private final float uiScale;

    /** The text of the card, written out character by character and switched at random. */
    private final PhiTypewriterText typewriter = new PhiTypewriterText(PhiTips.getTips());

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
        updateHover(mouseX, mouseY);

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        this.typewriter.advance(deltaMs);

        String tip = this.typewriter.getText();

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

        nvg.drawText(PhiI18n.get("home.tip.title"), contentLeft, titleY,
                PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        // The hint in the lower right corner of the card.
        float hintY = this.y + this.height - padding * 0.9F - hintSize;

        nvg.drawTextAligned(PhiI18n.get("home.tip.hint"), contentRight, hintY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), hintSize, Fonts.REGULAR);

        // The tip itself, written out character by character.
        float tipY = titleY + titleSize * 1.5F;
        int maxLines = Math.max(1, (int) ((hintY - bodySize * 0.6F - tipY) / lineHeight));

        this.typewriter.wrap(nvg, contentWidth, bodySize);

        int revealedChars = this.typewriter.getRevealedChars();
        List<String> tipLines = this.typewriter.getLines();
        float cursorX = -1.0F;
        float cursorY = tipY;

        for (int line = 0; line < tipLines.size() && line < maxLines; line++) {
            String text = tipLines.get(line);
            int lineStart = this.typewriter.getLineStart(line);
            int lineEnd = this.typewriter.getLineEnd(line);

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

        if (!isInside(mouseX, mouseY)) {
            return;
        }

        if (!this.typewriter.isComplete()) {
            // The first click completes the text.
            this.typewriter.complete();
            return;
        }

        this.typewriter.switchText();
    }
}
