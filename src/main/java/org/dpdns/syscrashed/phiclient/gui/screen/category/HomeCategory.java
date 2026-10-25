package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.component.PhiChangeLogPanel;
import org.dpdns.syscrashed.phiclient.gui.component.PhiControlBlock;
import org.dpdns.syscrashed.phiclient.gui.component.PhiGitHubPanel;
import org.dpdns.syscrashed.phiclient.gui.component.PhiMusicControl;
import org.dpdns.syscrashed.phiclient.gui.component.PhiPlayerCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiTipCard;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiControlIcons;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiToast;
import org.dpdns.syscrashed.phiclient.utils.PhiGameInfo;

import net.minecraft.client.Minecraft;

/**
 * The home page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page is a grid of two columns, the same way the Soar client builds its home page:</p>
 * <ul>
 *   <li>the <b>left column</b> holds the player information card at the top, the tip card below it,
 *       the six control blocks in the middle and the music player in the bottom row;</li>
 *   <li>the <b>right column</b> holds the change log, which reaches from the top down to the bottom
 *       row - it never overlaps another element.</li>
 * </ul>
 *
 * <p>The six control blocks (copy the address, copy the position, switch the theme, the time, the
 * weather and the dimension) are blocks of their own - they have the same rank as the tip card and are
 * not part of it. Every block shows an icon in the upper half and the text of the block below it.</p>
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

    /** The width of the panel that links to the repository. */
    private static final float GITHUB_PANEL_WIDTH = 190.0F;

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
        float blockHeight = PhiMetrics.CONTROL_BLOCK_HEIGHT * uiScale;
        float blockGap = PhiMetrics.CONTROL_BLOCK_GAP * uiScale;

        float playerTop = contentTop;
        float playerBottom = playerTop + playerHeight;

        float musicTop = contentBottom - musicHeight;

        // The two rows of the control blocks are between the tip card and the bottom row.
        float blocksBottom = musicTop - gap;
        float blocksTop = blocksBottom - blockHeight * 2.0F - blockGap;

        float tipTop = playerBottom + gap;
        float tipBottom = blocksTop - gap;

        // The player information card at the top of the left column.
        float playerX = contentLeft(layout, playerBottom);
        float playerRight = columnRightX + PhiSkew.SLOPE * (contentBottom - playerBottom);

        PhiPlayerCard playerCard = new PhiPlayerCard(playerX, playerTop, playerRight - playerX, playerHeight,
                uiScale);
        playerCard.setPlayerName(getPlayerName());
        playerCard.setWelcome(WELCOME);

        this.components.add(playerCard);

        // The tip card between the player card and the control blocks.
        float tipX = contentLeft(layout, tipBottom);
        float tipRight = columnRightX + PhiSkew.SLOPE * (contentBottom - tipBottom);

        this.components.add(new PhiTipCard(tipX, tipTop, tipRight - tipX, tipBottom - tipTop, uiScale));

        // The six control blocks: three interactive ones and three that only show a value.
        addControlBlocks(layout, columnRightX, contentBottom, blocksTop, blockHeight, blockGap, uiScale);

        // The bottom row of the left column: the music player and the panel of the repository next to
        // it - they are two blocks of their own.
        float repositoryWidth = Math.min(GITHUB_PANEL_WIDTH * uiScale, columnRightX * 0.45F);
        float repositoryX = columnRightX - repositoryWidth;
        float musicX = contentLeft(layout, contentBottom);

        this.components.add(new PhiMusicControl(musicX, musicTop, repositoryX - gap - musicX, musicHeight, uiScale));
        this.components.add(new PhiGitHubPanel(repositoryX, musicTop, repositoryWidth, musicHeight, uiScale));

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
     * Builds the six control blocks of the page: the first row holds the three interactive blocks
     * (copy the address, copy the position, switch the theme), the second row the three blocks that
     * only show a value (the time, the weather and the dimension of the game world).
     *
     * <p>The blocks are blocks of their own - they have the same rank as the tip card. Every block
     * shows an icon in the upper half and the text of the block below it, the whole group sits in the
     * middle of the block.</p>
     *
     * @param layout        the current geometry of the hub.
     * @param columnRightX  the right edge of the left column.
     * @param contentBottom the bottom of the content area.
     * @param blocksTop     the top of the first row of the blocks.
     * @param blockHeight   the height of a block.
     * @param blockGap      the gap between two blocks of a row.
     * @param uiScale       the design scale factor of the interface.
     */
    private void addControlBlocks(PhiHubLayout layout, float columnRightX, float contentBottom, float blocksTop,
                                  float blockHeight, float blockGap, float uiScale) {
        float firstRowTop = blocksTop;
        float firstRowBottom = firstRowTop + blockHeight;
        float firstRowLeft = contentLeft(layout, firstRowBottom);
        float firstRowRight = columnRightX + PhiSkew.SLOPE * (contentBottom - firstRowBottom);
        float firstBlockWidth = (firstRowRight - firstRowLeft - blockGap * 2.0F) / 3.0F;

        // Copy the address of the server.
        final PhiControlBlock copyAddress = new PhiControlBlock(firstRowLeft, firstRowTop, firstBlockWidth,
                blockHeight, uiScale, PhiControlIcons.SIGNAL, PhiControlIcons.CHECK, true, false);
        copyAddress.setLabel("Copy IP");
        copyAddress.setAction(new PhiControlBlock.Action() {
            @Override
            public void onClicked() {
                String address = PhiGameInfo.getServerAddress();
                boolean copied = PhiGameInfo.copy(address);

                showToast("Address", copied ? address : "The clipboard is not available");

                if (copied) {
                    // The icon of the block turns into the check mark for a moment.
                    copyAddress.triggerFeedback();
                }
            }
        });

        this.components.add(copyAddress);

        // Copy the position of the player.
        final PhiControlBlock copyPosition = new PhiControlBlock(
                firstRowLeft + (firstBlockWidth + blockGap), firstRowTop, firstBlockWidth,
                blockHeight, uiScale, PhiControlIcons.CROSSHAIR, PhiControlIcons.CHECK, true, false);
        copyPosition.setLabel("Copy Coord");
        copyPosition.setAction(new PhiControlBlock.Action() {
            @Override
            public void onClicked() {
                String position = PhiGameInfo.getCoordinates();
                boolean copied = PhiGameInfo.copy(position);

                showToast("Position", copied ? position : "The clipboard is not available");

                if (copied) {
                    copyPosition.triggerFeedback();
                }
            }
        });

        this.components.add(copyPosition);

        // Switch the theme. The icon of the block follows the palette on its own: the sun in the
        // light theme, the moon in the dark one.
        PhiControlBlock theme = new PhiControlBlock(firstRowLeft + (firstBlockWidth + blockGap) * 2.0F,
                firstRowTop, firstBlockWidth, blockHeight, uiScale,
                PhiControlIcons.SUN, PhiControlIcons.MOON, true, true);
        theme.setLabel("Theme");
        theme.setAction(new PhiControlBlock.Action() {
            @Override
            public void onClicked() {
                PhiTheme.setActive(PhiTheme.isLight() ? PhiTheme.DARK : PhiTheme.LIGHT);

                showToast("Theme", PhiTheme.isLight() ? "Light" : "Dark");
            }
        });

        this.components.add(theme);

        // The second row: the values of the game world.
        float secondRowTop = firstRowBottom + blockGap;
        float secondRowBottom = secondRowTop + blockHeight;
        float secondRowLeft = contentLeft(layout, secondRowBottom);
        float secondRowRight = columnRightX + PhiSkew.SLOPE * (contentBottom - secondRowBottom);
        float secondBlockWidth = (secondRowRight - secondRowLeft - blockGap * 2.0F) / 3.0F;

        PhiControlBlock time = new PhiControlBlock(secondRowLeft, secondRowTop, secondBlockWidth,
                blockHeight, uiScale, PhiControlIcons.CLOCK, null, false, false);
        time.setValueProvider(new PhiControlBlock.ValueProvider() {
            @Override
            public String getValue() {
                return PhiGameInfo.getTime();
            }
        });

        this.components.add(time);

        PhiControlBlock weather = new PhiControlBlock(
                secondRowLeft + (secondBlockWidth + blockGap), secondRowTop, secondBlockWidth,
                blockHeight, uiScale, PhiControlIcons.WEATHER, null, false, false);
        weather.setValueProvider(new PhiControlBlock.ValueProvider() {
            @Override
            public String getValue() {
                return PhiGameInfo.getWeather();
            }
        });

        this.components.add(weather);

        PhiControlBlock dimension = new PhiControlBlock(
                secondRowLeft + (secondBlockWidth + blockGap) * 2.0F, secondRowTop, secondBlockWidth,
                blockHeight, uiScale, PhiControlIcons.GLOBE, null, false, false);
        dimension.setValueProvider(new PhiControlBlock.ValueProvider() {
            @Override
            public String getValue() {
                return PhiGameInfo.getDimension();
            }
        });

        this.components.add(dimension);
    }

    /**
     * Shows a short confirmation at the top of the game window.
     *
     * @param title   the headline of the message.
     * @param message the text of the message.
     */
    private void showToast(String title, String message) {
        PhiToast toast = PhiClient.getInstance().getGuiManager().getToast();

        toast.show(title, message);
    }
}
