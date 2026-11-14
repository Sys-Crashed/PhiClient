package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A control block that only shows a value
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The read-only half of the two kinds of control block: it shows the icon and the value of a
 * provider (the time, the weather, the dimension) and never enters the hover feedback - the
 * interactive half is {@link PhiInteractiveControlBlock}.</p>
 */
public class PhiReadonlyControlBlock extends PhiControlBlock {

    private final PhiIcon icon;

    private ValueProvider valueProvider;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     * @param icon    the icon that is shown above the value.
     */
    public PhiReadonlyControlBlock(float x, float y, float width, float height, float uiScale,
                                   PhiIcon icon) {
        super(x, y, width, height, uiScale);

        this.icon = icon;
    }

    /**
     * Sets the provider of the value that is shown under the icon.
     *
     * @param valueProvider the new provider.
     */
    public void setValueProvider(ValueProvider valueProvider) {
        this.valueProvider = valueProvider;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        if (this.valueProvider != null) {
            setLabel(this.valueProvider.getValue());
        }

        drawSurface(nvg);

        // The value is passive: a constant group without the hover feedback.
        drawGroup(nvg, 1.0F, PhiTheme.textPrimary(), PhiTheme.textSecondary());
    }

    @Override
    protected void drawIcon(NanoVGManager nvg, float centerX, float centerY, float iconSize,
                            int iconColor) {
        this.icon.draw(nvg, centerX, centerY, iconSize, iconColor);
    }
}
