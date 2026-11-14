package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.component.text.PhiStaticText;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * Abstract base of a control block of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>A control block is a small element of the global shape family: a parallelogram with the same 75 /
 * 105 degree angles as every card of the interface. It shows an icon in the upper half and the text of
 * the control below it - the whole group sits in the middle of the block.</p>
 *
 * <p>The block has exactly two kinds, which are two classes and not a flag:</p>
 * <ul>
 *   <li>{@link PhiInteractiveControlBlock} - reacts to the pointer and runs an action (copy the
 *       address, copy the position, switch the theme); it owns the icon feedback and the hover sheen;</li>
 *   <li>{@link PhiReadonlyControlBlock} - only shows a value (the time, the weather, the dimension);
 *       it never enters the hover feedback.</li>
 * </ul>
 *
 * <p>Both kinds use the same shape and the same layout, so a row of them stays even.</p>
 */
public abstract class PhiControlBlock extends PhiComponent {

    /** Callback that is invoked when an interactive block is clicked. */
    public interface Action {

        /**
         * Runs the action of the block.
         */
        void onClicked();
    }

    /** Callback that provides the text of a block whose value changes (the time, the weather). */
    public interface ValueProvider {

        /**
         * @return the text that is shown under the icon.
         */
        String getValue();
    }

    /** The design scale factor of the interface. */
    protected final float uiScale;

    /** The text that is shown under the icon (shortened to the width of the block). */
    private final PhiStaticText labelText = new PhiStaticText();

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    protected PhiControlBlock(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
    }

    /**
     * Sets the text that is shown under the icon.
     *
     * @param label the new text.
     */
    public void setLabel(String label) {
        this.labelText.setContent(label);
    }

    /**
     * @return the text that is shown under the icon.
     */
    public String getLabel() {
        return this.labelText.getContent();
    }

    /**
     * @return the horizontal centre of the block - the centroid of the parallelogram, which sits
     *         right of the middle of the bounding box.
     */
    protected final float centerX() {
        return getShape().iconCenterX(this.x, this.width, this.height);
    }

    /**
     * Draws the base surface of the block.
     *
     * @param nvg the NanoVG drawing context.
     */
    protected final void drawSurface(NanoVGManager nvg) {
        nvg.drawSkewRect(this.x, this.y, this.width, this.height,
                PhiMetrics.CORNER_RADIUS * this.uiScale, PhiTheme.level3());
    }

    /**
     * Draws the icon and the label as one group in the middle of the block. A press makes the group
     * grow a little.
     *
     * @param nvg        the NanoVG drawing context.
     * @param scale      the scale of the press feedback (1 = original size).
     * @param iconColor  the color of the icon.
     * @param labelColor the color of the label.
     */
    protected final void drawGroup(NanoVGManager nvg, float scale, int iconColor, int labelColor) {
        float padding = PhiMetrics.CONTROL_BLOCK_PADDING * this.uiScale;
        float iconSize = PhiMetrics.CONTROL_BLOCK_ICON_SIZE * this.uiScale;
        float labelSize = PhiMetrics.FONT_BODY * this.uiScale;
        float iconGap = PhiMetrics.CONTROL_BLOCK_ICON_GAP * this.uiScale;

        // The whole group (icon and text) sits in the middle of the block.
        float groupHeight = iconSize + iconGap + labelSize;
        float centerX = centerX();
        float iconCenterY = this.y + (this.height - groupHeight) * 0.5F + iconSize * 0.5F;
        float pressY = this.y + this.height * 0.5F;

        nvg.save();
        nvg.translate(centerX, pressY);
        nvg.scale(scale, scale);
        nvg.translate(-centerX, -pressY);

        drawIcon(nvg, centerX, iconCenterY, iconSize, iconColor);

        float labelY = iconCenterY + iconSize * 0.5F + iconGap;
        float maxWidth = Math.max(0.0F, this.width - padding * 2.0F);

        nvg.drawTextCentered(this.labelText.getLimited(nvg, labelSize, Fonts.REGULAR, maxWidth),
                centerX, labelY, labelColor, labelSize, Fonts.REGULAR);

        nvg.restore();
    }

    /**
     * Draws the icon of the block at its centre.
     *
     * @param nvg       the NanoVG drawing context.
     * @param centerX   the horizontal centre of the icon.
     * @param centerY   the vertical centre of the icon.
     * @param iconSize  the edge length of the icon.
     * @param iconColor the color of the icon.
     */
    protected abstract void drawIcon(NanoVGManager nvg, float centerX, float centerY,
                                     float iconSize, int iconColor);
}
