package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.input.PhiImeBridge;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.lwjgl.input.Keyboard;

/**
 * A text input field of the PhiClient interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The field follows the global parallelogram shape and carries no outline: the focus state is
 * expressed by a brighter background and accent colored text, the resting state by the default level
 * and the placeholder in the secondary text color. Used as the search box of the music page and as
 * the name input of the playlist overlay.</p>
 *
 * <p>Editing is append-only at the end of the text (backspace deletes, printable characters append,
 * return or a click outside releases the focus). Every change is pushed to the
 * {@link ChangeListener}, so the owner can filter its list live.</p>
 */
public class PhiSearchField extends PhiComponent {

    /** Receives the new text after every edit. */
    public interface ChangeListener {

        /**
         * @param text the current text of the field.
         */
        void onChanged(String text);
    }

    private final float uiScale;

    private final StringBuilder text = new StringBuilder();

    private String placeholder = "";

    private ChangeListener listener;

    private boolean focused;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiSearchField(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);
        this.uiScale = uiScale;
    }

    /**
     * @param placeholder the text shown while the field is empty.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
    }

    /**
     * @param listener the change callback.
     */
    public void setChangeListener(ChangeListener listener) {
        this.listener = listener;
    }

    /**
     * @return the current text.
     */
    public String getText() {
        return this.text.toString();
    }

    /**
     * Replaces the text without notifying the listener (programmatic reset).
     *
     * @param value the new text.
     */
    public void setText(String value) {
        this.text.setLength(0);
        if (value != null) {
            this.text.append(value);
        }
    }

    /**
     * Releases the focus (used when an overlay closes).
     */
    public void blur() {
        this.focused = false;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean inside = isInside(mouseX, mouseY);
        if (mouseButton == 0) {
            this.focused = inside;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!this.focused) {
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            this.focused = false;
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (this.text.length() > 0) {
                this.text.deleteCharAt(this.text.length() - 1);
                notifyChanged();
            }
            return;
        }

        // Only printable characters (the CJK input arrives through the IME of the OS).
        if (typedChar >= 32 && typedChar != 127) {
            this.text.append(typedChar);
            notifyChanged();
        }
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        int background = this.focused ? PhiTheme.level4() : PhiTheme.level3();

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, background);

        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float padding = bodySize * 0.9F;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        // The search icon sits at the right end of the field.
        float iconSize = this.height * 0.52F;
        float iconX = contentRight - iconSize * 0.5F;
        float textWidth = Math.max(0.0F, contentRight - contentLeft - iconSize - padding * 0.5F);
        float textY = this.y + (this.height - bodySize) * 0.5F;

        if (this.text.length() == 0 && !this.focused) {
            nvg.drawText(nvg.limitText(this.placeholder, bodySize, Fonts.REGULAR, textWidth),
                    contentLeft, textY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
            PhiImeBridge.setCaretGuiPosition(-1, -1);
        } else {
            String shown = nvg.limitText(this.text.toString(), bodySize, Fonts.REGULAR, textWidth);
            int color = this.focused ? PhiTheme.accent() : PhiTheme.textPrimary();
            nvg.drawText(shown, contentLeft, textY, color, bodySize, Fonts.REGULAR);

            float caretX = contentLeft + nvg.textWidth(shown, bodySize, Fonts.REGULAR) + 1.0F;

            // The caret blinks at the end of the text while the field is focused.
            if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                nvg.drawRect(caretX, textY, Math.max(1.0F, this.uiScale), bodySize, PhiTheme.accent());
            }

            // Report the caret so the IME candidate window follows it; cleared when the field blurs.
            if (this.focused) {
                PhiImeBridge.setCaretGuiPosition((int) caretX, (int) (textY + bodySize));
            } else {
                PhiImeBridge.setCaretGuiPosition(-1, -1);
            }
        }

        PhiIcons.SEARCH.draw(nvg, iconX, this.y + this.height * 0.5F, iconSize,
                this.focused ? PhiTheme.accent() : PhiTheme.textSecondary());
    }

    /**
     * Pushes the current text to the listener.
     */
    private void notifyChanged() {
        if (this.listener != null) {
            this.listener.onChanged(getText());
        }
    }
}
