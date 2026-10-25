package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A progress bar of the PhiClient interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The bar is an upright rectangle: it lies below the buttons of the music area, and a leaning bar
 * next to upright buttons would not look even. The played part is drawn in the accent color, the
 * remaining part in the hover level - there is no outline and no visible scrollbar.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the bar.</li>
 *   <li>{@link #getProgress()} / {@link #setProgress(float)} - the progress in the range {@code [0, 1]}.</li>
 * </ul>
 */
public class PhiProgressBar extends PhiComponent {

    private float progress;

    private final float uiScale;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiProgressBar(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
    }

    /**
     * @return the progress of the bar in the range {@code [0, 1]}.
     */
    public float getProgress() {
        return this.progress;
    }

    /**
     * @param progress the new progress in the range {@code [0, 1]}.
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0.0F, Math.min(1.0F, progress));
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float radius = Math.min(this.height * 0.5F, PhiMetrics.CORNER_RADIUS * this.uiScale);

        // Remaining part.
        nvg.drawRoundedRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level4());

        // Played part.
        if (this.progress > 0.0F) {
            float played = this.width * this.progress;
            nvg.drawRoundedRect(this.x, this.y, played, this.height, radius, PhiTheme.accent());
        }
    }
}
