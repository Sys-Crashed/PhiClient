package org.dpdns.syscrashed.phiclient.gui.component.shape;

import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.utils.mouse.MouseUtils;

/**
 * The upright rounded square of the small icon buttons
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component.shape}).
 *
 * <p>The shape is the axis aligned exception of the interface: it is used exclusively by the small
 * icon buttons (the transport buttons of the music area), because a row of leaning buttons next to
 * upright buttons would not look even. It is a rectangle with rounded corners, so the hit test is the
 * plain bounding box test and the icon sits at the centre of the bounding box.</p>
 *
 * <p>This class is a stateless singleton.</p>
 */
public final class PhiRoundedSquareShape implements PhiShape {

    /** The single instance of the shape. */
    public static final PhiRoundedSquareShape INSTANCE = new PhiRoundedSquareShape();

    private PhiRoundedSquareShape() {
    }

    @Override
    public boolean contains(float pointX, float pointY, float x, float y, float w, float h) {
        return MouseUtils.isInside(pointX, pointY, x, y, w, h);
    }

    @Override
    public float contentLeft(float x, float h, float padding) {
        return x + padding;
    }

    @Override
    public float contentRight(float x, float w, float padding) {
        return x + w - padding;
    }

    @Override
    public float iconCenterX(float x, float w, float h) {
        return x + w * 0.5F;
    }

    @Override
    public void draw(NanoVGManager nvg, float x, float y, float w, float h, float radius, int color) {
        nvg.drawRoundedRect(x, y, w, h, radius, color);
    }
}
