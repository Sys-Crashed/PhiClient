package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiLanguageSelector;
import org.dpdns.syscrashed.phiclient.gui.component.PhiToggle;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

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

    /** Width of the language selector (three segments plus the gaps). */
    private static final float LANGUAGE_WIDTH = 216.0F;

    private final PhiToggle themeToggle;

    private final PhiLanguageSelector languageSelector = new PhiLanguageSelector(0.0F, 0.0F, 0.0F, 0.0F);

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
        float cardPadding = PhiMetrics.CARD_PADDING * uiScale;
        float top = layout.getHubY() + layout.getPadding();

        // Theme row: a card with the theme switch on its right side.
        float cardBottom = top + cardHeight;
        float cardX = contentLeft(layout, cardBottom);
        float cardWidth = contentRight(layout, cardBottom) - cardX;

        float toggleWidth = TOGGLE_WIDTH * uiScale;
        float toggleHeight = TOGGLE_HEIGHT * uiScale;
        float toggleY = top + (cardHeight - toggleHeight) * 0.5F;

        // The switch shares the content column of the card - the same right edge as the status text of
        // the other cards, otherwise the two would not be aligned.
        float toggleX = PhiSkew.contentRight(cardX, cardWidth, cardPadding) - toggleWidth;

        this.themeToggle.setX(toggleX);
        this.themeToggle.setY(toggleY);
        this.themeToggle.setWidth(toggleWidth);
        this.themeToggle.setHeight(toggleHeight);
        this.themeToggle.setValue(PhiTheme.isLight());

        PhiCard themeCard = new PhiCard(cardX, top, cardWidth, cardHeight, uiScale,
                PhiI18n.get("settings.theme.title"), PhiI18n.get("settings.theme.desc"), null);
        themeCard.setTrailing(this.themeToggle);

        this.components.add(themeCard);

        // Language row: a card with the three-segment language selector on its right side. The
        // selector shares the content column of the card, the same right edge as the theme switch.
        top += cardHeight + spacing;
        cardBottom = top + cardHeight;
        cardX = contentLeft(layout, cardBottom);
        cardWidth = contentRight(layout, cardBottom) - cardX;

        float selectorWidth = LANGUAGE_WIDTH * uiScale;
        float selectorHeight = TOGGLE_HEIGHT * uiScale;
        float selectorY = top + (cardHeight - selectorHeight) * 0.5F;
        float selectorX = PhiSkew.contentRight(cardX, cardWidth, cardPadding) - selectorWidth;

        this.languageSelector.setX(selectorX);
        this.languageSelector.setY(selectorY);
        this.languageSelector.setWidth(selectorWidth);
        this.languageSelector.setHeight(selectorHeight);

        PhiCard languageCard = new PhiCard(cardX, top, cardWidth, cardHeight, uiScale,
                PhiI18n.get("settings.language.title"), PhiI18n.get("settings.language.desc"), null);
        languageCard.setTrailing(this.languageSelector);

        this.components.add(languageCard);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // The theme switch and the language selector are not part of the component list (the cards
        // draw them), so they have to receive the clicks on their own.
        this.themeToggle.mouseClicked(mouseX, mouseY, mouseButton);
        this.languageSelector.mouseClicked(mouseX, mouseY, mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
