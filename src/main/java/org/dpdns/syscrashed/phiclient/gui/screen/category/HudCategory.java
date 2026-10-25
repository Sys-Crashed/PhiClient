package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;

/**
 * The HUD page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page currently shows a placeholder card. The full screen HUD editor of the design
 * specification (drag and drop placement, grid snapping and a minimal control bar) is implemented in a
 * later stage.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #layout(PhiHubLayout)} - builds the cards of the page.</li>
 * </ul>
 */
public class HudCategory extends PhiCategory {

    /**
     * Creates the HUD page.
     */
    public HudCategory() {
        super("HUD", PhiIcons.HUD);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float cardHeight = PhiMetrics.CARD_HEIGHT * layout.getUiScale();
        float top = layout.getHubY() + layout.getPadding();

        addCard(layout, top, cardHeight, "HUD editor", "Drag and drop placement of the HUD elements", "Planned");
    }
}
