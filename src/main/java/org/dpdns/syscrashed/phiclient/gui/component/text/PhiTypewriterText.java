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

    /** The exclusive end index of every wrapped line inside the text. */
    private final List<Integer> lineEnds = new ArrayList<Integer>();

    /**
     * The exact start index of every wrapped line inside the text. Word-wrapped lines start right
     * after the space of the previous line; CJK lines (broken character by character) start exactly
     * where the previous line ended, so a derived {@code prevEnd + 1} would skip a character there.
     */
    private final List<Integer> lineStarts = new ArrayList<Integer>();

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
     * <p>English text is wrapped word by word (a single over-long word is shortened with an
     * ellipsis). CJK text (Chinese/Japanese - it has no spaces to break on) is wrapped character by
     * character, never splitting a surrogate pair.</p>
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
        this.lineStarts.clear();

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

            if (line.length() > 0) {
                String candidate = line + " " + word;

                if (nvg.textWidth(candidate, size, Fonts.REGULAR) <= width) {
                    line = new StringBuilder(candidate);
                    position = wordEnd + 1;
                    continue;
                }

                // The word does not fit on the open line: flush it and retry the word alone.
                this.lines.add(line.toString());
                this.lineEnds.add(lineStart + line.length());
                this.lineStarts.add(lineStart);

                lineStart = position;
                line = new StringBuilder();
            }

            // The line is empty here: the word starts its own line.
            if (nvg.textWidth(word, size, Fonts.REGULAR) <= width) {
                line = new StringBuilder(word);
                position = wordEnd + 1;
                continue;
            }

            if (isCjkWord(word)) {
                // CJK text has no spaces to break on: wrap character by character (by code point,
                // so a surrogate pair is never split). Every full chunk becomes its own line.
                StringBuilder chunk = new StringBuilder();
                int chunkStart = position;

                while (position < wordEnd) {
                    int codePoint = text.codePointAt(position);
                    int charCount = Character.charCount(codePoint);
                    String next = chunk.toString() + text.substring(position, position + charCount);

                    if (nvg.textWidth(next, size, Fonts.REGULAR) > width && chunk.length() > 0) {
                        this.lines.add(chunk.toString());
                        this.lineEnds.add(chunkStart + chunk.length());
                        this.lineStarts.add(chunkStart);

                        chunkStart = position;
                        chunk = new StringBuilder();
                    }

                    chunk.append(text, position, position + charCount);
                    position += charCount;
                }

                // The remainder of the word stays on the open line (position is now at wordEnd).
                line = new StringBuilder(chunk.toString());
                continue;
            }

            // A single Latin word that is longer than the column is shortened with an ellipsis.
            line = new StringBuilder(nvg.limitText(word, size, Fonts.REGULAR, width));
            position = wordEnd + 1;
        }

        if (line.length() > 0) {
            this.lines.add(line.toString());
            this.lineEnds.add(lineStart + line.length());
            this.lineStarts.add(lineStart);
        }
    }

    /**
     * @param word a word of the wrapped text.
     * @return true if the word contains CJK characters and should be wrapped character by character.
     */
    private static boolean isCjkWord(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            if (isCjk(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param c a character.
     * @return true for CJK radicals/symbols/unified ideographs, Hiragana, Katakana and fullwidth
     *         forms (the ranges the translations use).
     */
    private static boolean isCjk(char c) {
        return (c >= 0x2E80 && c <= 0x9FFF)   // radicals, CJK symbols, unified ideographs (+ ext A)
                || (c >= 0x3040 && c <= 0x30FF) // Hiragana + Katakana
                || (c >= 0xFF00 && c <= 0xFFEF); // fullwidth forms
    }

    /**
     * @return the wrapped lines of the current text.
     */
    public List<String> getLines() {
        return this.lines;
    }

    /**
     * @param line the index of a wrapped line.
     * @return the index of the first character of the line inside the text. The exact stored start
     *         is used instead of a derived {@code prevEnd + 1}, because CJK lines are broken
     *         character by character without a separating space.
     */
    public int getLineStart(int line) {
        if (line < 0 || line >= this.lineStarts.size()) {
            return 0;
        }

        return this.lineStarts.get(line);
    }

    /**
     * @param line the index of a wrapped line.
     * @return the index of the last character of the line inside the text.
     */
    public int getLineEnd(int line) {
        return this.lineEnds.get(line);
    }
}
