package org.dpdns.syscrashed.phiclient.gui.component.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;

/**
 * A text that is written out character by character
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.text}).
 *
 * <p>The text is one of the three text abilities of the interface. It writes a text of a list
 * character by character, always finishing after one second. The first completion reveals the whole
 * text immediately, the second one switches to another text - the next text is picked at random, so
 * the list never runs through in the same order twice. The lines are wrapped into the width of the
 * column and cached, so they do not jump while the text is written out.</p>
 */
public final class PhiTypewriterText {

    /** The time the text needs to be written out completely. */
    private static final float REVEAL_DURATION = 1000.0F;

    private final List<String> texts;

    /** The random source that picks the next text. */
    private final Random random = new Random();

    /** The wrapped lines of the text that is currently shown. */
    private final List<String> lines = new ArrayList<String>();

    /** The index of the last character of every wrapped line inside the text. */
    private final List<Integer> lineEnds = new ArrayList<Integer>();

    /** The text the wrapped lines belong to. */
    private int wrappedIndex = -1;

    /** The width the wrapped lines were built for. */
    private float wrappedWidth = -1.0F;

    private int index;

    /** The number of characters that are currently visible. */
    private float revealed;

    /** The number of characters that are written per second. */
    private float charactersPerSecond;

    /**
     * @param texts the texts that are shown one after another (at least one).
     */
    public PhiTypewriterText(List<String> texts) {
        this.texts = texts;
    }

    /**
     * Advances the writing by the given time.
     *
     * @param deltaMs the milliseconds since the previous frame.
     */
    public void advance(float deltaMs) {
        String text = getText();

        // The text is always written out completely after one second.
        if (this.charactersPerSecond <= 0.0F) {
            this.charactersPerSecond = text.length() * 1000.0F / REVEAL_DURATION;
        }

        this.revealed += deltaMs * this.charactersPerSecond / 1000.0F;

        if (this.revealed > text.length()) {
            this.revealed = text.length();
        }
    }

    /**
     * @return the text that is currently shown.
     */
    public String getText() {
        return this.texts.get(this.index);
    }

    /**
     * @return the number of characters that are currently visible.
     */
    public int getRevealedChars() {
        return (int) this.revealed;
    }

    /**
     * @return true if the text is written out completely.
     */
    public boolean isComplete() {
        return this.revealed >= getText().length();
    }

    /**
     * Writes the remaining characters out immediately.
     */
    public void complete() {
        this.revealed = getText().length();
    }

    /**
     * Switches to another text - the next text is picked at random, so the list never runs through in
     * the same order twice.
     */
    public void switchText() {
        int next = this.index;

        while (next == this.index && this.texts.size() > 1) {
            next = this.random.nextInt(this.texts.size());
        }

        this.index = next;
        this.revealed = 0.0F;
        this.charactersPerSecond = 0.0F;
    }

    /**
     * Wraps the text that is currently shown into lines that fit into the given width. The result is
     * cached, so the lines do not jump while the text is written out.
     *
     * @param nvg   the NanoVG drawing context.
     * @param width the width of a line.
     * @param size  the font size of the text.
     */
    public void wrap(NanoVGManager nvg, float width, float size) {
        if (this.wrappedIndex == this.index && this.wrappedWidth == width) {
            return;
        }

        this.wrappedIndex = this.index;
        this.wrappedWidth = width;
        this.lines.clear();
        this.lineEnds.clear();

        String text = this.texts.get(this.index);
        int position = 0;
        int lineStart = 0;
        StringBuilder line = new StringBuilder();

        while (position < text.length()) {
            int wordEnd = text.indexOf(' ', position);

            if (wordEnd < 0) {
                wordEnd = text.length();
            }

            String word = text.substring(position, wordEnd);
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (nvg.textWidth(candidate, size, Fonts.REGULAR) > width && line.length() > 0) {
                this.lines.add(line.toString());
                this.lineEnds.add(lineStart + line.length());

                lineStart = position;
                line = new StringBuilder(word);
            } else if (line.length() == 0) {
                // A single word that is longer than the column is shortened.
                line = new StringBuilder(nvg.limitText(word, size, Fonts.REGULAR, width));
            } else {
                line = new StringBuilder(candidate);
            }

            position = wordEnd + 1;
        }

        if (line.length() > 0) {
            this.lines.add(line.toString());
            this.lineEnds.add(lineStart + line.length());
        }
    }

    /**
     * @return the wrapped lines of the current text.
     */
    public List<String> getLines() {
        return this.lines;
    }

    /**
     * @param line the index of a wrapped line.
     * @return the index of the first character of the line inside the text.
     */
    public int getLineStart(int line) {
        return line == 0 ? 0 : this.lineEnds.get(line - 1) + 1;
    }

    /**
     * @param line the index of a wrapped line.
     * @return the index of the last character of the line inside the text.
     */
    public int getLineEnd(int line) {
        return this.lineEnds.get(line);
    }
}
