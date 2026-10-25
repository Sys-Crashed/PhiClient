package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.utils.mouse.MouseUtils;

/**
 * Base class for UI components (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>A component owns its position and size, and is responsible for drawing itself and handling
 * mouse/keyboard interaction. Parents (categories / screens) only handle layout and event
 * dispatch.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - draws the component (coordinates are in the scaled GuiScreen space).</li>
 *   <li>{@link #mouseClicked(int, int, int)} / {@link #mouseReleased(int, int, int)} / {@link #keyTyped(char, int)} - interaction stubs.</li>
 *   <li>{@link #updateHover(int, int)} - refreshes the hover state (call at the start of {@link #draw}).</li>
 *   <li>{@link #isHovered()} - whether the pointer is currently over the component.</li>
 *   <li>{@link #getX()} / {@link #setX(float)} and the {@code Y}/{@code Width}/{@code Height} equivalents - geometry accessors.</li>
 * </ul>
 */
public abstract class PhiComponent {

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    private boolean hovered;

    /** The duration of the hover ripple in milliseconds. */
    private static final float RIPPLE_DURATION = 260.0F;

    /** How long the component grows when it is pressed. */
    private static final long PRESS_UP = 120L;

    /** The total duration of the press feedback. */
    private static final long PRESS_TOTAL = 300L;

    /** How much the component grows when it is pressed. */
    private static final float PRESS_AMOUNT = 0.06F;

    /** The point where the pointer entered the component (the center of the hover ripple). */
    private float hoverEnterX;

    private float hoverEnterY;

    /** The progress of the hover ripple (1 = the component is completely highlighted). */
    private float hoverProgress;

    /** The time of the last frame in which the hover state was updated. */
    private long hoverTime;

    /** The time of the last press (0 when there is none). */
    private long pressTime;

    public PhiComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Draws the component. Coordinates are always in the GuiScreen's scaled coordinate system.
     *
     * @param nvg          the NanoVG drawing context.
     * @param mouseX       the current pointer x.
     * @param mouseY       the current pointer y.
     * @param partialTicks the render interpolation factor.
     */
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
    }

    /**
     * Called when the component is clicked.
     *
     * @param mouseX      the click x.
     * @param mouseY      the click y.
     * @param mouseButton the mouse button index.
     */
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    /**
     * Called when a mouse button is released over the component.
     *
     * @param mouseX      the release x.
     * @param mouseY      the release y.
     * @param mouseButton the mouse button index.
     */
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    /**
     * Called when a key is typed while the component is focused.
     *
     * @param typedChar the typed character.
     * @param keyCode   the key code.
     */
    public void keyTyped(char typedChar, int keyCode) {
    }

    /**
     * Refreshes the hover state. Subclasses should call this at the start of {@link #draw}.
     *
     * <p>The method also drives the hover ripple: the highlight expands from the point where the
     * pointer entered the component and shrinks back to it when the pointer leaves.</p>
     *
     * @param mouseX the current pointer x.
     * @param mouseY the current pointer y.
     */
    protected void updateHover(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        float deltaMs = this.hoverTime == 0L ? 16.0F : Math.min(100.0F, now - this.hoverTime);
        this.hoverTime = now;

        boolean inside = isInside(mouseX, mouseY);

        if (inside && !this.hovered) {
            this.hoverEnterX = mouseX;
            this.hoverEnterY = mouseY;
        }

        this.hovered = inside;

        this.hoverProgress += (inside ? 1.0F : -1.0F) * deltaMs / RIPPLE_DURATION;
        this.hoverProgress = Math.max(0.0F, Math.min(1.0F, this.hoverProgress));
    }

    /**
     * @param pointX the point x.
     * @param pointY the point y.
     * @return true if the point is inside the component. Components that are not axis aligned
     *         rectangles override this method.
     */
    protected boolean isInside(float pointX, float pointY) {
        return MouseUtils.isInside(pointX, pointY, this.x, this.y, this.width, this.height);
    }

    /**
     * Starts the press feedback: the component grows a little and shrinks back to its original size.
     */
    protected void beginPress() {
        this.pressTime = System.currentTimeMillis();
    }

    /**
     * @return the scale of the press feedback (1 = the original size). The component grows with an
     *         out-cubic easing and shrinks back with an in-cubic easing.
     */
    protected float getPressScale() {
        if (this.pressTime == 0L) {
            return 1.0F;
        }

        long elapsed = System.currentTimeMillis() - this.pressTime;

        if (elapsed >= PRESS_TOTAL) {
            this.pressTime = 0L;
            return 1.0F;
        }

        if (elapsed < PRESS_UP) {
            float t = elapsed / PRESS_UP;
            return 1.0F + PRESS_AMOUNT * (float) (1.0D - Math.pow(1.0D - t, 3.0D));
        }

        float t = (elapsed - PRESS_UP) / (PRESS_TOTAL - PRESS_UP);
        return 1.0F + PRESS_AMOUNT * (float) Math.pow(t, 3.0D);
    }

    /**
     * @return the progress of the hover ripple in the range {@code [0, 1]}.
     */
    protected float getHoverProgress() {
        return this.hoverProgress;
    }

    /**
     * @return the x of the point where the pointer entered the component.
     */
    protected float getHoverEnterX() {
        return this.hoverEnterX;
    }

    /**
     * @return the y of the point where the pointer entered the component.
     */
    protected float getHoverEnterY() {
        return this.hoverEnterY;
    }

    /**
     * @return true if the pointer is currently over the component.
     */
    public boolean isHovered() {
        return this.hovered;
    }

    /**
     * Overwrites the hover state. Subclasses use this when the component is not an axis aligned
     * rectangle and therefore needs its own hit test.
     *
     * @param hovered the new hover state.
     */
    protected void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    /**
     * @return the component's left x.
     */
    public float getX() {
        return this.x;
    }

    /**
     * @param x the new left x.
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * @return the component's top y.
     */
    public float getY() {
        return this.y;
    }

    /**
     * @param y the new top y.
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * @return the component width.
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * @param width the new width.
     */
    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * @return the component height.
     */
    public float getHeight() {
        return this.height;
    }

    /**
     * @param height the new height.
     */
    public void setHeight(float height) {
        this.height = height;
    }
}
