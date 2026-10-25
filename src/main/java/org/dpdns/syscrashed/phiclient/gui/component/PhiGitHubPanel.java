package org.dpdns.syscrashed.phiclient.gui.component;

import java.net.URI;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.lwjgl.Sys;

/**
 * The panel that links to the repository of the project
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The panel is a block of its own next to the music area - it is not part of it. It shows the
 * official mark of GitHub with the word "Repository" below it; the whole group sits in the middle of
 * the panel. A click opens the repository in the browser.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the panel.</li>
 *   <li>{@link #mouseClicked(int, int, int)} - opens the repository in the browser.</li>
 * </ul>
 */
public class PhiGitHubPanel extends PhiComponent {

    /** The repository that is opened by the panel. */
    public static final String REPOSITORY_URL = "https://github.com/Sys-Crashed/PhiClient";

    /** Edge length of the mark of GitHub. */
    private static final float MARK_SIZE = 34.0F;

    private final float uiScale;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiGitHubPanel(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);

        int background = isHovered() ? PhiTheme.level4() : PhiTheme.level3();

        nvg.drawSkewRect(this.x, this.y, this.width, this.height,
                PhiMetrics.CORNER_RADIUS * this.uiScale, background);

        float markSize = MARK_SIZE * this.uiScale;
        float labelSize = PhiMetrics.FONT_BODY * this.uiScale;
        float gap = 6.0F * this.uiScale;

        // The mark of GitHub is in the upper half of the panel, the word "Repository" below it - the
        // whole group sits in the middle of the panel.
        float groupHeight = markSize + gap + labelSize;
        float centerX = this.x + this.width * 0.5F + PhiSkew.offset(this.height) * 0.5F;
        float markCenterY = this.y + (this.height - groupHeight) * 0.5F + markSize * 0.5F;

        PhiIcons.GITHUB.draw(nvg, centerX, markCenterY,
                markSize, isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary());

        float labelY = markCenterY + markSize * 0.5F + gap;
        float maxWidth = Math.max(0.0F, this.width - PhiMetrics.CARD_PADDING * this.uiScale * 2.0F);

        nvg.drawTextCentered(nvg.limitText("Repository", labelSize, Fonts.REGULAR, maxWidth),
                centerX, labelY, PhiTheme.textSecondary(), labelSize, Fonts.REGULAR);
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