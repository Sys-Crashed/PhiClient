package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
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
 * <p>The card shows one tip at a time and writes it out character by character - the text is always
 * complete after one second, no matter how long the tip is. A click completes the text immediately, a
 * second click switches to the next tip.</p>
 *
 * <p>Below the tip the card holds a row of quick access tools: a theme switch and the buttons that open
 * the other pages of the hub. The tools fill the card, so it does not look empty, and every one of them
 * is a real action - there is no placeholder in the card.</p>
 *
 * <p><b>Layout rule</b> (the same rule the Soar client uses): the card owns exactly one content column
 * (see {@link PhiCard}). The headline, the tip (wrapped over several lines), the divider and the tools
 * all start at the left edge of that column, the counter and the status line at its right edge. Because
 * the column is a rectangle, the tip can be wrapped over several lines without any of them drifting
 * away from the others.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the card.</li>
 *   <li>{@link #setTools(List)} - the quick access row of the card.</li>
 * </ul>
 */
public class PhiTipCard extends PhiComponent {

    /** The time the tip needs to be written out completely. */
    private static final float REVEAL_DURATION = 1000.0F;

    /** The horizontal distance between two tool buttons. */
    private static final float TOOL_GAP = 9.0F;

    /** A tool of the quick access row. */
    public static class Tool {

        private final PhiIcon icon;

        private final String label;

        private final PhiIconButton.Action action;

        /**
         * @param icon   the icon of the tool.
         * @param label  the label below the icon.
         * @param action the action of the tool.
         */
        public Tool(PhiIcon icon, String label, PhiIconButton.Action action) {
            this.icon = icon;
            this.label = label;
            this.action = action;
        }

        /**
         * @return the icon of the tool.
         */
        public PhiIcon getIcon() {
            return this.icon;
        }

        /**
         * @return the label of the tool.
         */
        public String getLabel() {
            return this.label;
        }

        /**
         * @return the action of the tool.
         */
        public PhiIconButton.Action getAction() {
            return this.action;
        }
    }

    private final float uiScale;

    private final List<String> tips = PhiTips.getTips();

    private final List<Tool> tools = new ArrayList<Tool>();

    private final List<PhiIconButton> toolButtons = new ArrayList<PhiIconButton>();

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

    private int frameCount;

    private float frameTimer;

    private int fps;

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

    /**
     * Replaces the tools of the quick access row.
     *
     * @param tools the new tools.
     */
    public void setTools(List<Tool> tools) {
        this.tools.clear();
        this.tools.addAll(tools);

        buildToolButtons();
    }

    /**
     * Creates the buttons of the quick access row and places them at the bottom of the card.
     */
    private void buildToolButtons() {
        this.toolButtons.clear();

        if (this.tools.isEmpty()) {
            return;
        }

        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float buttonSize = PhiMetrics.ICON_BUTTON_SIZE * this.uiScale;

        for (int i = 0; i < this.tools.size(); i++) {
            Tool tool = this.tools.get(i);

            PhiIconButton button = new PhiIconButton(0.0F, 0.0F, buttonSize, buttonSize, this.uiScale,
                    tool.getIcon());

            button.setAction(tool.getAction());
            this.toolButtons.add(button);
        }

        // The row is placed at the left edge of the content column of the card.
        layoutTools(padding, buttonSize);
    }

    /**
     * Places the buttons of the quick access row inside the content column of the card.
     *
     * @param padding    the inset between the card and its content.
     * @param buttonSize the edge length of a button.
     */
    private void layoutTools(float padding, float buttonSize) {
        if (this.toolButtons.isEmpty()) {
            return;
        }

        float labelSize = PhiMetrics.FONT_BODY * this.uiScale * 0.85F;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);

        // The row sits at the bottom of the card, the labels are below the buttons.
        float rowBottom = this.y + this.height - padding * 0.8F;
        float rowY = rowBottom - (buttonSize + labelSize * 1.35F);

        for (int i = 0; i < this.toolButtons.size(); i++) {
            PhiIconButton button = this.toolButtons.get(i);

            button.setX(contentLeft + i * (buttonSize + TOOL_GAP * this.uiScale));
            button.setY(rowY);
        }
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        // The frame rate of the interface (shown in the status line of the card).
        this.frameCount++;
        this.frameTimer += deltaMs;

        if (this.frameTimer >= 500.0F) {
            this.fps = Math.round(this.frameCount * 1000.0F / this.frameTimer);
            this.frameCount = 0;
            this.frameTimer = 0.0F;
        }

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
        float labelSize = bodySize * 0.85F;
        float buttonSize = PhiMetrics.ICON_BUTTON_SIZE * this.uiScale;
        float lineHeight = bodySize * 1.35F;

        // The one content column of the card.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = contentRight - contentLeft;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        // The bottom block of the card: the tools with their labels and the divider above them.
        float toolRowHeight = buttonSize + labelSize * 1.35F;
        float rowBottom = this.y + this.height - padding * 0.8F;
        float rowY = rowBottom - toolRowHeight;
        float dividerY = rowY - padding * 0.6F;
        float statusY = dividerY - bodySize * 1.05F;

        // Title and counter.
        float titleY = this.y + padding * 0.9F;

        nvg.drawText("Tip", contentLeft, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        String counter = (this.index + 1) + " / " + this.tips.size();

        nvg.drawTextAligned(counter, contentRight, titleY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // The tip itself, written out character by character.
        float tipY = titleY + titleSize * 1.5F;
        int maxLines = Math.max(1, (int) ((statusY - bodySize * 0.5F - tipY) / lineHeight));

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

        // Status line on the right side, right above the divider.
        String status = "PhiClient " + PhiClient.VERSION + "  -  " + this.fps + " FPS  -  "
                + (PhiTheme.isLight() ? "Light" : "Dark");

        nvg.drawTextAligned(nvg.limitText(status, labelSize, Fonts.REGULAR, contentWidth), contentRight, statusY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), labelSize, Fonts.REGULAR);

        // Divider above the quick access row.
        nvg.drawRect(contentLeft, dividerY, contentWidth, Math.max(1.0F, this.uiScale), PhiTheme.level2());

        // The tools of the quick access row with their labels.
        layoutTools(padding, buttonSize);

        float labelY = rowY + buttonSize + labelSize * 0.2F;

        for (int i = 0; i < this.toolButtons.size(); i++) {
            PhiIconButton button = this.toolButtons.get(i);

            button.draw(nvg, mouseX, mouseY, partialTicks);

            // The label is centered below the button - the icon of a button sits in the centre of its
            // parallelogram, so the label uses the same centre.
            float centerX = button.getX() + button.getWidth() * 0.5F + PhiSkew.offset(button.getHeight()) * 0.5F;

            nvg.drawTextCentered(nvg.limitText(this.tools.get(i).getLabel(), labelSize, Fonts.REGULAR,
                    button.getWidth() + TOOL_GAP * this.uiScale), centerX, labelY,
                    PhiTheme.textSecondary(), labelSize, Fonts.REGULAR);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        // The tools have the priority - they are inside the card as well.
        for (PhiIconButton button : this.toolButtons) {
            button.mouseClicked(mouseX, mouseY, mouseButton);
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
}
