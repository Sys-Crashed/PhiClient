package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A rectangular chip of the tip card (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>A chip is the small element that carries a single piece of information or a single action. In
 * contrast to the cards of the hub, a chip is <b>not</b> a parallelogram: a row of chips would look
 * broken if every one of them leaned to the right, so they stay upright rectangles.</p>
 *
 * <p>A chip is either interactive (it reacts to the pointer and runs an action) or it only shows a
 * value. Both kinds use the same shape and the same text rules, so a row of them stays even.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the chip.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - runs the action of an interactive chip.</li>
 *   <li>{@link #setAction(Action)} - the action of the chip.</li>
 * </ul>
 */
public class PhiChip extends PhiComponent {

    /** An action that is run when an interactive chip is clicked. */
    public interface Action {

        /**
         * Runs the action of the chip.
         */
        void onClicked();
    }

    private final float uiScale;

    private String label;

    /** Whether the chip reacts to the pointer. */
    private final boolean interactive;

    private Action action;

    /**
     * @param x          the left x of the chip.
     * @param y          the top y of the chip.
     * @param width      the width of the chip.
     * @param height     the height of the chip.
     * @param uiScale    the design scale factor of the interface.
     * @param label      the text of the chip.
     * @param interactive whether the chip reacts to the pointer.
     */
    public PhiChip(float x, float y, float width, float height, float uiScale, String label, boolean interactive) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.label = label == null ? "" : label;
        this.interactive = interactive;
    }

    /**
     * Sets the action of the chip.
     *
     * @param action the action (null is ignored).
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * @return the text of the chip.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Replaces the text of the chip. The chips that show a value of the game use this, because those
     * values change while the interface is open.
     *
     * @param label the new text.
     */
    public void setLabel(String label) {
        this.label = label == null ? "" : label;
    }

    /**
     * @return true if the chip reacts to the pointer.
     */
    public boolean isInteractive() {
        return this.interactive;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        if (this.interactive) {
            updateHover(mouseX, mouseY);
        }

        int background;

        if (this.interactive) {
            background = isHovered() ? PhiTheme.level4() : PhiTheme.level3();
        } else {
            background = PhiTheme.level2();
        }

        // The chip is an upright rectangle - a row of leaning chips would not look even.
        nvg.drawRoundedRect(this.x, this.y, this.width, this.height,
                PhiMetrics.CHIP_RADIUS * this.uiScale, background);

        float textSize = PhiMetrics.FONT_BODY * this.uiScale;
        float padding = PhiMetrics.CHIP_PADDING * this.uiScale;
        int color = this.interactive
                ? (isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary())
                : PhiTheme.textSecondary();

        nvg.drawTextCentered(
                nvg.limitText(this.label, textSize, Fonts.REGULAR, this.width - padding * 2.0F),
                this.x + this.width * 0.5F, this.y + (this.height - textSize) * 0.5F,
                color, textSize, Fonts.REGULAR);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.interactive || this.action == null || mouseButton != 0) {
            return;
        }

        if (!isInside(mouseX, mouseY)) {
            return;
        }

        this.action.onClicked();
    }
}