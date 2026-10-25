package org.dpdns.syscrashed.phiclient.gui.icon;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * A vector icon of the PhiClient interface (package {@code org.dpdns.syscrashed.phiclient.gui.icon}).
 *
 * <p>Icons are drawn as vector paths (no texture, no image resource), so they stay crisp at every GUI
 * scale and follow the accent color of the current state.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int)} - renders the icon.</li>
 * </ul>
 */
public interface PhiIcon {

    /**
     * Draws the icon.
     *
     * @param nvg     the NanoVG drawing context.
     * @param centerX the horizontal center of the icon.
     * @param centerY the vertical center of the icon.
     * @param size    the edge length of the (square) icon box.
     * @param color   the ARGB color to draw the icon with.
     */
    void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color);
}
