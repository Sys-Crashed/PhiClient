package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.render.PlayerHeadRenderer;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The portrait of the player (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The portrait shows the front of the head of the current skin of the player (see
 * {@link PlayerHeadRenderer}). While the skin is still loading, the initial of the player is drawn
 * instead. The portrait is a <b>square</b>: it is the one element of the interface that is not sheared,
 * so the head of the skin keeps its upright shape.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the portrait.</li>
 *   <li>{@link #setInitial(String)} - the fallback text of the portrait.</li>
 * </ul>
 */
public class PhiAvatar extends PhiComponent {

    private static final PlayerHeadRenderer HEAD_RENDERER = new PlayerHeadRenderer();

    private String initial = "?";

    private final float uiScale;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiAvatar(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
    }

    /**
     * @param initial the text of the portrait (usually the first letter of the player name).
     */
    public void setInitial(String initial) {
        this.initial = initial == null || initial.isEmpty() ? "?" : initial.substring(0, 1).toUpperCase();
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float size = Math.min(this.width, this.height);

        // The skin of the player is preferred, the initial is only a fallback.
        if (HEAD_RENDERER.drawHead(nvg, this.x, this.y, size, radius)) {
            return;
        }

        nvg.drawRoundedRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        float fontSize = size * 0.46F;
        nvg.drawTextCentered(this.initial, this.x + this.width * 0.5F,
                this.y + (this.height - fontSize) * 0.5F, PhiTheme.accent(), fontSize, Fonts.REGULAR);
    }
}
