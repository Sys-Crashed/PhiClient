package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * A toggle switch of the PhiClient interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The switch consists of a track and a knob - both use the global shape of the interface (a
 * parallelogram with 75 / 105 degree angles). The knob slides between the two ends of the track and
 * the transition is animated. There is no outline: the state is expressed by the background level and
 * by the color of the knob.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the switch.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - toggles the switch.</li>
 *   <li>{@link #isValue()} / {@link #setValue(boolean)} - the state of the switch.</li>
 *   <li>{@link #setOnToggled(Listener)} - the callback that receives a state change.</li>
 * </ul>
 */
public class PhiToggle extends PhiComponent {

    /** Callback that is invoked when the user toggles the switch. */
    public interface Listener {
        /**
         * @param value the new state of the switch.
         */
        void onToggled(boolean value);
    }

    /** Duration of the knob transition in milliseconds. */
    private static final float TRANSITION_MS = 140.0F;

    private boolean value;

    /** The animated position of the knob (0 = off, 1 = on). */
    private float position;

    private Listener listener;

    private long lastFrame;

    /**
     * @param x      the left x of the switch.
     * @param y      the top y of the switch.
     * @param width  the width of the switch.
     * @param height the height of the switch.
     * @param value  the initial state of the switch.
     */
    public PhiToggle(float x, float y, float width, float height, boolean value) {
        super(x, y, width, height);

        this.value = value;
        this.position = value ? 1.0F : 0.0F;
        this.lastFrame = System.currentTimeMillis();
    }

    /**
     * @param listener the callback that receives the state changes.
     */
    public void setOnToggled(Listener listener) {
        this.listener = listener;
    }

    /**
     * @return true if the switch is on.
     */
    public boolean isValue() {
        return this.value;
    }

    /**
     * Sets the state of the switch without notifying the listener.
     *
     * @param value the new state.
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float target = this.value ? 1.0F : 0.0F;
        this.position += (target - this.position) * Math.min(1.0F, deltaMs / TRANSITION_MS);

        float inset = this.height * 0.15F;
        float knobSize = this.height - inset * 2.0F;
        float radius = this.height * 0.3F;

        // Track.
        int track = isHovered() ? PhiTheme.level5() : PhiTheme.level4();
        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, track);

        // Knob.
        float travel = this.width - knobSize - inset * 2.0F;
        float knobX = this.x + inset + travel * this.position;
        int knob = this.value ? PhiTheme.accent() : PhiTheme.level5();

        nvg.drawSkewRect(knobX, this.y + inset, knobSize, knobSize, knobSize * 0.3F, knob);
    }

    /**
     * The switch is a parallelogram, so the pointer has to be tested against the shape and not against
     * its bounding box.
     *
     * @param pointX the point x.
     * @param pointY the point y.
     * @return true if the point is inside the switch.
     */
    @Override
    protected boolean isInside(float pointX, float pointY) {
        return PhiSkew.contains(pointX, pointY, this.x, this.y, this.width, this.height);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return;
        }

        this.value = !this.value;

        if (this.listener != null) {
            this.listener.onToggled(this.value);
        }
    }
}
