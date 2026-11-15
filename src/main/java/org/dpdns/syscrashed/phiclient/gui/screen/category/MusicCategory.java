package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dpdns.syscrashed.phiclient.gui.component.PhiIconButton;
import org.dpdns.syscrashed.phiclient.gui.component.PhiMusicTabBar;
import org.dpdns.syscrashed.phiclient.gui.component.PhiPlaylistOverlay;
import org.dpdns.syscrashed.phiclient.gui.component.PhiProgressBar;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSearchField;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSongCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiSongList;
import org.dpdns.syscrashed.phiclient.gui.component.PhiVolumeSlider;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.LibraryView;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicLibrary;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicManager;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicPlayer;
import org.dpdns.syscrashed.phiclient.gui.music.PhiPlaylist;
import org.dpdns.syscrashed.phiclient.gui.music.PhiTrack;
import org.dpdns.syscrashed.phiclient.gui.music.PlayMode;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
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

    /** True while the playlist overlay is open. */
    private boolean overlayVisible;

    /** The playlist the management overlay acts on. */
    private String manageTarget = "";

    /** The track the picker overlay adds to a playlist. */
    private String pickTrackId = "";

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

        // One content column for the whole page, anchored at the bottom of the hub.
        float columnX = contentLeft(layout, contentBottom);
        float columnRight = contentRight(layout, contentBottom);
        float columnWidth = columnRight - columnX;

        // Headline row: the title at the left, the search field at the right.
        float controlsHeight = ROW_CONTROLS_HEIGHT * s;
        float searchWidth = Math.min(200.0F * s, columnWidth * 0.42F);
        this.searchField = new PhiSearchField(columnRight - searchWidth, top, searchWidth,
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

        // View tabs.
        float tabY = top + controlsHeight + 12.0F * s;
        this.tabBar = new PhiMusicTabBar(columnX, tabY, columnWidth, controlsHeight, s);
        this.tabBar.setListener(new TabListener());
        this.components.add(this.tabBar);

        // Bottom control bar.
        this.barHeight = BAR_HEIGHT * s;
        this.barY = contentBottom - this.barHeight;
        this.barX = columnX;
        this.barWidth = columnWidth;

        // Song list between the tabs and the control bar.
        float listTop = tabY + controlsHeight + 12.0F * s;
        float listHeight = Math.max(40.0F * s, this.barY - 12.0F * s - listTop);
        this.songList = new PhiSongList(columnX, listTop, columnWidth, listHeight,
                PhiSongCard.ROW_HEIGHT * s, 8.0F * s);
        this.components.add(this.songList);

        buildControlBar(layout);
        this.cardsSignature = "";
        rebuildCards();

        // The overlay sits above the list but below the control bar in z-order of the input:
        // it is only drawn while open and is the only input target then.
        this.overlay = new PhiPlaylistOverlay(columnX + columnWidth * 0.10F, listTop,
                columnWidth * 0.80F, listHeight, s);
        this.overlay.setListener(new OverlayListener());
        this.overlayVisible = false;
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
                buttonSize, s, PhiIcons.PLAY);
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
        // Sync the live state into the widgets.
        this.tabBar.setSelection(this.manager.getView(), this.manager.getPlaylistName());
        this.tabBar.setPlaylists(this.manager.getPlaylists());
        this.volumeSlider.setVolume(this.manager.getVolumePercent());

        rebuildCards();
        syncCardStates();

        // Headline.
        float titleSize = 24.0F * this.uiScale;
        float titleY = layout.getHubY() + layout.getPadding()
                + (ROW_CONTROLS_HEIGHT * this.uiScale - titleSize) * 0.5F;
        nvg.drawText(getDisplayName(), this.songList.getX() + PhiSkew.offset(ROW_CONTROLS_HEIGHT * this.uiScale),
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

        if (this.overlayVisible) {
            this.overlay.draw(nvg, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Draws the bottom control bar: the panel, the current track and the times.
     */
    private void drawControlBar(NanoVGManager nvg) {
        float s = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * s;
        float barPad = 14.0F * s;
        float contentLeft = PhiSkew.contentLeft(this.barX, this.barHeight, barPad);
        float contentRight = PhiSkew.contentRight(this.barX, this.barWidth, barPad);

        nvg.drawSkewRect(this.barX, this.barY, this.barWidth, this.barHeight, radius, PhiTheme.level2());

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
        float textWidth = Math.max(20.0F * s, 150.0F * s);

        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR, textWidth), textX,
                coverY + 2.0F * s, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);
        nvg.drawText(nvg.limitText(artist, bodySize, Fonts.REGULAR, textWidth), textX,
                coverY + titleSize * 1.30F, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // Progress + time labels (the widgets themselves are components, drawn by super.draw).
        this.progressBar.setProgress(this.manager.getProgress());
        this.playButton.setIcon(player.isPlaying() ? PhiIcons.PAUSE : PhiIcons.PLAY);
        this.playButton.setHighlighted(player.isPlaying());
        this.modeButton.setIcon(modeIcon(this.manager.getPlayMode()));
        this.modeButton.setHighlighted(this.manager.getPlayMode() != PlayMode.SEQUENTIAL_LOOP);

        float timeSize = bodySize * 0.95F;
        float timeSlot = nvg.textWidth("00:00", timeSize, Fonts.REGULAR);
        float timeGap = 10.0F * s;
        float trackY = this.progressBar.getY();

        this.progressBar.setX(contentLeft + timeSlot + timeGap);
        this.progressBar.setWidth(Math.max(10.0F, contentRight - contentLeft - (timeSlot + timeGap) * 2.0F));

        nvg.drawTextAligned(formatTime(player.getPositionSeconds()), contentLeft + timeSlot,
                trackY - (timeSize - this.progressBar.getHeight()) * 0.5F,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);
        nvg.drawText(formatTime(player.getDurationSeconds()),
                contentRight - timeSlot, trackY - (timeSize - this.progressBar.getHeight()) * 0.5F,
                PhiTheme.textSecondary(), timeSize, Fonts.REGULAR);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.overlayVisible) {
            this.overlay.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (this.overlayVisible) {
            return;
        }
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.overlayVisible) {
            this.overlay.keyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (this.overlayVisible) {
            return this.overlay.onWheel(notches, mouseX, mouseY);
        }
        return super.onWheel(notches, mouseX, mouseY);
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
     * Opens the picker overlay for a track.
     *
     * @param trackId the track to add.
     */
    private void openPick(String trackId) {
        this.pickTrackId = trackId;
        List<String> names = new ArrayList<String>();
        for (PhiPlaylist playlist : this.manager.getPlaylists()) {
            names.add(playlist.getName());
        }
        this.overlay.showPick(names);
        this.overlayVisible = true;
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
            // Create with a free default name, then open the manager so the user can rename it.
            int suffix = 1;
            String name;
            do {
                name = PhiI18n.tr("music.playlist.default_name", suffix);
                suffix++;
            } while (MusicCategory.this.manager.getLibrary().getPlaylist(name) != null);

            if (MusicCategory.this.manager.createPlaylist(name)) {
                MusicCategory.this.manager.setView(LibraryView.PLAYLIST, name);
                openManage(name);
            }
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
    }

    /** Routes the overlay actions to the facade. */
    private final class OverlayListener implements PhiPlaylistOverlay.Listener {
        @Override
        public void onClose() {
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onApplyName(String name) {
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            String trimmed = name.trim();
            if (MusicCategory.this.overlay.getMode() == PhiPlaylistOverlay.Mode.MANAGE) {
                if (MusicCategory.this.manager.renamePlaylist(MusicCategory.this.manageTarget, trimmed)) {
                    MusicCategory.this.manageTarget = trimmed;
                    MusicCategory.this.overlay.showManage(trimmed,
                            MusicCategory.this.manager.getLibrary().getEffectiveTracks(
                                    LibraryView.PLAYLIST, trimmed));
                }
            } else {
                if (MusicCategory.this.manager.createPlaylist(trimmed)) {
                    MusicCategory.this.manager.addToPlaylist(trimmed, MusicCategory.this.pickTrackId);
                    MusicCategory.this.overlayVisible = false;
                }
            }
        }

        @Override
        public void onDelete() {
            MusicCategory.this.manager.deletePlaylist(MusicCategory.this.manageTarget);
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onPick(String playlistName) {
            MusicCategory.this.manager.addToPlaylist(playlistName, MusicCategory.this.pickTrackId);
            MusicCategory.this.overlayVisible = false;
        }

        @Override
        public void onMove(String trackId, boolean up) {
            MusicCategory.this.manager.moveInPlaylist(MusicCategory.this.manageTarget, trackId, up);
            MusicCategory.this.overlay.rebuildRows(MusicCategory.this.manager.getLibrary()
                    .getEffectiveTracks(LibraryView.PLAYLIST, MusicCategory.this.manageTarget));
        }

        @Override
        public void onRemove(String trackId) {
            MusicCategory.this.manager.removeFromPlaylist(MusicCategory.this.manageTarget, trackId);
            MusicCategory.this.overlay.rebuildRows(MusicCategory.this.manager.getLibrary()
                    .getEffectiveTracks(LibraryView.PLAYLIST, MusicCategory.this.manageTarget));
        }
    }
}
