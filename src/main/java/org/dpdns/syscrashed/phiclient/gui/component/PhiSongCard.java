package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.component.text.PhiStaticText;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.PhiTrack;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.lwjgl.nanovg.NanoVG;

/**
 * One song row of the music library (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The card is a parallelogram of the global shape (hit test {@link PhiSkew#contains}) and shows the
 * cover, the title, the artist and the duration of its {@link PhiTrack}. The currently playing track
 * is expressed by the accent colored title and cover icon - never by an outline. The favorite star
 * and the add-to-playlist button at the right end are upright controls with an axis aligned hit box;
 * clicking anywhere else on the card plays the track.</p>
 *
 * <p>The texts are cached through {@link PhiStaticText}, so the draw path does not shorten unchanged
 * strings every frame; the metadata of the track arrives asynchronously and simply appears when the
 * title content changes. The hover feedback is the shared {@link PhiSheen} sweep.</p>
 */
public class PhiSongCard extends PhiComponent {

    /** The actions a song card offers to its owner. */
    public interface ActionListener {

        /**
         * @param card the clicked card (play it).
         */
        void onPlay(PhiSongCard card);

        /**
         * @param card the card whose favorite star was clicked.
         */
        void onFavorite(PhiSongCard card);

        /**
         * @param card the card whose add-to-playlist button was clicked.
         */
        void onAddToPlaylist(PhiSongCard card);

        /**
         * @param card the card whose delete button was clicked.
         */
        void onDelete(PhiSongCard card);
    }

    /** The height of one song row in design pixels. */
    public static final float ROW_HEIGHT = 56.0F;

    private final PhiTrack track;

    private final float uiScale;

    private final PhiStaticText title = new PhiStaticText();

    private final PhiStaticText artist = new PhiStaticText();

    private final PhiSheen sheen = new PhiSheen();

    private ActionListener listener;

    private boolean favorite;

    private boolean playing;

    /** The hit boxes of the two upright controls (computed by the last draw). */
    private float favoriteX;
    private float favoriteY;
    private float controlSize;
    private float addX;
    private float deleteX;

    private float favoriteAmount;
    private float favoriteHover;
    private float addHover;
    private float deleteHover;
    private long lastFrame = System.currentTimeMillis();

    /**
     * @param track   the track of the card.
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiSongCard(PhiTrack track, float x, float y, float width, float uiScale) {
        super(x, y, width, ROW_HEIGHT * uiScale);
        this.track = track;
        this.uiScale = uiScale;
    }

    /**
     * @return the track of the card.
     */
    public PhiTrack getTrack() {
        return this.track;
    }

    /**
     * @param listener the action callback.
     */
    public void setActionListener(ActionListener listener) {
        this.listener = listener;
    }

    /**
     * @param favorite whether the track is a favorite (star in the accent color).
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /**
     * @param playing whether this card is the currently playing track.
     */
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        updateHover(mouseX, mouseY);
        this.sheen.update(isHovered());

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        this.favoriteAmount = this.favorite
                ? Math.min(1.0F, this.favoriteAmount + deltaMs / 160.0F)
                : Math.max(0.0F, this.favoriteAmount - deltaMs / 160.0F);
        this.favoriteHover = approach(this.favoriteHover,
                insideBox(mouseX, mouseY, this.favoriteX, this.favoriteY, this.controlSize), deltaMs);
        this.addHover = approach(this.addHover,
                insideBox(mouseX, mouseY, this.addX, this.favoriteY, this.controlSize), deltaMs);
        this.deleteHover = approach(this.deleteHover,
                insideBox(mouseX, mouseY, this.deleteX, this.favoriteY, this.controlSize), deltaMs);

        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        int background = this.playing ? PhiTheme.level4()
                : PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), getHoverProgress());

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, background);
        this.sheen.drawSkew(nvg, this.x, this.y, this.width, this.height, radius,
                getHoverProgress(), PhiTheme.sheen());

        float padding = 12.0F * this.uiScale;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        // Cover (upright rounded square with the note placeholder).
        float coverSize = this.height - padding * 1.6F;
        float coverY = this.y + (this.height - coverSize) * 0.5F;
        nvg.drawRoundedRect(contentLeft, coverY, coverSize, coverSize, radius * 0.6F, PhiTheme.level2());
        PhiIcons.MUSIC.draw(nvg, contentLeft + coverSize * 0.5F, coverY + coverSize * 0.5F,
                coverSize * 0.5F, this.playing ? PhiTheme.accent() : PhiTheme.textSecondary());

        // The upright controls at the right end: delete, add-to-playlist and the favorite star.
        this.controlSize = this.height * 0.42F;
        float controlY = this.y + (this.height - this.controlSize) * 0.5F;
        this.favoriteY = controlY;
        this.addX = contentRight - this.controlSize;
        this.favoriteX = this.addX - this.controlSize - padding * 0.6F;
        this.deleteX = this.favoriteX - this.controlSize - padding * 0.6F;

        // Duration left of the controls (a fixed slot, so the row does not jump).
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale * 0.88F;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale * 0.92F;
        float timeSize = bodySize * 0.95F;
        String duration = formatTime(this.track.getDurationSeconds());
        float durationRight = this.deleteX - padding * 0.6F;

        nvg.drawTextAligned(duration, durationRight, this.y + (this.height - timeSize) * 0.5F,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);

        // Title and artist behind the cover, clipped before the duration.
        float textX = contentLeft + coverSize + padding;
        float textWidth = Math.max(0.0F, durationRight - nvg.textWidth("00:00", timeSize, Fonts.REGULAR)
                - padding * 0.6F - textX);

        this.title.setContent(this.track.getTitle());
        String artistText = this.track.getArtist() != null ? this.track.getArtist()
                : PhiI18n.get("music.local");
        this.artist.setContent(artistText);

        float titleY = this.y + padding * 0.75F;
        nvg.drawText(this.title.getLimited(nvg, titleSize, Fonts.REGULAR, textWidth),
                textX, titleY, this.playing ? PhiTheme.accent() : PhiTheme.textPrimary(),
                titleSize, Fonts.REGULAR);
        nvg.drawText(this.artist.getLimited(nvg, bodySize, Fonts.REGULAR, textWidth),
                textX, titleY + titleSize * 1.30F, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // Upright controls: each one eases its own color and size independently of the row.
        float centerY = controlY + this.controlSize * 0.5F;
        PhiIcons.PLUS.draw(nvg, this.addX + this.controlSize * 0.5F, centerY,
                this.controlSize * 0.72F * (1.0F + 0.10F * this.addHover),
                PhiTheme.mix(PhiTheme.textPrimary(), PhiTheme.accent(), this.addHover));

        float favoriteCenterX = this.favoriteX + this.controlSize * 0.5F;
        float favoriteSize = this.controlSize * 0.86F * (1.0F + 0.10F * this.favoriteHover);
        int favoriteOutline = PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.accent(),
                Math.max(this.favoriteHover, this.favoriteAmount));
        PhiIcons.FAVORITE.draw(nvg, favoriteCenterX, centerY, favoriteSize, favoriteOutline);
        if (this.favoriteAmount > 0.001F) {
            PhiIcons.FAVORITE_FILLED.draw(nvg, favoriteCenterX, centerY, favoriteSize,
                    PhiTheme.withAlpha(PhiTheme.accent(), this.favoriteAmount));
        }

        PhiIcons.DELETE.draw(nvg, this.deleteX + this.controlSize * 0.5F, centerY,
                this.controlSize * 0.72F * (1.0F + 0.10F * this.deleteHover),
                PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.danger(), this.deleteHover));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || this.listener == null) {
            return;
        }

        // The upright controls use axis aligned boxes (they are not part of the skew family).
        if (insideBox(mouseX, mouseY, this.favoriteX, this.favoriteY, this.controlSize)) {
            beginPress();
            this.listener.onFavorite(this);
            return;
        }
        if (insideBox(mouseX, mouseY, this.addX, this.favoriteY, this.controlSize)) {
            beginPress();
            this.listener.onAddToPlaylist(this);
            return;
        }
        if (insideBox(mouseX, mouseY, this.deleteX, this.favoriteY, this.controlSize)) {
            beginPress();
            this.listener.onDelete(this);
            return;
        }

        if (isInside(mouseX, mouseY)) {
            beginPress();
            this.listener.onPlay(this);
        }
    }

    /**
     * @return true when the point is inside the square control box.
     */
    private static boolean insideBox(float pointX, float pointY, float boxX, float boxY, float size) {
        return pointX >= boxX && pointX <= boxX + size && pointY >= boxY && pointY <= boxY + size;
    }

    /**
     * Moves one control's hover amount toward its target (120 ms in, 240 ms out).
     */
    private static float approach(float current, boolean hovered, float deltaMs) {
        return hovered
                ? Math.min(1.0F, current + deltaMs / 120.0F)
                : Math.max(0.0F, current - deltaMs / 240.0F);
    }

    /**
     * Formats a duration as {@code m:ss}; unknown durations show a placeholder.
     */
    private static String formatTime(double seconds) {
        if (seconds <= 0.0D) {
            return "--:--";
        }
        int total = (int) seconds;
        int minutes = total / 60;
        int rest = total % 60;
        return minutes + ":" + (rest < 10 ? "0" + rest : Integer.toString(rest));
    }
}
