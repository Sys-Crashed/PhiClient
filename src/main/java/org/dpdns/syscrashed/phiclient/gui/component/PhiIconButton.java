package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A button that shows a single vector icon (package
 * {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The button uses the global shape of the interface (a rounded parallelogram with 75 / 105 degree
 * angles) and has no outline: the background level expresses the hover state, the icon color the
 * enabled state.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the button.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - triggers the action when the button is hit.</li>
 *   <li>{@link #setIcon(PhiIcon)} / {@link #setAction(Action)} - configuration.</li>
 * </ul>
 */
public class PhiIconButton extends PhiComponent {

    /** Callback that is invoked when the button is clicked. */
    public interface Action {
        /**
         * Called when the user clicks the button.
         */
        void onClicked();
    }

    private PhiIcon icon;

    private Action action;

    private boolean highlighted;

    private final float uiScale;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     * @param icon    the icon of the button.
     */
    public PhiIconButton(float x, float y, float width, float height, float uiScale, PhiIcon icon) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.icon = icon;
    }

    /**
     * @param icon the icon of the button.
     */
    public void setIcon(PhiIcon icon) {
        this.icon = icon;
    }

    /**
     * @param action the callback that is invoked on a click.
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Marks the button as highlighted (for example the currently playing state).
     *
     * @param highlighted the new state.
     */
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        int background = isHovered() ? PhiTheme.level4() : PhiTheme.level3();
        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, background);

        if (this.icon == null) {
            return;
        }

        // The icon is centered in the parallelogram, so it uses the centroid instead of the box centre.
        float offset = PhiSkew.offset(this.height);
        float centerX = this.x + this.width * 0.5F + offset * 0.5F;
        float centerY = this.y + this.height * 0.5F;
        int color = this.highlighted ? PhiTheme.accent() : PhiTheme.textPrimary();

        this.icon.draw(nvg, centerX, centerY, this.height * 0.55F, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || this.action == null) {
            return;
        }

        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return;
        }

        this.action.onClicked();
    }
}
