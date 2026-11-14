package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.lwjgl.nanovg.NanoVG;

/**
 * The modules page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The real module system is not implemented yet, so the page only shows a "Coming Soon" placeholder
 * in the middle of the content area. The previous placeholder module list ({@code PhiModulePanel} and
 * {@code PhiPlaceholderModules}) is no longer attached to the page; it is replaced by the real module
 * UI in a later stage.</p>
 */
public class ModulesCategory extends PhiCategory {

    /** The placeholder that is shown until the real module system exists. */
    private static final String COMING_SOON = "Coming Soon";

    /**
     * Creates the modules page.
     */
    public ModulesCategory() {
        super("Modules", PhiIcons.MODULES);
    }

    @Override
    public void draw(NanoVGManager nvg, PhiHubLayout layout, int mouseX, int mouseY, float partialTicks) {
        // The placeholder sits in the middle of the content area. Its center is computed between the
        // slanted left edge (navigation band) and the slanted right edge of the hub at the same height,
        // so it always stays centered inside the parallelogram.
        float top = layout.getHubY() + layout.getPadding();
        float bottom = layout.getHubY() + layout.getHubHeight() - layout.getPadding();
        float centerY = (top + bottom) * 0.5F;

        float left = contentLeft(layout, centerY);
        float right = contentRight(layout, centerY);
        float centerX = (left + right) * 0.5F;

        float size = PhiMetrics.FONT_NAME * layout.getUiScale();

        nvg.drawTextAligned(COMING_SOON, centerX, centerY,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                PhiTheme.textSecondary(), size, Fonts.REGULAR);
    }
}
