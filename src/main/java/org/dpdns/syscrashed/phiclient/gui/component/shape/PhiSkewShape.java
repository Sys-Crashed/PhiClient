package org.dpdns.syscrashed.phiclient.gui.component.shape;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * The global right leaning parallelogram of the interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.shape}).
 *
 * <p>The shape is the only parallelogram family: the top edge is shifted to the right by
 * {@link PhiSkew#offset(float)} relative to the bottom edge, so every edge of the interface shares
 * the same slope. The hit test follows {@link PhiSkew#contains} - the bounding box would report hits
 * outside of the shape - and the icon centre is the centroid of the parallelogram, which sits
 * {@code offset(h) * 0.5} right of the bounding box centre.</p>
 *
 * <p>This class is a stateless singleton.</p>
 */
public final class PhiSkewShape implements PhiShape {

    /** The single instance of the shape. */
    public static final PhiSkewShape INSTANCE = new PhiSkewShape();

    private PhiSkewShape() {
    }

    @Override
    public boolean contains(float pointX, float pointY, float x, float y, float w, float h) {
        return PhiSkew.contains(pointX, pointY, x, y, w, h);
    }

    @Override
    public float contentLeft(float x, float h, float padding) {
        return PhiSkew.contentLeft(x, h, padding);
    }

    @Override
    public float contentRight(float x, float w, float padding) {
        return PhiSkew.contentRight(x, w, padding);
    }

    @Override
    public float iconCenterX(float x, float w, float h) {
        return x + w * 0.5F + PhiSkew.offset(h) * 0.5F;
    }

    @Override
    public void draw(NanoVGManager nvg, float x, float y, float w, float h, float radius, int color) {
        nvg.drawSkewRect(x, y, w, h, radius, color);
    }
}
