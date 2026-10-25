package org.dpdns.syscrashed.phiclient.gui.screen.category;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.music.PhiMusicPlayer;

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

        addCard(layout, top, cardHeight, "Music player", "The playback engine is wired up", "Active");

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, "Playlist", "The audio files of the music folder",
                this.player.getTrackCount() + " tracks");

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, "Current track", this.player.getTitle(), this.player.getArtist());

        top += cardHeight + spacing;
        addCard(layout, top, cardHeight, "Music folder", "Put your audio files into this folder",
                this.player.getMusicFolder().getName());
    }
}