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
 * official mark of GitHub and the name of the repository; a click opens the repository in the
 * browser.</p>
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

    /** The name of the repository. */
    private static final String REPOSITORY_NAME = "Sys-Crashed/PhiClient";

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

        float padding = PhiMetrics.CARD_PADDING * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        // The mark of GitHub sits in its own square, so the whole panel reads as one row.
        float markSize = MARK_SIZE * this.uiScale;
        float markX = contentLeft;
        float markY = this.y + (this.height - markSize) * 0.5F;

        nvg.drawRoundedRect(markX, markY, markSize, markSize,
                PhiMetrics.CHIP_RADIUS * this.uiScale, PhiTheme.level2());

        PhiIcons.GITHUB.draw(nvg, markX + markSize * 0.5F, markY + markSize * 0.5F,
                markSize * 0.55F, isHovered() ? PhiTheme.accent() : PhiTheme.textPrimary());

        // The title and the name of the repository behind the mark.
        float textX = markX + markSize + padding;
        float textWidth = Math.max(0.0F, contentRight - textX);

        float titleY = this.y + this.height * 0.5F - bodySize * 0.7F;

        nvg.drawText(nvg.limitText("GitHub Repository", titleSize, Fonts.REGULAR, textWidth),
                textX, titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        nvg.drawText(nvg.limitText(REPOSITORY_NAME, bodySize, Fonts.REGULAR, textWidth),
                textX, titleY + titleSize * 1.2F, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
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