package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.component.PhiChangeLogPanel;
import org.dpdns.syscrashed.phiclient.gui.component.PhiIconButton;
import org.dpdns.syscrashed.phiclient.gui.component.PhiMusicControl;
import org.dpdns.syscrashed.phiclient.gui.component.PhiPlayerCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiTipCard;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiToast;

import net.minecraft.client.Minecraft;

/**
 * The home page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page is a grid of two columns, the same way the Soar client builds its home page:</p>
 * <ul>
 *   <li>the <b>left column</b> holds the player information card at the top, the tip card below it and
 *       the music player in the bottom row;</li>
 *   <li>the <b>right column</b> holds the change log, which reaches from the top down to the bottom
 *       row - it never overlaps another element.</li>
 * </ul>
 *
 * <p>Every block uses the global shape of the interface. The blocks of the left column are flush
 * against the slanted edge of the navigation band and their right edges follow the same slope, so the
 * column is one continuous band and the gap between the two columns is the same at every height.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #layout(PhiHubLayout)} - builds the blocks of the page.</li>
 * </ul>
 */
public class HomeCategory extends PhiCategory {

    /** The welcome text next to the name of the player. */
    private static final String WELCOME = "Welcome to PhiClient";

    /** The gap between two blocks. */
    private static final float GAP = 10.0F;

    /**
     * Creates the home page.
     */
    public HomeCategory() {
        super("Home", PhiIcons.HOME);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float uiScale = layout.getUiScale();
        float padding = layout.getPadding();
        float gap = GAP * uiScale;

        float contentTop = layout.getHubY() + padding;
        float contentBottom = layout.getHubY() + layout.getHubHeight() - padding;
        float contentHeight = contentBottom - contentTop;

        // The width of the content area, measured at the bottom of the window.
        float contentWidth = contentRight(layout, contentBottom) - contentLeft(layout, contentBottom);

        // The right column holds the change log - it reaches from the top down to the bottom row.
        float changeLogWidth = Math.max(PhiMetrics.CHANGELOG_MIN_WIDTH * uiScale, contentWidth * 0.32F);
        float changeLogX = contentRight(layout, contentBottom) - changeLogWidth;

        // The left column ends in front of the change log. Its right edge follows the slope of the
        // window, so the gap between the two columns is the same at every height.
        float columnRightX = changeLogX - gap;

        // The rows of the left column.
        float playerHeight = Math.max(PhiMetrics.PLAYER_CARD_HEIGHT * uiScale, contentHeight * 0.24F);
        float musicHeight = Math.max(PhiMetrics.MUSIC_CARD_HEIGHT * uiScale, contentHeight * 0.26F);

        float playerTop = contentTop;
        float playerBottom = playerTop + playerHeight;

        float musicTop = contentBottom - musicHeight;

        float tipTop = playerBottom + gap;
        float tipBottom = musicTop - gap;

        // The player information card at the top of the left column.
        float playerX = contentLeft(layout, playerBottom);
        float playerRight = columnRightX + PhiSkew.SLOPE * (contentBottom - playerBottom);

        PhiPlayerCard playerCard = new PhiPlayerCard(playerX, playerTop, playerRight - playerX, playerHeight,
                uiScale);
        playerCard.setPlayerName(getPlayerName());
        playerCard.setWelcome(WELCOME);

        this.components.add(playerCard);

        // The tip card between the player card and the bottom row.
        float tipX = contentLeft(layout, tipBottom);
        float tipRight = columnRightX + PhiSkew.SLOPE * (contentBottom - tipBottom);

        PhiTipCard tipCard = new PhiTipCard(tipX, tipTop, tipRight - tipX, tipBottom - tipTop, uiScale);
        tipCard.setTools(buildTools());

        this.components.add(tipCard);

        // The music player in the bottom row of the left column.
        float musicX = contentLeft(layout, contentBottom);

        this.components.add(new PhiMusicControl(musicX, musicTop, columnRightX - musicX, musicHeight, uiScale));

        // The change log in the right column.
        this.components.add(new PhiChangeLogPanel(changeLogX, contentTop, changeLogWidth, contentHeight, uiScale));
    }

    /**
     * @return the name of the player that is currently in game.
     */
    private String getPlayerName() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.thePlayer == null) {
            return "Player";
        }

        return mc.thePlayer.getName();
    }

    /**
     * Builds the quick access row of the tip card: the theme switch and the buttons that open the
     * other pages of the hub. Every tool is a real action.
     *
     * @return the tools of the row.
     */
    private List<PhiTipCard.Tool> buildTools() {
        List<PhiTipCard.Tool> tools = new ArrayList<PhiTipCard.Tool>();

        tools.add(new PhiTipCard.Tool(PhiIcons.CONTRAST, "Theme", new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                PhiTheme.setActive(PhiTheme.isLight() ? PhiTheme.DARK : PhiTheme.LIGHT);

                PhiToast toast = PhiClient.getInstance().getGuiManager().getToast();
                toast.show("Theme", PhiTheme.isLight() ? "Light" : "Dark");
            }
        }));

        tools.add(new PhiTipCard.Tool(PhiIcons.MODULES, "Modules", new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                openPage("Modules");
            }
        }));

        tools.add(new PhiTipCard.Tool(PhiIcons.HUD, "HUD", new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                openPage("HUD");
            }
        }));

        tools.add(new PhiTipCard.Tool(PhiIcons.MUSIC, "Music", new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                openPage("Music");
            }
        }));

        tools.add(new PhiTipCard.Tool(PhiIcons.SETTINGS, "Setting", new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                openPage("Setting");
            }
        }));

        return tools;
    }

    /**
     * Opens another page of the hub.
     *
     * @param pageId the id of the page.
     */
    private void openPage(String pageId) {
        PageSwitcher switcher = getPageSwitcher();

        if (switcher != null) {
            switcher.openPage(pageId);
        }
    }
}
