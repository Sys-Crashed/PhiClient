package org.dpdns.syscrashed.phiclient.gui.layout;

/**
 * Adaptive geometry of the PhiHub window
 * (package {@code org.dpdns.syscrashed.phiclient.gui.layout}).
 *
 * <p>The hub never changes its size by user input - it only follows the game window. The size is
 * derived from the window area so that the parallelogram covers a fixed share of the screen:</p>
 * <ul>
 *   <li>{@code r = 1 - (9/16) * cot(75 degrees)} is the ratio between the area of the parallelogram and
 *       the area of its 16:9 bounding box;</li>
 *   <li>{@code W_target = 4 * sqrt(share * As / (9 * r))} solves "the parallelogram covers {@code share}
 *       of the window area" for a 16:9 bounding box (35 percent at the moment);</li>
 *   <li>the result is clamped so that the hub always fits completely on screen.</li>
 * </ul>
 *
 * <p>The hub is centered by its <b>geometric</b> centre, not by the centre of its bounding box: the top
 * edge of the parallelogram is shifted to the right, so the geometric centre is shifted by half of the
 * skew.</p>
 *
 * <p>When the game window is resized, the hub follows with a smooth transition (0.15 s) instead of
 * jumping. All values are handled in the scaled GuiScreen coordinate space.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #update(int, int, int, int, float)} - recomputes the geometry for the current frame.</li>
 *   <li>{@link #getHubX()} / {@link #getHubY()} / {@link #getHubWidth()} / {@link #getHubHeight()} - the
 *       bounding box of the hub.</li>
 *   <li>{@link #getSkewOffset()} - the horizontal shift of the top edge.</li>
 *   <li>{@link #getBandWidth()} / {@link #getBandOuterEdgeXAt(float)} / {@link #getBandInnerEdgeXAt(float)}
 *       - the navigation band geometry (the band is placed on the left side of the hub).</li>
 *   <li>{@link #getLeftEdgeXAt(float)} - the slanted left edge of the hub.</li>
 *   <li>{@link #getPadding()} / {@link #getUiScale()} - the current metrics.</li>
 * </ul>
 */
public final class PhiHubLayout {

    /** Tangent of the 75 degree skew angle. */
    private static final double SKEW_TANGENT = Math.tan(Math.toRadians(75.0D));

    /** Ratio between the area of the parallelogram and the area of its 16:9 bounding box. */
    private static final double AREA_FACTOR = 1.0D - (9.0D / 16.0D) / SKEW_TANGENT;

    /**
     * Share of the game window area that is covered by the hub. The 50 percent of the first draft was
     * too dominant on screen, so the hub now covers a little bit more than a third.
     */
    private static final double AREA_SHARE = 0.35D;

    /** Time constant of the resize transition (the hub settles after roughly 0.15 s). */
    private static final double SMOOTHING_MS = 50.0D;

    private float hubX;
    private float hubY;
    private float hubWidth;
    private float hubHeight;

    private float uiScale = 1.0F;

    private boolean initialised;

    /**
     * Recomputes the hub geometry for the current frame.
     *
     * @param displayWidth  the physical width of the game window.
     * @param displayHeight the physical height of the game window.
     * @param scaledWidth   the scaled width of the game window.
     * @param scaledHeight  the scaled height of the game window.
     * @param deltaMs       the time in milliseconds since the previous frame.
     */
    public void update(int displayWidth, int displayHeight, int scaledWidth, int scaledHeight, float deltaMs) {
        this.uiScale = PhiMetrics.uiScale(scaledWidth);

        float scaleFactor = scaledWidth > 0 ? displayWidth / (float) scaledWidth : 1.0F;

        double windowArea = (double) displayWidth * (double) displayHeight;
        double targetWidth = 4.0D * Math.sqrt(AREA_SHARE * windowArea / (9.0D * AREA_FACTOR));
        double maxWidth = Math.min((double) displayWidth, (16.0D / 9.0D) * (double) displayHeight);
        targetWidth = Math.min(targetWidth, maxWidth);
        double targetHeight = targetWidth * 9.0D / 16.0D;

        float targetWidthScaled = (float) (targetWidth / scaleFactor);
        float targetHeightScaled = (float) (targetHeight / scaleFactor);

        if (!this.initialised) {
            this.hubWidth = targetWidthScaled;
            this.hubHeight = targetHeightScaled;
            this.initialised = true;
        } else {
            // Exponential smoothing: the hub follows a window resize with a ~0.15 s transition.
            float factor = 1.0F - (float) Math.exp(-Math.max(0.0F, deltaMs) / SMOOTHING_MS);
            this.hubWidth += (targetWidthScaled - this.hubWidth) * factor;
            this.hubHeight += (targetHeightScaled - this.hubHeight) * factor;
        }

        // The geometric centre of a parallelogram is not the centre of its bounding box: the top edge
        // is shifted to the right, so the centre is shifted by half of the skew. The hub is therefore
        // placed so that its geometric centre - not its bounding box - sits in the middle of the screen.
        float centreOffset = PhiSkew.offset(this.hubHeight) * 0.5F;

        this.hubX = (scaledWidth - this.hubWidth) * 0.5F - centreOffset;
        this.hubY = (scaledHeight - this.hubHeight) * 0.5F;
    }

    /**
     * @return the left x of the hub bounding box.
     */
    public float getHubX() {
        return this.hubX;
    }

    /**
     * @return the top y of the hub bounding box.
     */
    public float getHubY() {
        return this.hubY;
    }

    /**
     * @return the width of the hub bounding box.
     */
    public float getHubWidth() {
        return this.hubWidth;
    }

    /**
     * @return the height of the hub bounding box.
     */
    public float getHubHeight() {
        return this.hubHeight;
    }

    /**
     * @return the horizontal shift of the top edge of the hub.
     */
    public float getSkewOffset() {
        return PhiSkew.offset(this.hubHeight);
    }

    /**
     * @return the horizontal width of the navigation band (10 percent of the hub width).
     */
    public float getBandWidth() {
        return this.hubWidth * PhiMetrics.NAV_BAND_RATIO;
    }

    /**
     * @param y the absolute vertical position.
     * @return the horizontal position of the outer edge of the navigation band at {@code y}. The band
     *         is placed on the left side of the hub and hugs its slanted left edge.
     */
    public float getBandOuterEdgeXAt(float y) {
        return getLeftEdgeXAt(y);
    }

    /**
     * @param y the absolute vertical position.
     * @return the horizontal position of the inner edge of the navigation band at {@code y}. The band
     *         keeps a constant horizontal width at every height because both of its edges are parallel
     *         to the slanted left edge of the hub.
     */
    public float getBandInnerEdgeXAt(float y) {
        return getLeftEdgeXAt(y) + getBandWidth();
    }

    /**
     * @param y the absolute vertical position.
     * @return the horizontal position of the slanted right edge of the hub at {@code y}.
     */
    public float getRightEdgeXAt(float y) {
        return PhiSkew.rightEdgeAt(this.hubX, this.hubWidth, this.hubY, this.hubHeight, y);
    }

    /**
     * @param y the absolute vertical position.
     * @return the horizontal position of the slanted left edge of the hub at {@code y}.
     */
    public float getLeftEdgeXAt(float y) {
        return PhiSkew.leftEdgeAt(this.hubX, this.hubY, this.hubHeight, y);
    }

    /**
     * @return the inset between the hub window and its content.
     */
    public float getPadding() {
        return PhiMetrics.HUB_PADDING * this.uiScale;
    }

    /**
     * @return the factor that converts design pixels into scaled coordinates.
     */
    public float getUiScale() {
        return this.uiScale;
    }
}
