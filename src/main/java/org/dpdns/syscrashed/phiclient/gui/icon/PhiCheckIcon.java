package org.dpdns.syscrashed.phiclient.gui.icon;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * The check mark that confirms a successful action
 * (package {@code org.dpdns.syscrashed.phiclient.gui.icon}).
 *
 * <p>In contrast to the rasterized SVG icons this is a vector path: the feedback animation can draw
 * only a prefix of the stroke, which produces the draw-on effect of the Cupertino replace preset.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int)} - draws the complete mark.</li>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int, float)} - draws a prefix of the stroke.</li>
 * </ul>
 */
public final class PhiCheckIcon implements PhiIcon {

    @Override
    public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
        draw(nvg, centerX, centerY, size, color, 1.0F);
    }

    /**
     * Draws the check mark up to the given fraction of its total stroke length.
     *
     * @param nvg      the NanoVG drawing context.
     * @param centerX  the horizontal center of the icon.
     * @param centerY  the vertical center of the icon.
     * @param size     the edge length of the icon box.
     * @param color    the ARGB color of the stroke.
     * @param progress the fraction of the stroke to draw (0 = nothing, 1 = the complete mark).
     */
    public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color, float progress) {
        if (size <= 0.0F || progress <= 0.001F) {
            return;
        }

        float ax = centerX - size * 0.32F;
        float ay = centerY + size * 0.02F;
        float bx = centerX - size * 0.08F;
        float by = centerY + size * 0.28F;
        float cx = centerX + size * 0.36F;
        float cy = centerY - size * 0.26F;

        float firstLength = (float) Math.hypot(bx - ax, by - ay);
        float secondLength = (float) Math.hypot(cx - bx, cy - by);
        float drawn = Math.min(1.0F, progress) * (firstLength + secondLength);

        nvg.beginPath();
        nvg.moveTo(ax, ay);

        if (drawn <= firstLength) {
            float t = drawn / firstLength;
            nvg.lineTo(ax + (bx - ax) * t, ay + (by - ay) * t);
        } else {
            float t = (drawn - firstLength) / secondLength;
            nvg.lineTo(bx, by);
            nvg.lineTo(bx + (cx - bx) * t, by + (cy - by) * t);
        }

        nvg.strokePath(color, Math.max(1.0F, size * 0.11F));
    }
}
