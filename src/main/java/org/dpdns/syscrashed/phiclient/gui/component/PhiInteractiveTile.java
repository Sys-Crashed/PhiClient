package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;

/**
 * Base class for interactive tiles (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Adds the two interaction animations that every clickable control of the interface shares, so future
 * controls get them for free:</p>
 * <ul>
 *   <li><b>Hover reveal</b> - the hover background does not snap on; it spreads from the pointer
 *       position outward (see {@link #hoverAmount()}, {@link #hoverOriginX()}, {@link #hoverOriginY()}
 *       used together with {@code PhiRipple#drawReveal}).</li>
 *   <li><b>Press pulse</b> - a click briefly scales the tile up (ease-out cubic) and then settles back
 *       to its resting size (ease-in cubic), see {@link #pressScale()}.</li>
 * </ul>
 *
 * <p>Subclasses call {@link #updateInteraction(int, int)} at the very start of their draw, wrap their
 * whole drawing in the transform returned by {@link #applyPressTransform(java.util.function.Consumer)}
 * (or use {@link #pressScale()} manually) and read {@link #isHovered()} for the pointer state.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #updateInteraction(int, int)} - refreshes hover and advances the animations.</li>
 *   <li>{@link #isHovered()} - whether the pointer is over the tile.</li>
 *   <li>{@link #hoverAmount()} / {@link #hoverOriginX()} / {@link #hoverOriginY()} - the reveal state.</li>
 *   <li>{@link #pressScale()} / {@link #beginPress()} - the click pulse.</li>
 * </ul>
 */
public abstract class PhiInteractiveTile extends PhiComponent {

    /** How long the hover reveal takes to fully cover the tile (milliseconds). */
    private static final float HOVER_MS = 260.0F;

    /** How long the press grows toward its peak (milliseconds). */
    private static final long PRESS_UP_MS = 90L;

    /** How long the press settles back to rest after the peak (milliseconds). */
    private static final long PRESS_DOWN_MS = 150L;

    /** The relative size of the click pulse at its peak. */
    private static final float PRESS_PEAK = 0.07F;

    private boolean hovered;

    private float hoverAmount;
    private float hoverOriginX;
    private float hoverOriginY;
    private boolean wasHovered;

    private long lastFrame;

    private long pressStart = -1L;

    public PhiInteractiveTile(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.lastFrame = System.currentTimeMillis();
    }

    /**
     * Refreshes the hover state and advances the hover / press animations. Call at the start of draw.
     *
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     */
    protected void updateInteraction(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        boolean hov = PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height);
        if (hov && !this.wasHovered) {
            // The reveal always spreads from where the pointer entered.
            this.hoverOriginX = mouseX;
            this.hoverOriginY = mouseY;
        }
        this.wasHovered = hov;
        this.hovered = hov;

        float target = hov ? 1.0F : 0.0F;
        // Exponential smoothing gives a soft "spread" without a hard edge.
        float k = 1.0F - (float) Math.exp(-deltaMs / (HOVER_MS * 0.35F));
        this.hoverAmount += (target - this.hoverAmount) * Math.min(1.0F, k);
        if (Math.abs(this.hoverAmount) < 0.001F) {
            this.hoverAmount = 0.0F;
        }
    }

    @Override
    public boolean isHovered() {
        return this.hovered;
    }

    /**
     * @return the progress of the hover reveal in {@code [0, 1]} (1 = the whole tile is hovered).
     */
    protected float hoverAmount() {
        return this.hoverAmount;
    }

    /**
     * @return the x the hover reveal spreads from (the pointer entry point).
     */
    protected float hoverOriginX() {
        return this.hoverOriginX;
    }

    /**
     * @return the y the hover reveal spreads from (the pointer entry point).
     */
    protected float hoverOriginY() {
        return this.hoverOriginY;
    }

    /**
     * Triggers the click pulse. Call from the click handler before running the action.
     */
    protected void beginPress() {
        this.pressStart = System.currentTimeMillis();
    }

    /**
     * @return the current scale of the click pulse (1 = resting, slightly above 1 at the peak).
     */
    protected float pressScale() {
        if (this.pressStart < 0L) {
            return 1.0F;
        }

        long elapsed = System.currentTimeMillis() - this.pressStart;

        if (elapsed < PRESS_UP_MS) {
            float p = elapsed / (float) PRESS_UP_MS;
            // Ease-out cubic: fast at first, gentle at the peak.
            float eased = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
            return 1.0F + PRESS_PEAK * eased;
        }

        if (elapsed < PRESS_UP_MS + PRESS_DOWN_MS) {
            float p = (elapsed - PRESS_UP_MS) / (float) PRESS_DOWN_MS;
            // Ease-in cubic: gentle at first, fast at the end as it settles.
            float eased = p * p * p;
            return 1.0F + PRESS_PEAK * (1.0F - eased);
        }

        this.pressStart = -1L;
        return 1.0F;
    }
}
