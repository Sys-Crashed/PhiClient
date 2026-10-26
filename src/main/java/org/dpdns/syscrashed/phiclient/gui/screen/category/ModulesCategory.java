package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.PhiModulePanel;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.module.PhiModule;
import org.dpdns.syscrashed.phiclient.gui.module.PhiPlaceholderModules;

/**
 * The modules page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page holds the module list of the client. Ten placeholder modules test the GUI for now: five
 * of them are plain switches, five of them additionally adjust their parameters in the "more settings"
 * panel, which is opened by the gear icon of their row - the same way the Soar client does it. The
 * real module system replaces the placeholders in a later stage.</p>
 */
public class ModulesCategory extends PhiCategory {

    /** The placeholder modules of the page. */
    private final List<PhiModule> modules = PhiPlaceholderModules.create();

    /** The module panel of the page; kept between layouts so the scroll and view states survive. */
    private PhiModulePanel modulePanel;

    /** The ui scale the panel was built with; a change rebuilds the panel. */
    private float lastUiScale = -1.0F;

    /**
     * Creates the modules page.
     */
    public ModulesCategory() {
        super("Modules", PhiIcons.MODULES);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float top = layout.getHubY() + layout.getPadding();
        float bottom = layout.getHubY() + layout.getHubHeight() - layout.getPadding();
        float x = contentLeft(layout, bottom);
        float width = contentRight(layout, bottom) - x;
        float height = bottom - top;

        // The panel is placed flush against the slanted edges of the hub, like the cards of the other
        // pages: its left edge is parallel to the navigation band at the constant distance of the
        // padding.
        if (this.modulePanel == null || this.lastUiScale != layout.getUiScale()) {
            this.modulePanel = new PhiModulePanel(x, top, width, height, layout.getUiScale(), this.modules);
            this.lastUiScale = layout.getUiScale();
        } else {
            this.modulePanel.setX(x);
            this.modulePanel.setY(top);
            this.modulePanel.setWidth(width);
            this.modulePanel.setHeight(height);
        }

        this.components.add(this.modulePanel);
    }
}
