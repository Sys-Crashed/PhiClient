package org.dpdns.syscrashed.phiclient.gui.icon;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * An icon that is rasterized from an SVG resource
 * (package {@code org.dpdns.syscrashed.phiclient.gui.icon}).
 *
 * <p>The SVG files live in {@code /assets/phiclient/images} and are parsed by NanoSVG the first time
 * the icon is drawn. The rasterized image is tinted with the color of the state, so the same file can
 * be used in every theme and in every state (default, hover, accent).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, float, float, float, int)} - renders the icon.</li>
 * </ul>
 */
public class PhiSvgIcon implements PhiIcon {

    /** The SVG is rasterized with this factor of the drawn size, so it stays crisp. */
    private static final float OVERSAMPLE = 2.0F;

    /** The path of the resource (for example {@code /assets/phiclient/images/sun.svg}). */
    private final String resourcePath;

    /**
     * @param resourcePath the path of the SVG resource.
     */
    public PhiSvgIcon(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
        if (this.resourcePath == null || size <= 0.0F) {
            return;
        }

        int image = nvg.getSvgImage(this.resourcePath, size * OVERSAMPLE, color);

        if (image == 0) {
            return;
        }

        nvg.drawImage(image, centerX - size * 0.5F, centerY - size * 0.5F, size, size, 1.0F);
    }
}
