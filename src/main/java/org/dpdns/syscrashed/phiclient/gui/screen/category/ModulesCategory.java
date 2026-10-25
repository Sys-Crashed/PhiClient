package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;

/**
 * The modules page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page shows the placeholders of the module system: the list of the modules, the components that
 * build them (toggle, slider, keybind) and the search field. The module system itself is implemented in
 * a later stage.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #layout(PhiHubLayout)} - builds the cards of the page.</li>
 * </ul>
 */
public class ModulesCategory extends PhiCategory {

    /**
     * Creates the modules page.
     */
    public ModulesCategory() {
        super("Modules", PhiIcons.MODULES);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float cardHeight = PhiMetrics.CARD_HEIGHT * layout.getUiScale();
        float spacing = PhiMetrics.CARD_SPACING * layout.getUiScale();
        float top = layout.getHubY() + layout.getPadding();

        addCard(layout, top, cardHeight, "Module list", "The modules of the client with their state", "Planned");

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, "Components", "Toggle / Slider / Keybind", "Planned");

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, "Search", "Filter the modules by their name", "Planned");
    }
}