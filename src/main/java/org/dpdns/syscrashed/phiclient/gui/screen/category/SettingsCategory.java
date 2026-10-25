package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiToggle;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The settings page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page hosts the theme switch of the design specification: a toggle that switches between the
 * dark and the light palette of the interface. The remaining settings (language drop down, ...) are
 * implemented in a later stage.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #layout(PhiHubLayout)} - builds the cards of the page.</li>
 * </ul>
 */
public class SettingsCategory extends PhiCategory {

    /** Width of the theme switch. */
    private static final float TOGGLE_WIDTH = 38.0F;

    /** Height of the theme switch. */
    private static final float TOGGLE_HEIGHT = 20.0F;

    private final PhiToggle themeToggle;

    /**
     * Creates the settings page.
     */
    public SettingsCategory() {
        super("Setting", PhiIcons.SETTINGS);

        this.themeToggle = new PhiToggle(0.0F, 0.0F, 0.0F, 0.0F, PhiTheme.isLight());
        this.themeToggle.setOnToggled(new PhiToggle.Listener() {
            @Override
            public void onToggled(boolean value) {
                PhiTheme.setActive(value ? PhiTheme.LIGHT : PhiTheme.DARK);
            }
        });
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float uiScale = layout.getUiScale();
        float cardHeight = PhiMetrics.CARD_HEIGHT * uiScale;
        float spacing = PhiMetrics.CARD_SPACING * uiScale;
        float padding = layout.getPadding();
        float top = layout.getHubY() + padding;

        // Theme row: a card with the theme switch on its right side.
        float cardBottom = top + cardHeight;
        float cardX = contentLeft(layout, cardBottom);
        float cardWidth = contentRight(layout, cardBottom) - cardX;

        float toggleWidth = TOGGLE_WIDTH * uiScale;
        float toggleHeight = TOGGLE_HEIGHT * uiScale;
        float toggleY = top + (cardHeight - toggleHeight) * 0.5F;
        float toggleX = PhiSkew.contentRight(cardX, cardWidth, padding) - toggleWidth;

        this.themeToggle.setX(toggleX);
        this.themeToggle.setY(toggleY);
        this.themeToggle.setWidth(toggleWidth);
        this.themeToggle.setHeight(toggleHeight);
        this.themeToggle.setValue(PhiTheme.isLight());

        PhiCard themeCard = new PhiCard(cardX, top, cardWidth, cardHeight, uiScale,
                "Theme", "Switch between the dark and the light palette", null);
        themeCard.setTrailing(this.themeToggle);

        this.components.add(themeCard);

        // Language row.
        top += cardHeight + spacing;
        cardBottom = top + cardHeight;
        cardX = contentLeft(layout, cardBottom);
        cardWidth = contentRight(layout, cardBottom) - cardX;

        this.components.add(new PhiCard(cardX, top, cardWidth, cardHeight, uiScale,
                "Language", "Drop down selector", "Planned"));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // The theme switch is not part of the component list (the card draws it), so it has to receive
        // the clicks on its own.
        this.themeToggle.mouseClicked(mouseX, mouseY, mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
