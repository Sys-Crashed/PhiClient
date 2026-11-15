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

    /** Receives the text when the editing ends (return key or loss of focus). */
    public interface SubmitListener {

        /**
         * @param text the text of the field at the moment the editing ended.
         */
        void onSubmit(String text);
    }

    private final float uiScale;

    private final StringBuilder text = new StringBuilder();

    private String placeholder = "";

    private ChangeListener listener;

    private SubmitListener submitListener;

    private boolean focused;

    private boolean showIcon = true;

    private float focusAmount;

    private long lastFrame = System.currentTimeMillis();

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
     * Shows or hides the search icon. A field that is used as a plain name input has no icon.
     *
     * @param showIcon whether the search icon is drawn.
     */
    public void setShowIcon(boolean showIcon) {
        this.showIcon = showIcon;
    }

    /**
     * @param listener the change callback.
     */
    public void setChangeListener(ChangeListener listener) {
        this.listener = listener;
    }

    /**
     * @param listener the callback for the finished editing.
     */
    public void setSubmitListener(SubmitListener listener) {
        this.submitListener = listener;
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
            boolean wasFocused = this.focused;
            this.focused = inside;
            if (wasFocused && !inside) {
                // The editing ended by clicking somewhere else: the text is confirmed.
                notifySubmit();
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!this.focused) {
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            this.focused = false;
            notifySubmit();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
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

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        this.focusAmount = this.focused
                ? Math.min(1.0F, this.focusAmount + deltaMs / 140.0F)
                : Math.max(0.0F, this.focusAmount - deltaMs / 200.0F);

        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        float feedback = Math.max(this.focusAmount, getHoverProgress());
        int background = PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), feedback);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, background);

        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float padding = bodySize * 0.9F;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        // The search icon sits at the right end of the field (a plain name field has none).
        float iconSize = this.height * 0.52F;
        float iconX = contentRight - iconSize * 0.5F;
        float iconSpace = this.showIcon ? iconSize + padding * 0.5F : 0.0F;
        float textWidth = Math.max(0.0F, contentRight - contentLeft - iconSpace);
        float textY = this.y + (this.height - bodySize) * 0.5F;

        if (this.text.length() == 0) {
            // The hint stays while the field is empty - focused or not - so the user always knows
            // what the field is for. The caret is drawn at the start while the field is focused.
            nvg.drawText(nvg.limitText(this.placeholder, bodySize, Fonts.REGULAR, textWidth),
                    contentLeft, textY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

            if (this.focused) {
                float caretX = contentLeft + 1.0F;
                float pulse = 0.5F + 0.5F * (float) Math.sin((now % 1000L) * Math.PI * 2.0D / 1000.0D);
                if (this.focusAmount > 0.001F) {
                    nvg.drawRect(caretX, textY, Math.max(1.0F, this.uiScale), bodySize,
                            PhiTheme.withAlpha(PhiTheme.accent(), this.focusAmount * (0.35F + 0.65F * pulse)));
                }
                PhiImeBridge.setCaretGuiPosition((int) caretX, (int) (textY + bodySize));
            } else {
                PhiImeBridge.setCaretGuiPosition(-1, -1);
            }
        } else {
            String shown = nvg.limitText(this.text.toString(), bodySize, Fonts.REGULAR, textWidth);
            int color = PhiTheme.mix(PhiTheme.textPrimary(), PhiTheme.accent(), this.focusAmount);
            nvg.drawText(shown, contentLeft, textY, color, bodySize, Fonts.REGULAR);

            float caretX = contentLeft + nvg.textWidth(shown, bodySize, Fonts.REGULAR) + 1.0F;

            // A smooth caret pulse also fades out with the focus instead of blinking abruptly.
            float pulse = 0.5F + 0.5F * (float) Math.sin((now % 1000L) * Math.PI * 2.0D / 1000.0D);
            if (this.focusAmount > 0.001F) {
                nvg.drawRect(caretX, textY, Math.max(1.0F, this.uiScale), bodySize,
                        PhiTheme.withAlpha(PhiTheme.accent(), this.focusAmount * (0.35F + 0.65F * pulse)));
            }

            // Report the caret so the IME candidate window follows it; cleared when the field blurs.
            if (this.focused) {
                PhiImeBridge.setCaretGuiPosition((int) caretX, (int) (textY + bodySize));
            } else {
                PhiImeBridge.setCaretGuiPosition(-1, -1);
            }
        }

        if (this.showIcon) {
            PhiIcons.SEARCH.draw(nvg, iconX, this.y + this.height * 0.5F, iconSize,
                    PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.accent(), this.focusAmount));
        }
    }

    /**
     * Pushes the current text to the listener.
     */
    private void notifyChanged() {
        if (this.listener != null) {
            this.listener.onChanged(getText());
        }
    }

    /**
     * Pushes the finished text to the submit listener.
     */
    private void notifySubmit() {
        if (this.submitListener != null) {
            this.submitListener.onSubmit(getText());
        }
    }
}
