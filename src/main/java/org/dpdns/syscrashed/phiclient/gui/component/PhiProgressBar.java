package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.component.shape.PhiRoundedSquareShape;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.lwjgl.input.Mouse;

/**
 * A progress bar of the PhiClient interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The bar is an upright rectangle: it lies below the buttons of the music area, and a leaning bar
 * next to upright buttons would not look even. The played part is drawn in the accent color, the
 * remaining part in the hover level - there is no outline and no visible scrollbar.</p>
 *
 * <p>The bar is interactive: a click or a drag seeks the track. While the user drags, the bar shows
 * the dragged position instead of the playback position, and the {@link SeekListener} receives the
 * ratio - the music engine debounces the actual stream restart, so dragging never spawns a decoder
 * process per frame. The hit test is the axis aligned bounding box (the music controls are upright).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the bar and advances an active drag.</li>
 *   <li>{@link #getProgress()} / {@link #setProgress(float)} - the progress in the range {@code [0, 1]}.</li>
 *   <li>{@link #setSeekListener(SeekListener)} - the seek callback (null makes the bar read-only).</li>
 *   <li>{@link #mouseClicked(int, int, int)} / {@link #mouseReleased(int, int, int)} - the drag lifecycle.</li>
 * </ul>
 */
public class PhiProgressBar extends PhiComponent {

    /** Receives the seek ratio while the user clicks or drags the bar. */
    public interface SeekListener {

        /**
         * @param ratio the requested position in the range {@code [0, 1]}.
         */
        void onSeek(float ratio);
    }

    private float progress;

    private final float uiScale;

    private SeekListener seekListener;

    /** True while the left button is held on the bar. */
    private boolean dragging;

    /** The ratio the user is currently dragging to. */
    private float dragRatio;

    /** Smoothly keeps the drag feedback visible after the pointer leaves the bar. */
    private float dragAmount;

    private long lastFrame;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiProgressBar(float x, float y, float width, float height, float uiScale) {
        // Upright rectangle: the hit test is the axis aligned box, like the other music controls.
        super(x, y, width, height, PhiRoundedSquareShape.INSTANCE);

        this.uiScale = uiScale;
        this.lastFrame = System.currentTimeMillis();
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

    /**
     * @param listener the seek callback (null makes the bar read-only).
     */
    public void setSeekListener(SeekListener listener) {
        this.seekListener = listener;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || this.seekListener == null || this.width <= 0.0F) {
            return;
        }

        if (!isInside(mouseX, mouseY)) {
            return;
        }

        this.dragging = true;
        seekTo(mouseX);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            this.dragging = false;
        }
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        // A drag continues outside the bar: follow the pointer while the button is held.
        if (this.dragging) {
            if (Mouse.isButtonDown(0)) {
                seekTo(mouseX);
            } else {
                this.dragging = false;
            }
        }

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        this.dragAmount = this.dragging
                ? Math.min(1.0F, this.dragAmount + deltaMs / 90.0F)
                : Math.max(0.0F, this.dragAmount - deltaMs / 180.0F);

        float feedback = Math.max(getHoverProgress(), this.dragAmount);
        float radius = Math.min(this.height * 0.5F, PhiMetrics.CORNER_RADIUS * this.uiScale);
        float shown = this.dragging ? this.dragRatio : this.progress;

        // Remaining part.
        nvg.drawRoundedRect(this.x, this.y, this.width, this.height, radius,
                PhiTheme.mix(PhiTheme.level4(), PhiTheme.level5(), feedback * 0.55F));

        // Played part.
        if (shown > 0.0F) {
            float played = this.width * shown;
            nvg.drawRoundedRect(this.x, this.y, played, this.height, radius, PhiTheme.accent());
        }

        if (feedback > 0.001F) {
            float handleWidth = Math.max(2.0F * this.uiScale, this.height * 0.55F * feedback);
            float handleHeight = this.height * (1.0F + feedback);
            float handleX = Math.max(this.x + handleWidth * 0.5F,
                    Math.min(this.x + this.width - handleWidth * 0.5F, this.x + this.width * shown));
            float centerY = this.y + this.height * 0.5F;
            nvg.drawRoundedRect(handleX - handleWidth * 0.5F, centerY - handleHeight * 0.5F,
                    handleWidth, handleHeight, handleWidth * 0.5F,
                    PhiTheme.withAlpha(PhiTheme.accent(), feedback));
        }
    }

    /**
     * Converts a pointer x into a seek ratio and notifies the listener.
     *
     * @param mouseX the pointer x.
     */
    private void seekTo(int mouseX) {
        if (this.seekListener == null || this.width <= 0.0F) {
            return;
        }

        this.dragRatio = Math.max(0.0F, Math.min(1.0F, (mouseX - this.x) / this.width));
        this.seekListener.onSeek(this.dragRatio);
    }
}
