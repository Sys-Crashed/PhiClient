package org.dpdns.syscrashed.phiclient.gui.component;

import java.net.URI;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.lwjgl.Sys;

/**
 * The button that opens the repository of the project
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The button shows the official mark of GitHub and uses the global shape of the interface. It has no
 * outline: the background level expresses the hover state.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the button.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - opens the repository in the browser.</li>
 * </ul>
 */
public class PhiGitHubButton extends PhiComponent {

    /** The repository that is opened by the button. */
    public static final String REPOSITORY_URL = "https://github.com/Sys-Crashed/PhiClient";

    private final float uiScale;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiGitHubButton(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        int background = isHovered() ? PhiTheme.level4() : PhiTheme.level3();

        nvg.drawSkewRect(this.x, this.y, this.width, this.height,
                PhiMetrics.NAV_ITEM_RADIUS * this.uiScale, background);

        // The mark sits in the centre of the parallelogram, so it uses the centroid of the shape.
        float centerX = this.x + this.width * 0.5F + PhiSkew.offset(this.height) * 0.5F;
        float centerY = this.y + this.height * 0.5F;
        int color = isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary();

        PhiIcons.GITHUB.draw(nvg, centerX, centerY, this.height * 0.52F, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return;
        }

        openRepository();
    }

    /**
     * Opens the repository in the browser. The desktop integration is called through reflection (the
     * same way Minecraft itself does it), so the client does not need AWT on the class path.
     */
    private void openRepository() {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, new URI(REPOSITORY_URL));
        } catch (Throwable t) {
            try {
                Sys.openURL(REPOSITORY_URL);
            } catch (Throwable ignored) {
                PhiLogger.error("Failed to open the repository URL", t);
            }
        }
    }
}
