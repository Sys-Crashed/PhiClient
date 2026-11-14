package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.component.capability.PhiInteractive;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiIconFeedback;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;

/**
 * A control block that reacts to the pointer
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The interactive half of the two kinds of control block: the hover state is tracked with the
 * unified machine of the interface and the pointer is answered with a horizontal sheen that sweeps
 * over the block (the band follows the slant of the edges). The icon runs the icon feedback when the
 * block is clicked - the signal icon turns into a check mark, the crosshair into a check mark, the
 * sun blends into the moon.</p>
 */
public class PhiInteractiveControlBlock extends PhiControlBlock implements PhiInteractive {

    /** The icon feedback of the block (primary icon, check mark and the theme blend). */
    private final PhiIconFeedback iconFeedback;

    /** The horizontal sheen that sweeps over the block while the pointer is on it. */
    private final PhiSheen sheen = new PhiSheen();

    /** Whether the icon follows the theme (the sun blends into the moon). */
    private final boolean themeIcon;

    private Action action;

    /**
     * @param x            the left x of the bounding box.
     * @param y            the top y of the bounding box.
     * @param width        the width of the bounding box.
     * @param height       the height of the bounding box.
     * @param uiScale      the design scale factor of the interface.
     * @param icon         the icon that is normally shown.
     * @param feedbackIcon the icon the primary icon turns into (a check mark, or null).
     * @param themeIcon    whether the icon follows the theme (the sun blends into the moon).
     */
    public PhiInteractiveControlBlock(float x, float y, float width, float height, float uiScale,
                                      PhiIcon icon, PhiIcon feedbackIcon, boolean themeIcon) {
        super(x, y, width, height, uiScale);

        this.iconFeedback = new PhiIconFeedback(icon, feedbackIcon, themeIcon);
        this.themeIcon = themeIcon;
    }

    /**
     * Sets the action that runs when the block is clicked.
     *
     * @param action the new action.
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Runs the icon feedback of the block (the icon turns into the check mark and back).
     */
    public void triggerFeedback() {
        this.iconFeedback.trigger();
    }

    @Override
    public boolean acceptsClick() {
        return true;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);
        this.sheen.update(isHovered());

        // The theme icon follows the current theme (sun in the light theme, moon in the dark one).
        if (this.themeIcon) {
            this.iconFeedback.setStateTarget(PhiTheme.isLight() ? 0.0F : 1.0F);
        }

        drawSurface(nvg);

        // The horizontal sheen sweeps over the block while the pointer is on it.
        this.sheen.drawSkew(nvg, this.x, this.y, this.width, this.height,
                PhiMetrics.CORNER_RADIUS * this.uiScale, getHoverProgress(), PhiTheme.level4());

        int iconColor = isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary();

        drawGroup(nvg, getPressScale(), iconColor, PhiTheme.textPrimary());
    }

    @Override
    protected void drawIcon(NanoVGManager nvg, float centerX, float centerY, float iconSize,
                            int iconColor) {
        this.iconFeedback.draw(nvg, centerX, centerY, iconSize, iconColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.action == null || mouseButton != 0) {
            return;
        }

        if (!isInside(mouseX, mouseY)) {
            return;
        }

        beginPress();
        this.action.onClicked();
    }
}
