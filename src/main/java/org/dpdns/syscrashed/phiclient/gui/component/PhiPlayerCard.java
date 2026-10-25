package org.dpdns.syscrashed.phiclient.gui.component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiThemeUtils;
import org.lwjgl.nanovg.NanoVG;

/**
 * The player information card of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Everything of the card lives in one row: the head of the current skin of the player on the left
 * (as an upright square), the name of the player with the welcome text below it and - on the right side
 * - the current date and time. The card uses the global shape of the interface and has no outline.</p>
 *
 * <p><b>Layout rule</b>: the card owns exactly one content column (see {@link PhiCard}). The portrait
 * sits at the left edge of that column, the name and the welcome text share the column that starts
 * behind the portrait and the clock is aligned to the right edge of the column. Nothing can drift
 * apart, because every element uses the same two x positions.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} - renders the card.</li>
 *   <li>{@link #setPlayerName(String)} / {@link #setWelcome(String)} - the content of the card.</li>
 *   <li>{@link #setCustomBackground(boolean)} - enables the scrim of a custom background.</li>
 * </ul>
 */
public class PhiPlayerCard extends PhiComponent {

    /** Format of the clock. */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Format of the date. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final float uiScale;

    private final PhiAvatar avatar;

    /** The left x of the content column. */
    private final float contentLeft;

    /** The right x of the content column. */
    private final float contentRight;

    private String playerName = "Player";

    private String welcome = "Welcome to PhiClient";

    private boolean customBackground;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiPlayerCard(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;

        float padding = PhiMetrics.CARD_PADDING * uiScale;
        float headSize = Math.min(PhiMetrics.SKIN_HEAD_SIZE * uiScale, height - padding * 2.0F);
        float centerY = y + height * 0.5F;

        // The one content column of the card.
        this.contentLeft = PhiSkew.contentLeft(x, height, padding);
        this.contentRight = PhiSkew.contentRight(x, width, padding);

        this.avatar = new PhiAvatar(this.contentLeft, centerY - headSize * 0.5F, headSize, headSize, uiScale);
    }

    /**
     * @param playerName the name of the player.
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName == null || playerName.isEmpty() ? "Player" : playerName;
        this.avatar.setInitial(this.playerName);
    }

    /**
     * @param welcome the welcome text below the name of the player.
     */
    public void setWelcome(String welcome) {
        this.welcome = welcome == null ? "" : welcome;
    }

    /**
     * Enables the 60 percent scrim that keeps the texts readable on a custom background image.
     *
     * @param customBackground true when a custom background is used.
     */
    public void setCustomBackground(boolean customBackground) {
        this.customBackground = customBackground;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = PhiMetrics.CARD_PADDING * this.uiScale;

        float nameSize = PhiMetrics.FONT_NAME * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float clockSize = PhiMetrics.FONT_CLOCK * this.uiScale;

        float centerY = this.y + this.height * 0.5F;

        // Background of the card. The gradient runs diagonally from the upper left corner (the deeper
        // tone) to the lower right corner (the lighter one). Only two neighbouring levels are used, so
        // the card never fades into the window and the gradient does not look dirty.
        int gradientFrom = PhiTheme.isLight() ? PhiTheme.level4() : PhiTheme.level2();
        int gradientTo = PhiTheme.isLight() ? PhiTheme.level2() : PhiTheme.level4();

        nvg.drawSkewRectGradientDiagonal(this.x, this.y, this.width, this.height, radius,
                gradientFrom, gradientTo);

        if (this.customBackground) {
            nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiThemeUtils.scrim());
        }

        // The head of the player.
        this.avatar.draw(nvg, mouseX, mouseY, partialTicks);

        // The name of the player with the welcome text below it - both in the column behind the
        // portrait, so they share one left edge.
        float nameX = this.avatar.getX() + this.avatar.getWidth() + padding;
        float blockHeight = nameSize * 1.25F + bodySize;
        float nameY = centerY - blockHeight * 0.5F;

        // The clock owns the right end of the card, so the name has to stay in front of it.
        LocalDateTime now = LocalDateTime.now();
        float clockWidth = nvg.textWidth(TIME_FORMAT.format(now), clockSize, Fonts.REGULAR);
        float nameWidth = Math.max(0.0F, this.contentRight - clockWidth - padding - nameX);

        nvg.drawText(nvg.limitText(this.playerName, nameSize, Fonts.REGULAR, nameWidth),
                nameX, nameY, PhiTheme.textPrimary(), nameSize, Fonts.REGULAR);

        if (!this.welcome.isEmpty()) {
            float welcomeY = nameY + nameSize * 1.25F;

            nvg.drawText(nvg.limitText(this.welcome, bodySize, Fonts.REGULAR, nameWidth),
                    nameX, welcomeY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
        }

        // Clock and date on the right side of the card.
        float clockY = centerY - clockSize * 0.85F;

        nvg.drawTextAligned(TIME_FORMAT.format(now), this.contentRight, clockY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, PhiTheme.textPrimary(), clockSize, Fonts.REGULAR);

        float dateY = clockY + clockSize * 1.12F;

        nvg.drawTextAligned(DATE_FORMAT.format(now), this.contentRight, dateY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
    }
}
