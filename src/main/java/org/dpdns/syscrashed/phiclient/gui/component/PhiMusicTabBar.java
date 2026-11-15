package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.LibraryView;
import org.dpdns.syscrashed.phiclient.gui.music.PhiPlaylist;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The view tabs of the music page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>One row of parallelogram tabs: All, Favorite, every named playlist and a trailing plus button
 * that creates a playlist. The selected tab uses the level-5 background and the accent text color,
 * the other tabs the level-3 background and the secondary text color - no outline, no disabled gray.
 * Left click selects a view, right click on a playlist tab opens its management overlay. Tabs that
 * do not fit are clipped instead of wrapping.</p>
 */
public class PhiMusicTabBar extends PhiComponent {

    /** The view actions the bar offers to the music page. */
    public interface Listener {

        /** Selects the full library. */
        void onSelectAll();

        /** Selects the favorites. */
        void onSelectFavorite();

        /**
         * @param name the playlist to select.
         */
        void onSelectPlaylist(String name);

        /** Creates a new playlist. */
        void onCreatePlaylist();

        /**
         * @param name the playlist whose management overlay should open.
         */
        void onManagePlaylist(String name);
    }

    /** One tab of the bar. */
    private static final class Tab {
        private float x;
        private float width;
        private String label;
        private LibraryView view;
        private String playlistName = "";
        private boolean createButton;
        private final PhiSheen sheen = new PhiSheen();
        private boolean hovered;
        private float hoverAmount;
        private float selectedAmount;
    }

    private final float uiScale;

    private final List<Tab> tabs = new ArrayList<Tab>();

    private Listener listener;

    private LibraryView view = LibraryView.ALL;

    private String playlistName = "";

    private List<PhiPlaylist> playlists = new ArrayList<PhiPlaylist>();

    /** The tab list signature of the last build (rebuilt only when it changes). */
    private String builtSignature = "";

    private long lastFrame = System.currentTimeMillis();

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiMusicTabBar(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);
        this.uiScale = uiScale;
    }

    /**
     * @param listener the view callback.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Synchronizes the selected view (called every frame by the owner).
     *
     * @param view         the active view.
     * @param playlistName the active playlist name.
     */
    public void setSelection(LibraryView view, String playlistName) {
        this.view = view != null ? view : LibraryView.ALL;
        this.playlistName = playlistName != null ? playlistName : "";
    }

    /**
     * Synchronizes the named playlists (called every frame by the owner).
     *
     * @param playlists the named playlists in creation order.
     */
    public void setPlaylists(List<PhiPlaylist> playlists) {
        this.playlists = playlists != null ? playlists : new ArrayList<PhiPlaylist>();
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        buildTabs(nvg);

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        float gap = 6.0F * this.uiScale;

        nvg.save();
        nvg.scissor(this.x, this.y - 1.0F, this.width + this.height, this.height + 2.0F);

        float cursor = this.x;
        for (Tab tab : this.tabs) {
            if (cursor > this.x + this.width) {
                break; // clipped, no wrapping
            }

            boolean selected = !tab.createButton && isSelected(tab);
            tab.hovered = PhiSkew.contains(mouseX, mouseY, cursor, this.y, tab.width, this.height);
            tab.hoverAmount = tab.hovered
                    ? Math.min(1.0F, tab.hoverAmount + deltaMs / 120.0F)
                    : Math.max(0.0F, tab.hoverAmount - deltaMs / 420.0F);
            tab.selectedAmount = selected
                    ? Math.min(1.0F, tab.selectedAmount + deltaMs / 160.0F)
                    : Math.max(0.0F, tab.selectedAmount - deltaMs / 160.0F);
            tab.sheen.update(tab.hovered);

            int background = PhiTheme.mix(
                    PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), tab.hoverAmount),
                    PhiTheme.level5(), tab.selectedAmount);
            nvg.drawSkewRect(cursor, this.y, tab.width, this.height, radius, background);
            tab.sheen.drawSkew(nvg, cursor, this.y, tab.width, this.height, radius,
                    tab.hoverAmount, PhiTheme.sheen());

            if (tab.createButton) {
                PhiIcons.PLUS.draw(nvg, cursor + tab.width * 0.5F + PhiSkew.offset(this.height) * 0.5F,
                        this.y + this.height * 0.5F, this.height * (0.5F + 0.05F * tab.hoverAmount),
                        PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.accent(), tab.hoverAmount));
            } else {
                float textSize = PhiMetrics.FONT_BODY * this.uiScale;
                float textX = PhiSkew.contentLeft(cursor, this.height, this.height * 0.35F);
                String label = nvg.limitText(tab.label, textSize, Fonts.REGULAR,
                        tab.width - this.height * 0.5F);
                int color = PhiTheme.mix(
                        PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), tab.hoverAmount),
                        PhiTheme.accent(), tab.selectedAmount);
                nvg.drawText(label, textX, this.y + (this.height - textSize) * 0.5F,
                        color, textSize, Fonts.REGULAR);
            }

            tab.x = cursor;
            cursor += tab.width + gap;
        }

        nvg.restore();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.listener == null) {
            return;
        }

        for (Tab tab : this.tabs) {
            if (!PhiSkew.contains(mouseX, mouseY, tab.x, this.y, tab.width, this.height)) {
                continue;
            }

            tab.sheen.restart();
            if (tab.createButton) {
                if (mouseButton == 0) {
                    this.listener.onCreatePlaylist();
                }
                return;
            }

            if (mouseButton == 0) {
                switch (tab.view) {
                    case ALL:
                        this.listener.onSelectAll();
                        break;
                    case FAVORITE:
                        this.listener.onSelectFavorite();
                        break;
                    case PLAYLIST:
                    default:
                        this.listener.onSelectPlaylist(tab.playlistName);
                        break;
                }
            } else if (mouseButton == 1 && tab.view == LibraryView.PLAYLIST) {
                this.listener.onManagePlaylist(tab.playlistName);
            }
            return;
        }
    }

    /**
     * @param tab a tab.
     * @return true when the tab is the selected view.
     */
    private boolean isSelected(Tab tab) {
        if (tab.view != this.view) {
            return false;
        }
        return tab.view != LibraryView.PLAYLIST || tab.playlistName.equals(this.playlistName);
    }

    /**
     * Rebuilds the tab list when the playlist names changed.
     */
    private void buildTabs(NanoVGManager nvg) {
        StringBuilder signature = new StringBuilder();
        for (PhiPlaylist playlist : this.playlists) {
            signature.append(playlist.getName()).append(';');
        }
        String current = signature.toString();
        if (current.equals(this.builtSignature) && !this.tabs.isEmpty()) {
            return;
        }
        this.builtSignature = current;
        this.tabs.clear();

        float textSize = PhiMetrics.FONT_BODY * this.uiScale;
        float horizontal = this.height * 0.35F;

        this.tabs.add(makeTab(nvg, PhiI18n.get("music.tab.all"), LibraryView.ALL, "", textSize, horizontal));
        this.tabs.add(makeTab(nvg, PhiI18n.get("music.tab.favorite"), LibraryView.FAVORITE, "", textSize, horizontal));
        for (PhiPlaylist playlist : this.playlists) {
            this.tabs.add(makeTab(nvg, playlist.getName(), LibraryView.PLAYLIST, playlist.getName(),
                    textSize, horizontal));
        }

        Tab create = new Tab();
        create.createButton = true;
        create.width = this.height;
        this.tabs.add(create);
    }

    /**
     * Creates one view tab and measures its width.
     */
    private Tab makeTab(NanoVGManager nvg, String label, LibraryView view, String playlistName,
                        float textSize, float horizontal) {
        Tab tab = new Tab();
        tab.label = label;
        tab.view = view;
        tab.playlistName = playlistName;
        tab.width = nvg.textWidth(label, textSize, Fonts.REGULAR) + horizontal * 2.0F
                + PhiSkew.offset(this.height);
        return tab;
    }
}
