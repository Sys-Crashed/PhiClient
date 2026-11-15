package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicManager;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicPlayer;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiIconFeedback;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.lwjgl.nanovg.NanoVG;

/**
 * The music control area of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The area shows the album cover, the title and the artist of the current track, the transport
 * buttons (previous, play / pause, next), the progress bar with the time of the track and the number of
 * tracks of the playlist. All elements use the global shape of the interface.</p>
 *
 * <p><b>Layout rule</b> (the same rule the Soar client uses): the area owns exactly one content
 * column (see {@link PhiCard}). The cover sits at the left edge of the column, the title and the artist
 * behind it, the transport buttons and the button of the repository at the right edge. The three
 * transport buttons are placed symmetrically around their centre and the progress bar runs between the
 * two time labels, so the whole area reads as one grid.</p>
 *
 * <p>The playback is real: the player reads the audio files of the folder {@code PhiClient/music} inside
 * the game directory (see {@link PhiMusicPlayer}). If the folder is empty, the area shows the path of
 * the folder instead of a fake track.</p>
 */
public class PhiMusicControl extends PhiComponent {

    /** Edge length of a transport button. */
    private static final float TRANSPORT_SIZE = 26.0F;

    /** Horizontal distance between two transport buttons. */
    private static final float TRANSPORT_GAP = 6.0F;

    /** Horizontal distance between the cover and the texts. */
    private static final float TEXT_GAP = 12.0F;

    /** Horizontal distance between the texts and the transport buttons. */
    private static final float CONTROL_GAP = 14.0F;

    private final float uiScale;

    /** The shared playback engine (the hub keeps playing after the GUI closes). */
    private final PhiMusicPlayer player = PhiMusicManager.get().getPlayer();

    private final PhiIconButton previousButton;
    private final PhiIconButton playButton;
    private final PhiIconButton nextButton;

    private final PhiProgressBar progressBar;

    /** Play / pause icon morph (Cupertino replace + damped spring), driven by the playing state. */
    private final PhiIconFeedback playFeedback = new PhiIconFeedback(PhiIcons.PLAY, PhiIcons.PAUSE, true);

    /** Adapts the feedback animator to the icon slot of the play button. */
    private final PhiIcon playIcon = new PhiIcon() {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            PhiMusicControl.this.playFeedback.draw(nvg, centerX, centerY, size, color);
        }
    };

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiMusicControl(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);

        this.uiScale = uiScale;

        float padding = PhiMetrics.CARD_PADDING * uiScale;
        float contentLeft = PhiSkew.contentLeft(x, height, padding);
        float contentRight = PhiSkew.contentRight(x, width, padding);

        float coverSize = Math.min(PhiMetrics.COVER_SIZE * uiScale, height - padding * 2.0F);
        float coverY = y + padding * 0.9F;

        // The three transport buttons are placed symmetrically around their centre.
        float transportSize = TRANSPORT_SIZE * uiScale;
        float transportGap = TRANSPORT_GAP * uiScale;
        float transportWidth = transportSize * 3.0F + transportGap * 2.0F;
        float transportX = contentRight - transportWidth;
        float transportY = coverY + (coverSize - transportSize) * 0.5F;

        this.previousButton = new PhiIconButton(transportX, transportY, transportSize, transportSize,
                uiScale, PhiIcons.PREVIOUS);
        this.playButton = new PhiIconButton(transportX + transportSize + transportGap, transportY,
                transportSize, transportSize, uiScale, this.playIcon);
        this.nextButton = new PhiIconButton(transportX + (transportSize + transportGap) * 2.0F, transportY,
                transportSize, transportSize, uiScale, PhiIcons.NEXT);

        this.previousButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                PhiMusicControl.this.player.previous();
            }
        });
        this.playButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                PhiMusicControl.this.player.toggle();
            }
        });
        this.nextButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                PhiMusicControl.this.player.next();
            }
        });

        // The progress bar runs between the two time labels at the bottom of the area.
        float barHeight = PhiMetrics.PROGRESS_BAR_HEIGHT * uiScale;
        float barY = y + height - padding - barHeight;

        this.progressBar = new PhiProgressBar(contentLeft, barY, contentRight - contentLeft, barHeight, uiScale);
        this.progressBar.setSeekListener(new PhiProgressBar.SeekListener() {
            @Override
            public void onSeek(float ratio) {
                PhiMusicControl.this.player.seek(ratio);
            }
        });
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.progressBar.mouseReleased(mouseX, mouseY, mouseButton);
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        // The engine is tick-driven by PhiMusicManager (no per-screen update here).
        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = PhiMetrics.CARD_PADDING * this.uiScale;

        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float timeSize = bodySize * 0.9F;

        // The one content column of the area.
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        // Album cover.
        float coverSize = Math.min(PhiMetrics.COVER_SIZE * this.uiScale, this.height - padding * 2.0F);
        float coverY = this.y + padding * 0.9F;

        nvg.drawRoundedRect(contentLeft, coverY, coverSize, coverSize, radius * 0.6F, PhiTheme.level3());
        PhiIcons.MUSIC.draw(nvg, contentLeft + coverSize * 0.5F, coverY + coverSize * 0.5F,
                coverSize * 0.5F, PhiTheme.accent());

        // Title and artist behind the cover. The column of the texts ends in front of the transport
        // buttons, so the texts can never run into them.
        float transportSize = TRANSPORT_SIZE * this.uiScale;
        float transportGap = TRANSPORT_GAP * this.uiScale;
        float transportWidth = transportSize * 3.0F + transportGap * 2.0F;

        float textX = contentLeft + coverSize + TEXT_GAP * this.uiScale;
        float textRight = contentRight - transportWidth - CONTROL_GAP * this.uiScale;
        float textWidth = Math.max(0.0F, textRight - textX);

        String counter = PhiI18n.tr("music.tracks", this.player.getTrackCount());
        float counterWidth = nvg.textWidth(counter, bodySize, Fonts.REGULAR);

        float titleY = coverY + titleSize * 0.1F;
        float artistY = titleY + titleSize * 1.25F;

        nvg.drawText(nvg.limitText(this.player.getTitle(), titleSize, Fonts.REGULAR,
                Math.max(0.0F, textWidth - counterWidth - bodySize)), textX, titleY,
                PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        nvg.drawText(nvg.limitText(this.player.getArtist(), bodySize, Fonts.REGULAR, textWidth),
                textX, artistY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // The number of tracks of the playlist at the end of the title row.
        nvg.drawTextAligned(counter, textRight, titleY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // The time of the track at the two ends of the progress bar.
        float barHeight = PhiMetrics.PROGRESS_BAR_HEIGHT * this.uiScale;
        float barY = this.y + this.height - padding - barHeight;

        // Both labels get the same slot, so the bar does not move while the track plays.
        float timeSlot = nvg.textWidth("00:00", timeSize, Fonts.REGULAR);
        float timeGap = 10.0F * this.uiScale;

        float barX = contentLeft + timeSlot + timeGap;
        float barRight = contentRight - timeSlot - timeGap;

        this.progressBar.setX(barX);
        this.progressBar.setY(barY);
        this.progressBar.setWidth(Math.max(1.0F, barRight - barX));
        this.progressBar.setHeight(barHeight);

        float timeY = barY - (timeSize - barHeight) * 0.5F - timeSize * 0.08F;

        float duration = this.player.getDurationSeconds();
        float position = this.player.getPositionSeconds();
        float pendingSeek = this.player.getPendingSeekRatio();
        if (pendingSeek >= 0.0F) {
            position = pendingSeek * duration;
        }

        nvg.drawTextAligned(formatTime(position), contentLeft + timeSlot, timeY,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);

        nvg.drawText(formatTime(duration), barRight + timeGap, timeY,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);

        // Transport buttons, the button of the repository and the progress bar.
        this.progressBar.setHeldRatio(pendingSeek);
        this.progressBar.setProgress(this.player.getProgress());
        this.playFeedback.setStateTarget(this.player.isPlaying() ? 1.0F : 0.0F);
        this.playButton.setHighlighted(this.player.isPlaying());

        this.previousButton.draw(nvg, mouseX, mouseY, partialTicks);
        this.playButton.draw(nvg, mouseX, mouseY, partialTicks);
        this.nextButton.draw(nvg, mouseX, mouseY, partialTicks);
        this.progressBar.draw(nvg, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.progressBar.mouseClicked(mouseX, mouseY, mouseButton);
        this.previousButton.mouseClicked(mouseX, mouseY, mouseButton);
        this.playButton.mouseClicked(mouseX, mouseY, mouseButton);
        this.nextButton.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Formats a time in seconds as {@code m:ss}.
     *
     * @param seconds the time in seconds.
     * @return the formatted time.
     */
    private static String formatTime(float seconds) {
        int total = Math.max(0, (int) seconds);
        int minutes = total / 60;
        int rest = total % 60;

        return minutes + ":" + (rest < 10 ? "0" + rest : Integer.toString(rest));
    }
}
