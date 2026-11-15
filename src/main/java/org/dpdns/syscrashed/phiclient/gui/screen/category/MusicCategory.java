package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.component.PhiComponent;
import org.dpdns.syscrashed.phiclient.gui.component.PhiConfirmOverlay;
import org.dpdns.syscrashed.phiclient.gui.component.PhiIconButton;
import org.dpdns.syscrashed.phiclient.gui.component.PhiMusicTabBar;
import org.dpdns.syscrashed.phiclient.gui.component.PhiPlaylistOverlay;
import org.dpdns.syscrashed.phiclient.gui.component.PhiProgressBar;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSearchField;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSongCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSongList;
import org.dpdns.syscrashed.phiclient.gui.component.PhiVolumeSlider;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.LibraryView;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicImporter;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicLibrary;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicManager;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicPlayer;
import org.dpdns.syscrashed.phiclient.gui.music.PhiPlaylist;
import org.dpdns.syscrashed.phiclient.gui.music.PhiTrack;
import org.dpdns.syscrashed.phiclient.gui.music.PlayMode;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiIconFeedback;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiTooltip;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.lwjgl.nanovg.NanoVG;

/**
 * The music page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page is the full local music player: a headline with a search field, the view tabs
 * (All / Favorite / the named playlists), the inertia-scrolled song cards and the bottom control bar
 * (current track, transport, seekable progress bar, volume, repeat mode, import and open-folder).
 * All state lives in the {@link PhiMusicManager} singleton - the page only reads snapshots and calls
 * the facade, so the playback continues when the hub closes.</p>
 *
 * <p>Geometry: the whole page shares one content column anchored at the bottom of the hub; the song
 * rows keep their x anchor while scrolling. The cards and the panel are parallelograms; the small
 * controls of the control bar are upright rounded rectangles with axis aligned hit boxes.</p>
 *
 * <p>The playlist overlay (manage / pick) covers the song list but never the control bar; while it is
 * open, it is the only component that receives input.</p>
 */
public class MusicCategory extends PhiCategory {

    /** Height of the bottom control bar in design pixels. */
    private static final float BAR_HEIGHT = 96.0F;

    /** Height of the search field and of the tab bar. */
    private static final float ROW_CONTROLS_HEIGHT = 30.0F;

    /** Share of the hub area the playlist dialog covers (the dialog is self-adaptive). */
    private static final float DIALOG_AREA_RATIO = 0.35F;

    private final PhiMusicManager manager = PhiMusicManager.get();

    private PhiSearchField searchField;
    private PhiMusicTabBar tabBar;
    private PhiSongList songList;

    private PhiIconButton previousButton;
    private PhiIconButton playButton;
    private PhiIconButton nextButton;
    private PhiIconButton modeButton;
    private PhiIconButton importButton;
    private PhiIconButton folderButton;
    private PhiProgressBar progressBar;
    private PhiVolumeSlider volumeSlider;

    private PhiPlaylistOverlay overlay;

    private PhiConfirmOverlay confirmOverlay;

    private PhiIconButton playListButton;

    /** True while the confirmation overlay is open. */
    private boolean confirmVisible;

    /** The track the confirmation overlay deletes. */
    private String pendingDeleteTrackId = "";

    /** The import collision the confirmation overlay decides (null when there is none). */
    private PhiMusicImporter.ImportConflict pendingConflict;

    /** The tooltip of the hovered song (the row itself truncates the title). */
    private final PhiTooltip songTooltip = new PhiTooltip();

    /** The anchor of the song tooltip (it keeps its place while it fades out). */
    private float tooltipAnchorX;
    private float tooltipAnchorY;

    /** Play / pause icon morph (Cupertino replace + damped spring), driven by the playing state. */
    private final PhiIconFeedback playFeedback = new PhiIconFeedback(PhiIcons.PLAY, PhiIcons.PAUSE, true);

    /** Adapts the feedback animator to the icon slot of the play button. */
    private final PhiIcon playIcon = new PhiIcon() {
        @Override
        public void draw(NanoVGManager nvg, float centerX, float centerY, float size, int color) {
            MusicCategory.this.playFeedback.draw(nvg, centerX, centerY, size, color);
        }
    };

    /** True while the playlist overlay is open. */
    private boolean overlayVisible;

    /** Animated visibility of the two overlays (they keep drawing while fading out). */
    private float overlayVisibility;
    private float confirmVisibility;
    private long lastFrame = System.currentTimeMillis();

    /** The playlist the management overlay acts on. */
    private String manageTarget = "";

    /** The playlist the confirmation overlay deletes (empty when the overlay confirms a track). */
    private String pendingDeletePlaylist = "";

    /** The current search filter. */
    private String searchText = "";

    /** The signature of the visible track list (the cards are rebuilt only when it changes). */
    private String cardsSignature = "";

    /** The cached rows, keyed by the track id. */
    private final List<PhiSongCard> cards = new ArrayList<PhiSongCard>();

    /** The bar geometry of the last layout (the draw pass needs it). */
    private float barX;
    private float barY;
    private float barWidth;
    private float barHeight;
    private float uiScale = 1.0F;

    /**
     * Creates the music page.
     */
    public MusicCategory() {
        super("Music", PhiIcons.MUSIC);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        this.uiScale = layout.getUiScale();
        float s = this.uiScale;
        float padding = layout.getPadding();
        float top = layout.getHubY() + padding;
        float contentBottom = layout.getHubY() + layout.getHubHeight() - padding;

        // Soar-style grid: every row measures its own content rectangle at its OWN height. The
        // left edge of the hub leans to the right, so a row near the top starts further right than
        // a row near the bottom - a single bottom-anchored column would push the top rows into the
        // navigation band.
        float controlsHeight = ROW_CONTROLS_HEIGHT * s;

        // Headline row: the title at the left, the search field at the right.
        float titleRowBottom = top + controlsHeight;
        float titleRowRight = contentRight(layout, titleRowBottom);
        float searchWidth = Math.min(200.0F * s, (titleRowRight - contentLeft(layout, titleRowBottom)) * 0.42F);
        this.searchField = new PhiSearchField(titleRowRight - searchWidth, top, searchWidth,
                controlsHeight, s);
        this.searchField.setPlaceholder(PhiI18n.get("music.search"));
        this.searchField.setText(this.searchText);
        this.searchField.setChangeListener(new PhiSearchField.ChangeListener() {
            @Override
            public void onChanged(String text) {
                MusicCategory.this.searchText = text != null ? text : "";
            }
        });
        this.components.add(this.searchField);

        // View tabs (their own row, anchored at the bottom of the row).
        float tabY = titleRowBottom + 12.0F * s;
        float tabBottom = tabY + controlsHeight;
        float tabX = contentLeft(layout, tabBottom);
        float tabRight = contentRight(layout, tabBottom);

        // The play button of the current view: selecting a playlist only previews it, the play
        // button starts it explicitly.
        float playButtonSize = controlsHeight;
        float playButtonX = tabRight - playButtonSize;
        this.tabBar = new PhiMusicTabBar(tabX, tabY, playButtonX - 8.0F * s - tabX, controlsHeight, s);
        this.tabBar.setListener(new TabListener());
        this.components.add(this.tabBar);

        this.playListButton = new PhiIconButton(playButtonX, tabY, playButtonSize, playButtonSize, s,
                PhiIcons.PLAY_LIST);
        this.playListButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.playView(MusicCategory.this.manager.getView(),
                        MusicCategory.this.manager.getPlaylistName());
            }
        });
        this.components.add(this.playListButton);

        // Bottom control bar: a parallelogram anchored at the bottom of the hub (its left edge
        // follows the hub edge, like the cards of the home page).
        this.barHeight = BAR_HEIGHT * s;
        this.barY = contentBottom - this.barHeight;
        this.barX = contentLeft(layout, contentBottom);
        this.barWidth = contentRight(layout, contentBottom) - this.barX;

        // Song list: one upright content rectangle. The left x is measured at the TOP of the list
        // (the largest left edge), the right x at the TOP as well - both slanted edges are parallel,
        // so the distance between them is the same at every height. Measuring the right edge at the
        // bottom of the viewport instead would cut the rectangle short by SLOPE * listHeight and
        // push every row away from the right edge of the hub (the rows would no longer line up).
        float listTop = tabBottom + 12.0F * s;
        float listHeight = Math.max(40.0F * s, this.barY - 12.0F * s - listTop);
        float listX = contentLeft(layout, listTop);
        float listRight = contentRight(layout, listTop);
        this.songList = new PhiSongList(listX, listTop, listRight - listX, listHeight,
                PhiMetrics.SONG_ROW_HEIGHT * s, PhiMetrics.SONG_ROW_SPACING * s);
        this.components.add(this.songList);

        buildControlBar(layout);
        this.cardsSignature = "";
        rebuildCards();

        // The playlist dialog floats above the whole hub, centered on its geometric centre and sized
        // to a fixed share of the hub area (self-adaptive with the window).
        float hubW = layout.getHubWidth();
        float hubH = layout.getHubHeight();
        float ratio = (float) Math.sqrt(DIALOG_AREA_RATIO);
        float dialogWidth = Math.max(280.0F * s, hubW * ratio);
        float dialogHeight = Math.max(200.0F * s, hubH * ratio);
        float hubCenterX = layout.getHubX() + hubW * 0.5F + PhiSkew.offset(hubH) * 0.5F;
        float hubCenterY = layout.getHubY() + hubH * 0.5F;
        float dialogX = hubCenterX - dialogWidth * 0.5F - PhiSkew.offset(dialogHeight) * 0.5F;
        float dialogY = hubCenterY - dialogHeight * 0.5F;
        this.overlay = new PhiPlaylistOverlay(dialogX, dialogY, dialogWidth, dialogHeight, s);
        this.overlay.setListener(new OverlayListener());
        this.overlayVisible = false;

        // The confirmation panel sits in the middle of the hub.
        float confirmWidth = 340.0F * s;
        float confirmHeight = 140.0F * s;
        float confirmX = hubCenterX - confirmWidth * 0.5F - PhiSkew.offset(confirmHeight) * 0.5F;
        float confirmY = hubCenterY - confirmHeight * 0.5F;
        this.confirmOverlay = new PhiConfirmOverlay(confirmX, confirmY, confirmWidth, confirmHeight, s);
        this.confirmOverlay.setListener(new ConfirmListener());
        this.confirmVisible = false;
    }

    /**
     * Builds the widgets of the bottom control bar.
     */
    private void buildControlBar(PhiHubLayout layout) {
        float s = this.uiScale;
        float barPad = 14.0F * s;
        float contentLeft = PhiSkew.contentLeft(this.barX, this.barHeight, barPad);
        float contentRight = PhiSkew.contentRight(this.barX, this.barWidth, barPad);

        float buttonSize = 26.0F * s;
        float gap = 8.0F * s;
        float upperY = this.barY + barPad * 0.6F;

        // Right group: volume slider, repeat mode, import, open folder.
        float volumeWidth = 104.0F * s;
        float rightX = contentRight - buttonSize; // folder is the last
        this.folderButton = new PhiIconButton(rightX, upperY, buttonSize, buttonSize, s, PhiIcons.FOLDER);
        this.folderButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.openMusicFolder();
            }
        });

        rightX -= buttonSize + gap;
        this.importButton = new PhiIconButton(rightX, upperY, buttonSize, buttonSize, s, PhiIcons.PLUS);
        this.importButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.importViaDialog();
            }
        });

        rightX -= buttonSize + gap;
        this.modeButton = new PhiIconButton(rightX, upperY, buttonSize, buttonSize, s, PhiIcons.REPEAT);
        this.modeButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                cyclePlayMode();
            }
        });

        this.volumeSlider = new PhiVolumeSlider(rightX - gap - volumeWidth, upperY, volumeWidth,
                buttonSize, s);
        this.volumeSlider.setVolume(this.manager.getVolumePercent());
        this.volumeSlider.setChangeListener(new PhiVolumeSlider.ChangeListener() {
            @Override
            public void onChanged(int percent) {
                MusicCategory.this.manager.setVolume(percent);
            }
        });

        // Transport, centered in the space between the track info and the right group.
        float transportWidth = buttonSize * 3.0F + gap * 2.0F;
        float infoRight = contentLeft + PhiMetrics.COVER_SIZE * s + 12.0F * s + 120.0F * s;
        float rightGroupLeft = this.volumeSlider.getX() - gap;
        float transportX = Math.max(infoRight,
                (infoRight + rightGroupLeft - transportWidth) * 0.5F);

        this.previousButton = new PhiIconButton(transportX, upperY, buttonSize, buttonSize, s,
                PhiIcons.PREVIOUS);
        this.previousButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.previous();
            }
        });
        this.playButton = new PhiIconButton(transportX + buttonSize + gap, upperY, buttonSize,
                buttonSize, s, this.playIcon);
        this.playButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.toggle();
            }
        });
        this.nextButton = new PhiIconButton(transportX + (buttonSize + gap) * 2.0F, upperY,
                buttonSize, buttonSize, s, PhiIcons.NEXT);
        this.nextButton.setAction(new PhiIconButton.Action() {
            @Override
            public void onClicked() {
                MusicCategory.this.manager.next();
            }
        });

        // Seekable progress bar between the two time labels at the bottom of the bar.
        float barTrackHeight = PhiMetrics.PROGRESS_BAR_HEIGHT * s;
        float barTrackY = this.barY + this.barHeight - barPad - barTrackHeight;
        this.progressBar = new PhiProgressBar(contentLeft, barTrackY, contentRight - contentLeft,
                barTrackHeight, s);
        this.progressBar.setSeekListener(new PhiProgressBar.SeekListener() {
            @Override
            public void onSeek(float ratio) {
                MusicCategory.this.manager.seek(ratio);
            }
        });

        this.components.add(this.previousButton);
        this.components.add(this.playButton);
        this.components.add(this.nextButton);
        this.components.add(this.modeButton);
        this.components.add(this.importButton);
        this.components.add(this.folderButton);
        this.components.add(this.volumeSlider);
        this.components.add(this.progressBar);
    }

    @Override
    public void draw(NanoVGManager nvg, PhiHubLayout layout, int mouseX, int mouseY, float partialTicks) {
        checkImportConflicts();

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        this.overlayVisibility = this.overlayVisible
                ? Math.min(1.0F, this.overlayVisibility + deltaMs / 160.0F)
                : Math.max(0.0F, this.overlayVisibility - deltaMs / 140.0F);
        this.confirmVisibility = this.confirmVisible
                ? Math.min(1.0F, this.confirmVisibility + deltaMs / 160.0F)
                : Math.max(0.0F, this.confirmVisibility - deltaMs / 140.0F);

        // Sync the live state into the widgets.
        this.tabBar.setSelection(this.manager.getView(), this.manager.getPlaylistName());
        this.tabBar.setPlaylists(this.manager.getPlaylists());
        this.volumeSlider.setVolume(this.manager.getVolumePercent());

        rebuildCards();
        syncCardStates();

        // The bar panel is drawn BEFORE the components, so the transport buttons and the sliders
        // (drawn by super.draw) land on top of it instead of being covered by it.
        nvg.drawSkewRect(this.barX, this.barY, this.barWidth, this.barHeight,
                PhiMetrics.CORNER_RADIUS * this.uiScale, PhiTheme.level2());

        // Headline, measured at the bottom of its own row (never at the bottom of the page).
        float titleSize = 24.0F * this.uiScale;
        float titleRowBottom = layout.getHubY() + layout.getPadding() + ROW_CONTROLS_HEIGHT * this.uiScale;
        float titleY = layout.getHubY() + layout.getPadding()
                + (ROW_CONTROLS_HEIGHT * this.uiScale - titleSize) * 0.5F;
        nvg.drawText(getDisplayName(), contentLeft(layout, titleRowBottom),
                titleY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        super.draw(nvg, layout, mouseX, mouseY, partialTicks);

        drawControlBar(nvg);

        // Empty state of the list.
        if (this.cards.isEmpty()) {
            String empty = this.manager.getTracks().isEmpty()
                    ? PhiI18n.get("music.empty") : PhiI18n.get("music.search.empty");
            float textSize = PhiMetrics.FONT_BODY * this.uiScale;
            nvg.drawTextCentered(empty, this.songList.getX() + this.songList.getWidth() * 0.5F,
                    this.songList.getY() + this.songList.getHeight() * 0.4F,
                    PhiTheme.textSecondary(), textSize, Fonts.REGULAR);
        }

        drawSongTooltip(nvg, layout, partialTicks);
    }

    @Override
    public boolean hasModalOverlay() {
        return this.overlayVisible || this.confirmVisible
                || this.overlayVisibility > 0.001F || this.confirmVisibility > 0.001F;
    }

    @Override
    public void drawOverlayLayer(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        if (this.overlayVisible || this.overlayVisibility > 0.001F) {
            drawOverlayFade(nvg, this.overlay, this.overlayVisibility, mouseX, mouseY, partialTicks);
        }
        if (this.confirmVisible || this.confirmVisibility > 0.001F) {
            drawOverlayFade(nvg, this.confirmOverlay, this.confirmVisibility, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Draws an overlay with a short alpha fade only. No scale is applied: a scale transform shifts the
     * drawn geometry away from the un-transformed pointer position, which is what used to make the
     * hover highlight land next to the cursor.
     */
    private static void drawOverlayFade(NanoVGManager nvg, PhiComponent overlay, float visibility,
                                        int mouseX, int mouseY, float partialTicks) {
        float value = Math.max(0.0F, Math.min(1.0F, visibility));
        float inverse = 1.0F - value;
        float eased = 1.0F - inverse * inverse * inverse;

        nvg.save();
        nvg.setGlobalAlpha(eased);
        overlay.draw(nvg, mouseX, mouseY, partialTicks);
        nvg.restore();
    }

    /**
     * Draws the tooltip of the hovered song: the row truncates a long title, the tooltip shows it
     * in full. The tooltip clamps itself against the game window, so it never leaves the screen.
     */
    private void drawSongTooltip(NanoVGManager nvg, PhiHubLayout layout, float partialTicks) {
        float deltaMs = partialTicks * 50.0F;
        PhiSongCard hovered = null;

        if (!this.overlayVisible && !this.confirmVisible
                && this.overlayVisibility <= 0.001F && this.confirmVisibility <= 0.001F) {
            for (PhiSongCard card : this.cards) {
                if (card.isHovered()) {
                    hovered = card;
                    break;
                }
            }
        }

        if (hovered != null) {
            String artist = hovered.getTrack().getArtist() != null
                    ? hovered.getTrack().getArtist() : PhiI18n.get("music.local");
            this.tooltipAnchorX = hovered.getX() + hovered.getWidth() * 0.5F;
            this.tooltipAnchorY = hovered.getY() + hovered.getHeight() * 0.5F;
            this.songTooltip.update(hovered.getTrack().getTitle() + " - " + artist, true, deltaMs);
        } else {
            this.songTooltip.update("", false, deltaMs);
        }

        if (this.songTooltip.isVisible()) {
            this.songTooltip.draw(nvg, this.tooltipAnchorX, this.tooltipAnchorY,
                    layout.getDisplayWidth(), layout.getDisplayHeight(), this.uiScale);
        }
    }

    /**
     * Draws the content of the bottom control bar: the current track and the times. The panel
     * itself is drawn earlier (before the components), the buttons are components.
     */
    private void drawControlBar(NanoVGManager nvg) {
        float s = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * s;
        float barPad = 14.0F * s;
        float contentLeft = PhiSkew.contentLeft(this.barX, this.barHeight, barPad);
        float contentRight = PhiSkew.contentRight(this.barX, this.barWidth, barPad);

        PhiMusicPlayer player = this.manager.getPlayer();

        // Cover and the current track info.
        float coverSize = PhiMetrics.COVER_SIZE * s;
        float coverY = this.barY + barPad * 0.6F;
        nvg.drawRoundedRect(contentLeft, coverY, coverSize, coverSize, radius * 0.6F, PhiTheme.level3());
        PhiIcons.MUSIC.draw(nvg, contentLeft + coverSize * 0.5F, coverY + coverSize * 0.5F,
                coverSize * 0.5F, PhiTheme.accent());

        PhiTrack current = currentLibraryTrack();
        String title = current != null ? current.getTitle() : player.getTitle();
        String artist = current != null && current.getArtist() != null
                ? current.getArtist() : player.getArtist();

        float titleSize = PhiMetrics.FONT_TITLE * s * 0.92F;
        float bodySize = PhiMetrics.FONT_BODY * s * 0.92F;
        float textX = contentLeft + coverSize + 12.0F * s;
        // The texts are clipped before the transport buttons, so they can never run into them.
        float textWidth = Math.max(20.0F * s, Math.min(150.0F * s, this.playButton.getX() - textX - 10.0F * s));

        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR, textWidth), textX,
                coverY + 2.0F * s, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);
        nvg.drawText(nvg.limitText(artist, bodySize, Fonts.REGULAR, textWidth), textX,
                coverY + titleSize * 1.30F, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // Progress + time labels (the widgets themselves are components, drawn by super.draw).
        // While a seek is pending the bar keeps showing the requested position, so the time label
        // has to show the same position - otherwise the bar and the clock disagree.
        float pendingSeek = this.manager.getPendingSeekRatio();
        this.progressBar.setHeldRatio(pendingSeek);
        this.progressBar.setProgress(this.manager.getProgress());
        this.playFeedback.setStateTarget(player.isPlaying() ? 1.0F : 0.0F);
        this.playButton.setHighlighted(player.isPlaying());
        this.modeButton.setIcon(modeIcon(this.manager.getPlayMode()));
        this.modeButton.setHighlighted(this.manager.getPlayMode() != PlayMode.SEQUENTIAL_LOOP);

        float timeSize = bodySize * 0.95F;
        float timeSlot = nvg.textWidth("00:00", timeSize, Fonts.REGULAR);
        float timeGap = 10.0F * s;
        float trackY = this.progressBar.getY();

        this.progressBar.setX(contentLeft + timeSlot + timeGap);
        this.progressBar.setWidth(Math.max(10.0F, contentRight - contentLeft - (timeSlot + timeGap) * 2.0F));

        float duration = player.getDurationSeconds();
        float position = player.getPositionSeconds();
        if (pendingSeek >= 0.0F) {
            position = pendingSeek * duration;
        }

        nvg.drawTextAligned(formatTime(position), contentLeft + timeSlot,
                trackY - (timeSize - this.progressBar.getHeight()) * 0.5F,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);
        nvg.drawText(formatTime(duration),
                contentRight - timeSlot, trackY - (timeSize - this.progressBar.getHeight()) * 0.5F,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.confirmVisible) {
            this.confirmOverlay.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (this.overlayVisible) {
            this.overlay.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (this.confirmVisible || this.overlayVisible) {
            return;
        }
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.confirmVisible) {
            this.confirmOverlay.keyTyped(typedChar, keyCode);
            return;
        }
        if (this.overlayVisible) {
            this.overlay.keyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (this.confirmVisible) {
            return true;
        }
        if (this.overlayVisible) {
            return this.overlay.onWheel(notches, mouseX, mouseY);
        }
        return super.onWheel(notches, mouseX, mouseY);
    }

    /**
     * Asks the user about a pending import collision (overwrite or cancel). The decision is applied
     * on the client thread, so the collision is published by the importer and consumed here.
     */
    private void checkImportConflicts() {
        if (this.confirmVisible || this.pendingConflict != null || !this.manager.hasPendingImportConflicts()) {
            return;
        }

        PhiMusicImporter.ImportConflict conflict = this.manager.pollNextImportConflict();
        if (conflict == null) {
            return;
        }

        this.pendingConflict = conflict;
        this.confirmOverlay.show(PhiI18n.get("music.import.title"),
                PhiI18n.tr("music.import.conflict", conflict.getTargetName()),
                PhiI18n.get("music.import.conflict.overwrite"), false);
        this.confirmVisible = true;
    }

    /**
     * Rebuilds the song cards when the view, the filter or the library changed.
     */
    private void rebuildCards() {
        List<PhiTrack> visible = visibleTracks();
        StringBuilder signature = new StringBuilder();
        for (PhiTrack track : visible) {
            signature.append(track.getId()).append(';');
        }
        String current = signature.toString();
        if (current.equals(this.cardsSignature)) {
            return;
        }
        this.cardsSignature = current;

        this.cards.clear();
        float width = this.songList.getWidth();
        for (PhiTrack track : visible) {
            PhiSongCard card = new PhiSongCard(track, this.songList.getX(), 0.0F, width, this.uiScale);
            card.setActionListener(new SongListener());
            this.cards.add(card);
        }
        this.songList.setCards(this.cards);
    }

    /**
     * Syncs the favorite / playing flags into the rows.
     */
    private void syncCardStates() {
        File currentFile = this.manager.getPlayer().getCurrentTrack();
        String currentId = currentFile != null
                ? PhiMusicLibrary.toId(this.manager.getMusicFolder(), currentFile) : "";
        boolean playing = this.manager.getPlayer().isPlaying();

        for (PhiSongCard card : this.cards) {
            String id = card.getTrack().getId();
            card.setFavorite(this.manager.isFavorite(id));
            card.setPlaying(playing && id.equals(currentId));
        }
    }

    /**
     * @return the tracks of the current view filtered by the search text (title or artist).
     */
    private List<PhiTrack> visibleTracks() {
        List<PhiTrack> tracks = this.manager.getTracks();
        String filter = this.searchText.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            return tracks;
        }
        List<PhiTrack> filtered = new ArrayList<PhiTrack>();
        for (PhiTrack track : tracks) {
            String artist = track.getArtist() != null ? track.getArtist() : "";
            if (track.getTitle().toLowerCase(Locale.ROOT).contains(filter)
                    || artist.toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(track);
            }
        }
        return filtered;
    }

    /**
     * @return the library track of the currently playing file (null when unknown).
     */
    private PhiTrack currentLibraryTrack() {
        File current = this.manager.getPlayer().getCurrentTrack();
        if (current == null) {
            return null;
        }
        return this.manager.getLibrary().getTrack(
                PhiMusicLibrary.toId(this.manager.getMusicFolder(), current));
    }

    /**
     * Cycles the repeat mode: sequential loop, pseudo random, single loop.
     */
    private void cyclePlayMode() {
        PlayMode current = this.manager.getPlayMode();
        PlayMode next;
        switch (current) {
            case PSEUDO_RANDOM:
                next = PlayMode.SINGLE_LOOP;
                break;
            case SINGLE_LOOP:
                next = PlayMode.SEQUENTIAL_LOOP;
                break;
            case SEQUENTIAL_LOOP:
            default:
                next = PlayMode.PSEUDO_RANDOM;
                break;
        }
        this.manager.setPlayMode(next);
    }

    /**
     * @param mode a repeat mode.
     * @return the icon of the mode button.
     */
    private static PhiIcons modeIcon(PlayMode mode) {
        switch (mode) {
            case PSEUDO_RANDOM:
                return PhiIcons.SHUFFLE;
            case SINGLE_LOOP:
                return PhiIcons.REPEAT_ONE;
            case SEQUENTIAL_LOOP:
            default:
                return PhiIcons.REPEAT;
        }
    }

    /**
     * Opens the management overlay for a playlist.
     *
     * @param name the playlist name.
     */
    private void openManage(String name) {
        this.manageTarget = name;
        this.overlay.showManage(name, this.manager.getLibrary().getEffectiveTracks(
                LibraryView.PLAYLIST, name));
        this.overlayVisible = true;
    }

    /**
     * Opens the create dialog: a name field and a checklist of every library track.
     */
    private void openCreate() {
        this.overlay.showCreate(this.manager.getLibrary().getAllTracks());
        this.overlayVisible = true;
    }

    /**
     * Opens the add dialog for a track.
     *
     * @param trackId the track to add.
     */
    private void openPick(String trackId) {
        List<String> names = new ArrayList<String>();
        for (PhiPlaylist playlist : this.manager.getPlaylists()) {
            names.add(playlist.getName());
        }
        this.overlay.showAdd(names, trackId);
        this.overlayVisible = true;
    }

    /**
     * Shows a toast (used for validation feedback while a dialog stays open).
     */
    private void showToast(String title, String message) {
        PhiClient.getInstance().getGuiManager().getToast().show(title, message);
    }

    /**
     * Formats a time in seconds as {@code m:ss}.
     */
    private static String formatTime(float seconds) {
        int total = Math.max(0, (int) seconds);
        int minutes = total / 60;
        int rest = total % 60;
        return minutes + ":" + (rest < 10 ? "0" + rest : Integer.toString(rest));
    }

    /** Routes the tab actions to the facade. */
    private final class TabListener implements PhiMusicTabBar.Listener {
        @Override
        public void onSelectAll() {
            MusicCategory.this.manager.setView(LibraryView.ALL, "");
        }

        @Override
        public void onSelectFavorite() {
            MusicCategory.this.manager.setView(LibraryView.FAVORITE, "");
        }

        @Override
        public void onSelectPlaylist(String name) {
            MusicCategory.this.manager.setView(LibraryView.PLAYLIST, name);
        }

        @Override
        public void onCreatePlaylist() {
            // The create dialog applies the name only when its confirm button is pressed.
            openCreate();
        }

        @Override
        public void onManagePlaylist(String name) {
            openManage(name);
        }
    }

    /** Routes the song card actions to the facade. */
    private final class SongListener implements PhiSongCard.ActionListener {
        @Override
        public void onPlay(PhiSongCard card) {
            List<PhiTrack> tracks = MusicCategory.this.manager.getTracks();
            String id = card.getTrack().getId();
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).getId().equals(id)) {
                    MusicCategory.this.manager.getPlayer().playIndex(i);
                    return;
                }
            }
        }

        @Override
        public void onFavorite(PhiSongCard card) {
            String id = card.getTrack().getId();
            MusicCategory.this.manager.setFavorite(id, !MusicCategory.this.manager.isFavorite(id));
        }

        @Override
        public void onAddToPlaylist(PhiSongCard card) {
            openPick(card.getTrack().getId());
        }

        @Override
        public void onDelete(PhiSongCard card) {
            MusicCategory.this.pendingDeleteTrackId = card.getTrack().getId();
            MusicCategory.this.confirmOverlay.show(PhiI18n.get("music.track.delete.title"),
                    PhiI18n.tr("music.track.delete.message", card.getTrack().getTitle()),
                    PhiI18n.get("music.track.delete"), true);
            MusicCategory.this.confirmVisible = true;
        }
    }

    /** Routes the confirmation panel to the facade. */
    private final class ConfirmListener implements PhiConfirmOverlay.Listener {
        @Override
        public void onConfirm() {
            MusicCategory.this.confirmVisible = false;

            if (MusicCategory.this.pendingConflict != null) {
                PhiMusicImporter.ImportConflict conflict = MusicCategory.this.pendingConflict;
                MusicCategory.this.pendingConflict = null;
                MusicCategory.this.manager.resolveImportConflict(conflict, true);
                return;
            }

            String playlistName = MusicCategory.this.pendingDeletePlaylist;
            MusicCategory.this.pendingDeletePlaylist = "";
            if (!playlistName.isEmpty()) {
                MusicCategory.this.manager.deletePlaylist(playlistName);
                MusicCategory.this.overlayVisible = false;
                return;
            }

            String id = MusicCategory.this.pendingDeleteTrackId;
            MusicCategory.this.pendingDeleteTrackId = "";
            if (!id.isEmpty()) {
                MusicCategory.this.manager.deleteTrack(id);
                MusicCategory.this.rebuildCards();
            }
        }

        @Override
        public void onCancel() {
            MusicCategory.this.confirmVisible = false;

            if (MusicCategory.this.pendingConflict != null) {
                PhiMusicImporter.ImportConflict conflict = MusicCategory.this.pendingConflict;
                MusicCategory.this.pendingConflict = null;
                MusicCategory.this.manager.resolveImportConflict(conflict, false);
                return;
            }

            MusicCategory.this.pendingDeletePlaylist = "";
            MusicCategory.this.pendingDeleteTrackId = "";
        }
    }

    /** Routes the overlay actions to the facade. */
    private final class OverlayListener implements PhiPlaylistOverlay.Listener {
        @Override
        public void onClose() {
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onCreate(String name, List<String> trackIds) {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                showToast(PhiI18n.get("music.playlist.new"), PhiI18n.get("music.playlist.name_empty"));
                return;
            }
            if (MusicCategory.this.manager.getLibrary().getPlaylist(trimmed) != null) {
                showToast(PhiI18n.get("music.playlist.new"), PhiI18n.get("music.playlist.name_duplicate"));
                return;
            }
            if (!MusicCategory.this.manager.createPlaylist(trimmed)) {
                return;
            }
            if (trackIds != null) {
                for (String trackId : trackIds) {
                    MusicCategory.this.manager.addToPlaylist(trimmed, trackId);
                }
            }
            MusicCategory.this.manager.setView(LibraryView.PLAYLIST, trimmed);
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onAddToPlaylist(String playlistName, String trackId) {
            if (playlistName == null || playlistName.isEmpty()) {
                showToast(PhiI18n.get("music.playlist.add"), PhiI18n.get("music.playlist.none_selected"));
                return;
            }
            MusicCategory.this.manager.addToPlaylist(playlistName, trackId);
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onRename(String oldName, String newName) {
            String trimmed = newName == null ? "" : newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(oldName)) {
                return;
            }
            if (MusicCategory.this.manager.getLibrary().getPlaylist(trimmed) != null) {
                showToast(PhiI18n.get("music.playlist.manage"), PhiI18n.get("music.playlist.name_duplicate"));
                return;
            }
            if (MusicCategory.this.manager.renamePlaylist(oldName, trimmed)) {
                MusicCategory.this.manageTarget = trimmed;
                MusicCategory.this.overlay.showManage(trimmed,
                        MusicCategory.this.manager.getLibrary().getEffectiveTracks(
                                LibraryView.PLAYLIST, trimmed));
            }
        }

        @Override
        public void onDeletePlaylist(String name) {
            MusicCategory.this.pendingDeletePlaylist = name;
            MusicCategory.this.confirmOverlay.show(PhiI18n.get("music.playlist.delete.title"),
                    PhiI18n.tr("music.playlist.delete.message", name),
                    PhiI18n.get("music.playlist.delete"), true);
            MusicCategory.this.confirmVisible = true;
        }

        @Override
        public void onRemoveFromPlaylist(String playlistName, String trackId) {
            MusicCategory.this.manager.removeFromPlaylist(playlistName, trackId);
            MusicCategory.this.overlay.rebuildMembers(MusicCategory.this.manager.getLibrary()
                    .getEffectiveTracks(LibraryView.PLAYLIST, playlistName));
        }
    }
}
