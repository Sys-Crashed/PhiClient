package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.component.shape.PhiRoundedSquareShape;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.lwjgl.input.Mouse;

/**
 * The volume slider of the music area (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>An upright rounded rectangle (the music controls never lean): a speaker icon at the left, the
 * track in the middle and the percentage at the right. Clicking or dragging the track changes the
 * volume; the hit test is the axis aligned bounding box. The value is an integer percentage in
 * {@code [0, 100]} and is pushed to the {@link ChangeListener} on every change - the music manager
 * persists it debounced.</p>
 */
public class PhiVolumeSlider extends PhiComponent {

    /** Receives the new volume in percent. */
    public interface ChangeListener {

        /**
         * @param percent the new volume in {@code [0, 100]}.
         */
        void onChanged(int percent);
    }

    private final float uiScale;

    private int volume = 100;

    private ChangeListener listener;

    private boolean dragging;

    /** Smoothly keeps the drag feedback visible after the pointer leaves the slider. */
    private float dragAmount;

    private long lastFrame;

    /** The track geometry of the last frame (shared by the drawing and the hit mapping). */
    private float trackX;

    private float trackWidth;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiVolumeSlider(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height, PhiRoundedSquareShape.INSTANCE);
        this.uiScale = uiScale;
        this.lastFrame = System.currentTimeMillis();
    }

    /**
     * @return the volume in percent {@code [0, 100]}.
     */
    public int getVolume() {
        return this.volume;
    }

    /**
     * Sets the volume without notifying the listener (external sync).
     *
     * @param percent the volume in percent.
     */
    public void setVolume(int percent) {
        this.volume = Math.max(0, Math.min(100, percent));
    }

    /**
     * @param listener the change callback.
     */
    public void setChangeListener(ChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isInside(mouseX, mouseY)) {
            return;
        }
        this.dragging = true;
        updateFromMouse(mouseX);
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

        if (this.dragging) {
            if (Mouse.isButtonDown(0)) {
                updateFromMouse(mouseX);
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

        float iconSize = this.height * 0.9F;
        float iconX = this.x + iconSize * 0.5F;
        float centerY = this.y + this.height * 0.5F;

        PhiIcons.VOLUME.draw(nvg, iconX, centerY, iconSize,
                this.volume == 0 ? PhiTheme.textSecondary() : PhiTheme.textPrimary());

        // Track between the icon and the percentage label.
        float labelSize = PhiMetrics.FONT_BODY * this.uiScale * 0.85F;
        String label = this.volume + "%";
        float labelWidth = nvg.textWidth("100%", labelSize, Fonts.REGULAR);
        float trackHeight = Math.max(3.0F, this.height * 0.30F);
        this.trackX = this.x + iconSize + this.height * 0.35F;
        float trackRight = this.x + this.width - labelWidth - this.height * 0.35F;
        this.trackWidth = Math.max(4.0F, trackRight - this.trackX);
        float trackX = this.trackX;
        float trackWidth = this.trackWidth;
        float trackY = centerY - trackHeight * 0.5F;
        float radius = trackHeight * 0.5F;

        nvg.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, radius,
                PhiTheme.mix(PhiTheme.level4(), PhiTheme.level5(), feedback * 0.55F));

        float filled = trackWidth * (this.volume / 100.0F);
        if (filled > 0.0F) {
            nvg.drawRoundedRect(trackX, trackY, filled, trackHeight, radius, PhiTheme.accent());
        }

        if (feedback > 0.001F) {
            float handleWidth = Math.max(2.0F * this.uiScale, trackHeight * 0.8F * feedback);
            float handleHeight = trackHeight * (1.0F + 1.6F * feedback);
            float handleX = Math.max(trackX + handleWidth * 0.5F,
                    Math.min(trackX + trackWidth - handleWidth * 0.5F, trackX + filled));
            nvg.drawRoundedRect(handleX - handleWidth * 0.5F,
                    trackY + trackHeight * 0.5F - handleHeight * 0.5F,
                    handleWidth, handleHeight, handleWidth * 0.5F,
                    PhiTheme.withAlpha(PhiTheme.accent(), feedback));
        }

        nvg.drawText(label, trackRight + this.height * 0.25F,
                centerY - labelSize * 0.5F, PhiTheme.textSecondary(), labelSize, Fonts.REGULAR);
    }

    /**
     * Maps the pointer x onto the track and pushes the new volume.
     *
     * @param mouseX the pointer x.
     */
    private void updateFromMouse(int mouseX) {
        if (this.trackWidth <= 0.0F) {
            return;
        }

        float ratio = (mouseX - this.trackX) / this.trackWidth;
        int percent = Math.round(Math.max(0.0F, Math.min(1.0F, ratio)) * 100.0F);
        if (percent != this.volume) {
            this.volume = percent;
            if (this.listener != null) {
                this.listener.onChanged(percent);
            }
        }
    }
}
