package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicPlayer;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The music page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>The page shows the player and the playlist of the local music: the player reads the audio files of
 * the folder {@code PhiClient/music} inside the game directory (see {@link PhiMusicPlayer}), so the
 * number of tracks and the current track are real. The search field and the category tabs of the design
 * specification are implemented in a later stage.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #layout(PhiHubLayout)} - builds the cards of the page.</li>
 * </ul>
 */
public class MusicCategory extends PhiCategory {

    private final PhiMusicPlayer player = new PhiMusicPlayer();

    /**
     * Creates the music page.
     */
    public MusicCategory() {
        super("Music", PhiIcons.MUSIC);
    }

    @Override
    public void layout(PhiHubLayout layout) {
        clearComponents();

        float cardHeight = PhiMetrics.CARD_HEIGHT * layout.getUiScale();
        float spacing = PhiMetrics.CARD_SPACING * layout.getUiScale();
        float top = layout.getHubY() + layout.getPadding();

        this.player.reload();

        addCard(layout, top, cardHeight, PhiI18n.get("music.player.title"),
                PhiI18n.get("music.player.desc"), PhiI18n.get("status.active"));

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, PhiI18n.get("music.playlist.title"),
                PhiI18n.get("music.playlist.desc"),
                PhiI18n.tr("music.tracks", this.player.getTrackCount()));

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, PhiI18n.get("music.current.title"),
                this.player.getTitle(), this.player.getArtist());

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, PhiI18n.get("music.folder.title"),
                PhiI18n.get("music.folder.desc"), this.player.getMusicFolder().getName());
    }
}