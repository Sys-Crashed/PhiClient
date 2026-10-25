package org.dpdns.syscrashed.phiclient.gui.layout;

/**
 * Fixed design metrics of the PhiClient UI
 * (package {@code org.dpdns.syscrashed.phiclient.gui.layout}).
 *
 * <p>All sizes are expressed in "design pixels" of the 1920x1080 baseline and are multiplied by
 * {@link #uiScale(int)} before they are used in the scaled GuiScreen coordinate space. This keeps the
 * proportions of the interface identical on every resolution while the GUI scale only affects the
 * crispness of the rendering.</p>
 *
 * <p>Provided members:</p>
 * <ul>
 *   <li>{@link #DESIGN_WIDTH} - the horizontal design baseline.</li>
 *   <li>{@link #NAV_ITEM_HEIGHT} / {@link #NAV_ITEM_SPACING} / {@link #NAV_ICON_SIZE} - navigation
 *       metrics.</li>
 *   <li>{@link #NAV_BAND_RATIO} - the width of the navigation band relative to the hub width.</li>
 *   <li>{@link #HUB_PADDING} / {@link #CARD_PADDING} / {@link #CARD_HEIGHT} - content metrics.</li>
 *   <li>{@link #TOOLTIP_*} - tooltip metrics and timing.</li>
 *   <li>{@link #uiScale(int)} - the factor that converts design pixels into scaled coordinates.</li>
 * </ul>
 */
public final class PhiMetrics {

    private PhiMetrics() {
    }

    /** The horizontal design baseline (1920x1080). */
    public static final float DESIGN_WIDTH = 1920.0F;

    /** Height of a single navigation entry. */
    public static final float NAV_ITEM_HEIGHT = 48.0F;

    /** Vertical gap between two navigation entries. */
    public static final float NAV_ITEM_SPACING = 6.0F;

    /** Edge length of a navigation icon. */
    public static final float NAV_ICON_SIZE = 20.0F;

    /** Width of the navigation band relative to the hub width. */
    public static final float NAV_BAND_RATIO = 0.10F;

    /** Inset between the navigation band and a navigation entry. */
    public static final float NAV_PADDING = 8.0F;

    /** Corner radius of the hub window and of the content cards. */
    public static final float CORNER_RADIUS = 16.0F;

    /** Corner radius of a navigation entry. */
    public static final float NAV_ITEM_RADIUS = 12.0F;

    /** Inset between the hub window and its content. */
    public static final float HUB_PADDING = 26.0F;

    /** Inset between a card and its text. */
    public static final float CARD_PADDING = 18.0F;

    /** Height of a standard content card. */
    public static final float CARD_HEIGHT = 58.0F;

    /** Vertical gap between two content cards. */
    public static final float CARD_SPACING = 12.0F;

    /** Height of a tooltip box. */
    public static final float TOOLTIP_HEIGHT = 34.0F;

    /** Horizontal padding inside a tooltip box. */
    public static final float TOOLTIP_PADDING = 14.0F;

    /** Distance between the anchor icon and the tooltip box. */
    public static final float TOOLTIP_GAP = 12.0F;

    /** Vertical distance the tooltip travels while it fades in. */
    public static final float TOOLTIP_OFFSET_Y = 6.0F;

    /** Delay before a tooltip starts to appear. */
    public static final float TOOLTIP_HOVER_DELAY = 200.0F;

    /** Delay before a tooltip starts to disappear. */
    public static final float TOOLTIP_EXIT_DELAY = 200.0F;

    /** Duration of the tooltip fade in / out. */
    public static final float TOOLTIP_FADE = 150.0F;

    /** Font size of a card title. */
    public static final float FONT_TITLE = 17.0F;

    /** Font size of a card description and of the status text. */
    public static final float FONT_BODY = 13.0F;

    /** Font size of the player name on the home page. */
    public static final float FONT_NAME = 22.0F;

    /** Font size of the clock on the home page. */
    public static final float FONT_CLOCK = 26.0F;

    /** Font size of a tooltip. */
    public static final float FONT_TOOLTIP = 13.0F;

    // ------------------------------------------------------------------ home page

    /** Height of the player information card. */
    public static final float PLAYER_CARD_HEIGHT = 96.0F;

    /** Height of the music control area. */
    public static final float MUSIC_CARD_HEIGHT = 96.0F;

    /** Edge length of the player avatar (the head of the skin). */
    public static final float SKIN_HEAD_SIZE = 64.0F;

    /** Edge length of the album cover. */
    public static final float COVER_SIZE = 48.0F;

    /** Edge length of an icon button. */
    public static final float ICON_BUTTON_SIZE = 30.0F;

    /** Height of the music progress bar. */
    public static final float PROGRESS_BAR_HEIGHT = 8.0F;

    /** Minimum width of the change log panel. */
    public static final float CHANGELOG_MIN_WIDTH = 170.0F;

    /** Height of a control block of the home page. */
    public static final float CONTROL_BLOCK_HEIGHT = 60.0F;

    /** Edge length of the icon of a control block. */
    public static final float CONTROL_BLOCK_ICON_SIZE = 20.0F;

    /** Vertical distance between the icon and the text of a control block. */
    public static final float CONTROL_BLOCK_ICON_GAP = 4.0F;

    /** Inset between the content of a control block and its edges. */
    public static final float CONTROL_BLOCK_PADDING = 8.0F;

    /** Horizontal distance between two control blocks of a row. */
    public static final float CONTROL_BLOCK_GAP = 8.0F;

    /**
     * Converts design pixels into the scaled GuiScreen coordinate space.
     *
     * @param scaledWidth the scaled width of the game window.
     * @return the scale factor (1.0 on a 1920 pixel wide window with GUI scale 1).
     */
    public static float uiScale(int scaledWidth) {
        return scaledWidth / DESIGN_WIDTH;
    }
}
