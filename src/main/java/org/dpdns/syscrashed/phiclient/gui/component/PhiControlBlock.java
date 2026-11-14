package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiIconFeedback;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;

/**
 * A control block of the home page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>A control block is a small element of the global shape family: a parallelogram with the same 75 /
 * 105 degree angles as every card of the interface. It shows an icon in the upper half and the text of
 * the control below it - the whole group sits in the middle of the block.</p>
 *
 * <p>A block is either <b>interactive</b> (it reacts to the pointer and runs an action) or it only
 * shows a value. Both kinds use the same shape and the same rules, so a row of them stays even.</p>
 *
 * <p><b>Feedback</b>: every block owns two icons. The first one is the icon of the state, the second
 * one the icon of the feedback (a check mark after a successful copy, the moon of the dark theme).
 * The transition uses the Cupertino replace preset: the resting icon moves out, the feedback icon
 * enters with a damped spring and a draw-on stroke, then the resting icon springs back. A block
 * whose icon follows the theme rotates between the sun and the moon. The hover feedback is a sheen
 * that sweeps along the slanted axis of the block.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the block.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - runs the action of an interactive block.</li>
 *   <li>{@link #setAction(Action)} / {@link #setLabel(String)} / {@link #setValueProvider(ValueProvider)}
 *       - configuration.</li>
 * </ul>
 */
public class PhiControlBlock extends PhiComponent {

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

    private final float uiScale;

    /** The replace transition of the resting icon and the feedback icon. */
    private final PhiIconFeedback iconFeedback;

    /** The sheen that sweeps along the slanted axis while the block is hovered. */
    private final PhiSheen sheen = new PhiSheen();

    /** Whether the block reacts to the pointer. */
    private final boolean interactive;

    /** Whether the icon of the block follows the theme (sun in the light theme, moon in the dark one). */
    private final boolean themeIcon;

    private String label;

    private Action action;

    private ValueProvider valueProvider;

    /**
     * @param x           the left x of the bounding box.
     * @param y           the top y of the bounding box.
     * @param width       the width of the bounding box.
     * @param height      the height of the bounding box.
     * @param uiScale     the design scale factor of the interface.
     * @param icon        the icon of the resting state.
     * @param feedbackIcon the icon of the feedback (may be null).
     * @param interactive whether the block reacts to the pointer.
     * @param themeIcon   whether the icon follows the theme.
     */
    public PhiControlBlock(float x, float y, float width, float height, float uiScale,
                           PhiIcon icon, PhiIcon feedbackIcon, boolean interactive, boolean themeIcon) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.iconFeedback = new PhiIconFeedback(icon, feedbackIcon, themeIcon);
        this.interactive = interactive;
        this.themeIcon = themeIcon;
    }

    /**
     * Sets the action of an interactive block.
     *
     * @param action the action (null is ignored).
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Sets the text that is shown under the icon.
     *
     * @param label the new text.
     */
    public void setLabel(String label) {
        this.label = label == null ? "" : label;
    }

    /**
     * @return the text that is shown under the icon.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Sets the callback that provides the text of the block. The callback is asked for the text in
     * every frame, so the values of the game (the time, the weather) stay up to date.
     *
     * @param valueProvider the callback (null removes it).
     */
    public void setValueProvider(ValueProvider valueProvider) {
        this.valueProvider = valueProvider;
    }

    /**
     * Shows the feedback icon for a moment.
     */
    public void triggerFeedback() {
        this.iconFeedback.trigger();
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        if (this.interactive) {
            updateHover(mouseX, mouseY);
            this.sheen.update(isHovered());
        }

        // The block shows the value of the game when it has a provider for it.
        if (this.valueProvider != null) {
            this.label = this.valueProvider.getValue();
        }

        // The icon of the theme block follows the palette, the other blocks show their feedback.
        if (this.themeIcon) {
            this.iconFeedback.setStateTarget(PhiTheme.isLight() ? 0.0F : 1.0F);
        }

        nvg.drawSkewRect(this.x, this.y, this.width, this.height,
                PhiMetrics.CORNER_RADIUS * this.uiScale, PhiTheme.level3());

        if (this.interactive) {
            this.sheen.drawSkew(nvg, this.x, this.y, this.width, this.height,
                    PhiMetrics.CORNER_RADIUS * this.uiScale, getHoverProgress(), PhiTheme.level4());
        }

        float padding = PhiMetrics.CONTROL_BLOCK_PADDING * this.uiScale;
        float iconSize = PhiMetrics.CONTROL_BLOCK_ICON_SIZE * this.uiScale;
        float labelSize = PhiMetrics.FONT_BODY * this.uiScale;
        float iconGap = PhiMetrics.CONTROL_BLOCK_ICON_GAP * this.uiScale;

        // The whole group (icon and text) sits in the middle of the block. The middle of a
        // parallelogram is not the middle of its bounding box - the top edge is shifted to the right.
        float groupHeight = iconSize + iconGap + labelSize;
        float centerX = this.x + this.width * 0.5F + PhiSkew.offset(this.height) * 0.5F;
        float iconCenterY = this.y + (this.height - groupHeight) * 0.5F + iconSize * 0.5F;

        // A press makes the group grow a little.
        float scale = getPressScale();
        float pressY = this.y + this.height * 0.5F;

        nvg.save();
        nvg.translate(centerX, pressY);
        nvg.scale(scale, scale);
        nvg.translate(-centerX, -pressY);

        int iconColor = this.interactive && isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary();

        this.iconFeedback.draw(nvg, centerX, iconCenterY, iconSize, iconColor);

        float labelY = iconCenterY + iconSize * 0.5F + iconGap;
        float maxWidth = Math.max(0.0F, this.width - padding * 2.0F);
        int labelColor = this.interactive ? PhiTheme.textPrimary() : PhiTheme.textSecondary();

        nvg.drawTextCentered(nvg.limitText(this.label, labelSize, Fonts.REGULAR, maxWidth),
                centerX, labelY, labelColor, labelSize, Fonts.REGULAR);

        nvg.restore();
    }

    @Override
    protected boolean isInside(float pointX, float pointY) {
        // The block is a parallelogram - the bounding box would report hits outside of the shape.
        return PhiSkew.contains(pointX, pointY, this.x, this.y, this.width, this.height);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.interactive || this.action == null || mouseButton != 0) {
            return;
        }

        if (!isInside(mouseX, mouseY)) {
            return;
        }

        beginPress();
        this.action.onClicked();
    }
}
