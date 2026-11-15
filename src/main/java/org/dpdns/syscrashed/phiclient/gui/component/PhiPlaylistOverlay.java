package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.PhiTrack;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiScrollState;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The playlist dialog of the music page (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>One modal, self-adaptive parallelogram panel that floats above every other element of the hub.
 * Three modes share one shell:</p>
 * <ul>
 *   <li><b>CREATE</b> - a name field, a checklist of every library track (all unchecked by default)
 *       and a confirm / cancel row. The name is applied only when the confirm button is pressed.</li>
 *   <li><b>ADD</b> - the list of the existing playlists with a single selection; the confirm button
 *       adds the pending track to the selected playlist. Cancel / the close icon change nothing.</li>
 *   <li><b>MANAGE</b> - a rename field with its own confirm button, the member list (each row removes
 *       itself) and a delete-playlist button at the bottom (confirmed elsewhere).</li>
 * </ul>
 *
 * <p>The title row carries only the close icon (pinned to the slanted right edge); there is no delete
 * key in the title row. The rows scroll along the diagonal of the panel, exactly like the change log:
 * their x anchor is measured at the un-scrolled row position and slides {@link PhiSkew#SLOPE} pixels
 * right per scrolled pixel. Every interactive element uses the shared {@link PhiSheen} hover template
 * with one shape family for both hover and click.</p>
 */
public class PhiPlaylistOverlay extends PhiComponent implements PhiScrollable {

    /** The mode of the dialog. */
    public enum Mode { CREATE, ADD, MANAGE }

    /** The actions the dialog offers to the music page. */
    public interface Listener {

        /** Closes the dialog without applying anything. */
        void onClose();

        /**
         * Creates a playlist with the given name and member track ids (CREATE mode, confirm only).
         *
         * @param name     the entered name.
         * @param trackIds the checked track identifiers.
         */
        void onCreate(String name, List<String> trackIds);

        /**
         * Adds the pending track to the selected playlist (ADD mode, confirm only).
         *
         * @param playlistName the selected playlist name.
         * @param trackId      the track to add.
         */
        void onAddToPlaylist(String playlistName, String trackId);

        /**
         * Renames the managed playlist (MANAGE mode, confirm only).
         *
         * @param oldName the current name.
         * @param newName the new name.
         */
        void onRename(String oldName, String newName);

        /** Deletes the managed playlist (MANAGE mode; the caller confirms first). */
        void onDeletePlaylist(String name);

        /**
         * Removes a member from the managed playlist (MANAGE mode).
         *
         * @param playlistName the playlist name.
         * @param trackId      the member to remove.
         */
        void onRemoveFromPlaylist(String playlistName, String trackId);
    }

    /** One row of the list, interpreted according to the mode. */
    private static final class Row {
        private String title = "";
        private String trackId = "";
        private String playlistName = "";
        private boolean checked;
        private float hoverAmount;
        private float removeHover;
        private final PhiSheen sheen = new PhiSheen();
    }

    private final float uiScale;

    private final PhiSearchField nameField;

    private final PhiScrollState scrollState = new PhiScrollState();

    private Mode mode = Mode.CREATE;

    private Listener listener;

    /** MANAGE: the name of the playlist the dialog acts on. */
    private String playlistName = "";

    /** ADD: the track that the confirm adds. */
    private String pickTrackId = "";

    private final List<Row> rows = new ArrayList<Row>();

    /** ADD: the selected playlist name (empty = no selection). */
    private String selectedPlaylistName = "";

    // Geometry of the last frame (the hit tests reuse it).
    private float contentLeft;
    private float contentRight;
    private float listTop;
    private float listBottom;
    private float rowHeight;
    private float rowGap;

    private float closeX;
    private float closeY;
    private float closeSize;

    private float confirmX;
    private float confirmY;
    private float confirmWidth;
    private float confirmHeight;
    private float cancelX;
    private float cancelY;
    private float cancelWidth;
    private float cancelHeight;

    private float renameX;
    private float renameY;
    private float renameWidth;
    private float renameHeight;
    private float deleteX;
    private float deleteY;
    private float deleteWidth;
    private float deleteHeight;

    private final PhiSheen confirmSheen = new PhiSheen();
    private final PhiSheen cancelSheen = new PhiSheen();
    private final PhiSheen renameSheen = new PhiSheen();
    private final PhiSheen deleteSheen = new PhiSheen();

    private float confirmHover;
    private float cancelHover;
    private float renameHover;
    private float deleteHover;
    private float closeHover;

    private long lastFrame = System.currentTimeMillis();

    /**
     * @param x       the left x of the dialog bounding box.
     * @param y       the top y of the dialog bounding box.
     * @param width   the width of the dialog bounding box.
     * @param height  the height of the dialog bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiPlaylistOverlay(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);
        this.uiScale = uiScale;

        float fieldHeight = 26.0F * uiScale;
        this.nameField = new PhiSearchField(0.0F, 0.0F, 0.0F, fieldHeight, uiScale);
        // It is a plain name field: no search icon and no submit listener - the text is only read
        // when the confirm / rename button is pressed, so return or focus loss never applies it.
        this.nameField.setShowIcon(false);
    }

    /**
     * @param listener the action callback.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Shows the create mode: a name field and a checklist of every library track.
     *
     * @param allTracks the full library (every track, in sort order).
     */
    public void showCreate(List<PhiTrack> allTracks) {
        this.mode = Mode.CREATE;
        this.playlistName = "";
        this.nameField.setText("");
        this.scrollState.reset();
        this.rows.clear();
        if (allTracks != null) {
            for (PhiTrack track : allTracks) {
                Row row = new Row();
                row.trackId = track.getId();
                row.title = track.getTitle();
                row.checked = false;
                this.rows.add(row);
            }
        }
    }

    /**
     * Shows the add mode: the list of the target playlists.
     *
     * @param playlistNames the available playlist names.
     * @param trackId       the track the confirm adds.
     */
    public void showAdd(List<String> playlistNames, String trackId) {
        this.mode = Mode.ADD;
        this.playlistName = "";
        this.pickTrackId = trackId != null ? trackId : "";
        this.selectedPlaylistName = "";
        this.nameField.setText("");
        this.scrollState.reset();
        this.rows.clear();
        if (playlistNames != null) {
            for (String target : playlistNames) {
                Row row = new Row();
                row.playlistName = target;
                row.title = target;
                this.rows.add(row);
            }
        }
    }

    /**
     * Shows the manage mode for a playlist.
     *
     * @param name    the playlist name.
     * @param members the current members (may be empty).
     */
    public void showManage(String name, List<PhiTrack> members) {
        this.mode = Mode.MANAGE;
        this.playlistName = name != null ? name : "";
        this.nameField.setText(this.playlistName);
        this.scrollState.reset();
        this.rows.clear();
        if (members != null) {
            for (PhiTrack track : members) {
                Row row = new Row();
                row.trackId = track.getId();
                row.title = track.getTitle();
                this.rows.add(row);
            }
        }
    }

    /**
     * Rebuilds the member rows (MANAGE mode, after a remove).
     *
     * @param members the current members.
     */
    public void rebuildMembers(List<PhiTrack> members) {
        this.rows.clear();
        if (members != null) {
            for (PhiTrack track : members) {
                Row row = new Row();
                row.trackId = track.getId();
                row.title = track.getTitle();
                this.rows.add(row);
            }
        }
    }

    /**
     * @return the mode of the dialog.
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * @return the name field text of the dialog (read by the owner when confirming).
     */
    public String getNameText() {
        return this.nameField.getText();
    }

    /**
     * @return the selected playlist name (ADD mode; empty when nothing is selected).
     */
    public String getSelectedPlaylistName() {
        return this.selectedPlaylistName;
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY)) {
            return false;
        }
        this.scrollState.scrollBy(notches * (this.rowHeight + this.rowGap));
        return true;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = 16.0F * this.uiScale;
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float buttonHeight = 26.0F * this.uiScale;
        float fieldHeight = 26.0F * this.uiScale;
        float buttonRadius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;

        this.contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        this.contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = Math.max(20.0F, this.contentRight - this.contentLeft);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        // Title row: the title on the left, the close icon pinned to the slanted right edge.
        this.closeSize = titleSize;
        this.closeY = this.y + padding * 0.8F;
        this.closeX = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height,
                this.closeY + this.closeSize * 0.5F) - padding * 0.8F - this.closeSize;

        boolean closeHovered = insideBox(mouseX, mouseY, this.closeX, this.closeY, this.closeSize);
        this.closeHover = approach(this.closeHover, closeHovered, deltaMs);
        drawCross(nvg, this.closeX + this.closeSize * 0.5F, this.closeY + this.closeSize * 0.5F,
                this.closeSize * (0.5F + 0.05F * this.closeHover),
                PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), this.closeHover));

        String title = PhiI18n.get(modeTitleKey());
        float titleRight = this.closeX - padding * 0.5F;
        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR, Math.max(0.0F, titleRight - this.contentLeft)),
                this.contentLeft, this.closeY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        // Name field (CREATE) and the rename field (MANAGE). MANAGE reserves the width of the rename
        // button on the right of the field.
        float fieldY = this.y + padding * 0.8F + titleSize + padding * 0.7F;
        float fieldWidth = contentWidth;
        if (this.mode == Mode.MANAGE) {
            String renameLabel = PhiI18n.get("music.playlist.rename");
            this.renameWidth = nvg.textWidth(renameLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                    + PhiSkew.offset(buttonHeight);
            this.renameHeight = buttonHeight;
            this.renameY = fieldY;
            this.renameX = this.contentRight - this.renameWidth;
            fieldWidth = Math.max(20.0F, this.renameX - padding * 0.5F - this.contentLeft);
        }

        if (this.mode == Mode.CREATE || this.mode == Mode.MANAGE) {
            this.nameField.setX(this.contentLeft);
            this.nameField.setY(fieldY);
            this.nameField.setWidth(fieldWidth);
            this.nameField.setHeight(fieldHeight);
            this.nameField.draw(nvg, mouseX, mouseY, partialTicks);
        }

        // The row list, clipped to the panel. The rows use the same metrics as the library list of
        // the music page, so both lists share one relaxed rhythm.
        this.rowHeight = PhiMetrics.SONG_ROW_HEIGHT * this.uiScale;
        this.rowGap = PhiMetrics.SONG_ROW_SPACING * this.uiScale;
        this.listTop = fieldY + (this.mode == Mode.CREATE || this.mode == Mode.MANAGE
                ? fieldHeight + padding * 0.6F : padding * 0.4F);

        // Bottom action row: confirm / cancel (CREATE, ADD) or the delete button (MANAGE).
        this.confirmWidth = 0.0F;
        this.cancelWidth = 0.0F;
        this.deleteWidth = 0.0F;
        float buttonY = this.y + this.height - padding * 0.8F - buttonHeight;

        if (this.mode == Mode.CREATE || this.mode == Mode.ADD) {
            this.confirmHeight = buttonHeight;
            this.cancelHeight = buttonHeight;
            this.confirmY = buttonY;
            this.cancelY = buttonY;
            String confirmLabel = PhiI18n.get("music.confirm.yes");
            String cancelLabel = PhiI18n.get("music.confirm.no");
            this.confirmWidth = nvg.textWidth(confirmLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                    + PhiSkew.offset(buttonHeight);
            this.cancelWidth = nvg.textWidth(cancelLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                    + PhiSkew.offset(buttonHeight);
            this.confirmX = this.contentRight - this.confirmWidth;
            this.cancelX = this.confirmX - this.cancelWidth - padding * 0.5F;
        } else if (this.mode == Mode.MANAGE) {
            this.deleteHeight = buttonHeight;
            this.deleteY = buttonY;
            String deleteLabel = PhiI18n.get("music.playlist.delete");
            this.deleteWidth = nvg.textWidth(deleteLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                    + PhiSkew.offset(buttonHeight);
            this.deleteX = this.contentRight - this.deleteWidth;
        }

        this.listBottom = buttonY - padding * 0.6F;

        float rowStride = this.rowHeight + this.rowGap;
        this.scrollState.setContentSize(Math.max(0.0F, this.rows.size() * rowStride - this.rowGap),
                Math.max(0.0F, this.listBottom - this.listTop));
        this.scrollState.update(deltaMs);

        float offset = this.scrollState.getOffset();
        float slide = PhiSkew.SLOPE * offset;

        // The rows follow the slanted left edge and scroll along the diagonal, like the change log.
        nvg.save();
        nvg.translate(this.x, this.y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);
        // A row leans to the right by SLOPE * rowHeight, but a scissor rectangle is axis aligned:
        // the clip is extended by that amount so the rounded right corners of a row survive.
        float rowWidth = this.width - padding * 2.0F;
        nvg.scissor(this.contentLeft - this.x, this.listTop - this.y,
                rowWidth + PhiSkew.SLOPE * this.rowHeight + 1.0F,
                Math.max(0.0F, this.listBottom - this.listTop));
        nvg.skewX(-PhiSkew.SHEAR_DEGREES);
        nvg.translate(-this.x, -this.y);

        if (this.rows.isEmpty()) {
            String empty = PhiI18n.get(this.mode == Mode.ADD ? "music.playlist.empty"
                    : "music.playlist.tracks.empty");
            nvg.drawTextCentered(empty, this.x + this.width * 0.5F + PhiSkew.offset(this.height) * 0.5F,
                    this.listTop + (this.listBottom - this.listTop) * 0.4F,
                    PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
        } else {
            for (int i = 0; i < this.rows.size(); i++) {
                float rowY = this.listTop + i * rowStride - offset;
                if (rowY + this.rowHeight < this.listTop || rowY > this.listBottom) {
                    continue;
                }
                float anchorX = PhiSkew.leftEdgeAt(this.x, this.y, this.height,
                        this.listTop + i * rowStride) + padding;
                drawRow(nvg, this.rows.get(i), anchorX + slide, rowY, bodySize, mouseX, mouseY, deltaMs);
            }
        }

        nvg.restore();

        // Action buttons.
        updateButtonHover(mouseX, mouseY, deltaMs);
        if (this.mode == Mode.CREATE || this.mode == Mode.ADD) {
            drawButton(nvg, PhiI18n.get("music.confirm.no"), this.cancelX, this.cancelY,
                    this.cancelWidth, this.cancelHeight, buttonRadius, bodySize, padding,
                    this.cancelHover, this.cancelSheen, false,
                    PhiTheme.textSecondary(), PhiTheme.textPrimary());
            drawButton(nvg, PhiI18n.get("music.confirm.yes"), this.confirmX, this.confirmY,
                    this.confirmWidth, this.confirmHeight, buttonRadius, bodySize, padding,
                    this.confirmHover, this.confirmSheen, true,
                    PhiTheme.accent(), PhiTheme.accent());
        } else if (this.mode == Mode.MANAGE) {
            drawButton(nvg, PhiI18n.get("music.playlist.rename"), this.renameX, this.renameY,
                    this.renameWidth, this.renameHeight, buttonRadius, bodySize, padding,
                    this.renameHover, this.renameSheen, false,
                    PhiTheme.textSecondary(), PhiTheme.textPrimary());
            drawButton(nvg, PhiI18n.get("music.playlist.delete"), this.deleteX, this.deleteY,
                    this.deleteWidth, this.deleteHeight, buttonRadius, bodySize, padding,
                    this.deleteHover, this.deleteSheen, false,
                    PhiTheme.textSecondary(), PhiTheme.danger());
        }
    }

    /**
     * @return the i18n key of the dialog title of the current mode.
     */
    private String modeTitleKey() {
        switch (this.mode) {
            case ADD:
                return "music.playlist.add";
            case MANAGE:
                return "music.playlist.manage";
            case CREATE:
            default:
                return "music.playlist.new";
        }
    }

    /**
     * Draws one row of the list. The row starts at the slanted left edge and runs along the diagonal.
     */
    private void drawRow(NanoVGManager nvg, Row row, float rowX, float rowY, float bodySize,
                         int mouseX, int mouseY, float deltaMs) {
        float padding = 16.0F * this.uiScale;
        // The row spans the dialog between its two slanted edges: the width is the width of the
        // dialog minus the two insets, so the right end keeps the same distance to the right edge
        // as the left end keeps to the left edge - at every row and at every scroll position.
        float rowWidth = this.width - padding * 2.0F;
        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;

        boolean selected = this.mode == Mode.ADD && row.playlistName.equals(this.selectedPlaylistName);
        boolean hovered = PhiSkew.contains(mouseX, mouseY, rowX, rowY, rowWidth, this.rowHeight);
        row.hoverAmount = approach(row.hoverAmount, hovered, deltaMs);
        row.sheen.update(hovered);

        int background = selected ? PhiTheme.level5()
                : PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), row.hoverAmount);
        nvg.drawSkewRect(rowX, rowY, rowWidth, this.rowHeight, radius, background);
        row.sheen.drawSkew(nvg, rowX, rowY, rowWidth, this.rowHeight, radius,
                row.hoverAmount, PhiTheme.sheen());

        int textColor = selected ? PhiTheme.accent()
                : PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), row.hoverAmount);
        float textY = rowY + (this.rowHeight - bodySize) * 0.5F;
        // Every element of the row follows the slanted edges of the row, so nothing can leave it.
        float textLeft = PhiSkew.leftEdgeAt(rowX, rowY, this.rowHeight, textY) + padding * 0.6F;

        if (this.mode == Mode.CREATE) {
            float checkSize = this.rowHeight * 0.5F;
            float checkY = rowY + (this.rowHeight - checkSize) * 0.5F;
            float checkX = PhiSkew.leftEdgeAt(rowX, rowY, this.rowHeight, checkY) + padding * 0.6F;
            drawCheckbox(nvg, checkX, checkY, checkSize, row.checked, textColor);
            float checkRight = checkX + checkSize;
            nvg.drawText(nvg.limitText(row.title, bodySize, Fonts.REGULAR,
                            Math.max(0.0F, rowX + rowWidth - (checkRight + padding * 0.6F))),
                    checkRight + padding * 0.6F, textY, textColor, bodySize, Fonts.REGULAR);
            return;
        }

        if (this.mode == Mode.MANAGE) {
            float removeSize = this.rowHeight * 0.5F;
            float removeCenterX = PhiSkew.rightEdgeAt(rowX, rowWidth, rowY, this.rowHeight,
                    rowY + this.rowHeight * 0.5F) - padding * 0.8F - removeSize * 0.5F;
            float centerY = rowY + this.rowHeight * 0.5F;
            boolean removeHovered = insideBox(mouseX, mouseY,
                    removeCenterX - removeSize * 0.5F, centerY - removeSize * 0.5F, removeSize);
            row.removeHover = approach(row.removeHover, removeHovered, deltaMs);
            drawCross(nvg, removeCenterX, centerY, removeSize * 0.42F,
                    PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.danger(), row.removeHover));
            float textRight = removeCenterX - removeSize * 0.5F - padding * 0.6F;
            nvg.drawText(nvg.limitText(row.title, bodySize, Fonts.REGULAR,
                            Math.max(0.0F, textRight - textLeft)),
                    textLeft, textY, textColor, bodySize, Fonts.REGULAR);
            return;
        }

        // ADD: a plain selectable row.
        nvg.drawText(nvg.limitText(row.title, bodySize, Fonts.REGULAR,
                        Math.max(0.0F, rowWidth - padding * 1.2F)),
                textLeft, textY, textColor, bodySize, Fonts.REGULAR);
    }

    /**
     * Draws a checkbox (an upright rounded square with a check mark when selected).
     */
    private static void drawCheckbox(NanoVGManager nvg, float x, float y, float size, boolean checked,
                                     int color) {
        float radius = size * 0.25F;
        if (checked) {
            nvg.drawRoundedRect(x, y, size, size, radius, PhiTheme.accent());
            float w = Math.max(1.0F, size * 0.13F);
            nvg.drawLine(x + size * 0.24F, y + size * 0.52F, x + size * 0.42F, y + size * 0.70F,
                    PhiTheme.textPrimary(), w);
            nvg.drawLine(x + size * 0.42F, y + size * 0.70F, x + size * 0.78F, y + size * 0.30F,
                    PhiTheme.textPrimary(), w);
        } else {
            nvg.drawRoundedRectOutline(x, y, size, size, radius, Math.max(1.0F, size * 0.10F), color);
        }
    }

    /**
     * Draws a button with the shared sheen hover template.
     */
    private void drawButton(NanoVGManager nvg, String label, float bx, float by, float bw, float bh,
                            float radius, float bodySize, float padding, float hover,
                            PhiSheen sheen, boolean primary, int textColor, int textHoverColor) {
        int base = primary ? PhiTheme.level4() : PhiTheme.level3();
        int hovered = primary ? PhiTheme.level5() : PhiTheme.level4();
        nvg.drawSkewRect(bx, by, bw, bh, radius, PhiTheme.mix(base, hovered, hover));
        sheen.drawSkew(nvg, bx, by, bw, bh, radius, hover, PhiTheme.sheen());
        nvg.drawText(label, PhiSkew.contentLeft(bx, bh, padding * 0.7F),
                by + (bh - bodySize) * 0.5F, PhiTheme.mix(textColor, textHoverColor, hover),
                bodySize, Fonts.REGULAR);
    }

    /**
     * Updates the hover state of the action buttons (after their geometry was computed).
     */
    private void updateButtonHover(int mouseX, int mouseY, float deltaMs) {
        if (this.mode == Mode.CREATE || this.mode == Mode.ADD) {
            boolean confirmHovered = PhiSkew.contains(mouseX, mouseY, this.confirmX, this.confirmY,
                    this.confirmWidth, this.confirmHeight);
            boolean cancelHovered = PhiSkew.contains(mouseX, mouseY, this.cancelX, this.cancelY,
                    this.cancelWidth, this.cancelHeight);
            this.confirmHover = approach(this.confirmHover, confirmHovered, deltaMs);
            this.cancelHover = approach(this.cancelHover, cancelHovered, deltaMs);
            this.confirmSheen.update(confirmHovered);
            this.cancelSheen.update(cancelHovered);
        } else if (this.mode == Mode.MANAGE) {
            boolean renameHovered = PhiSkew.contains(mouseX, mouseY, this.renameX, this.renameY,
                    this.renameWidth, this.renameHeight);
            boolean deleteHovered = PhiSkew.contains(mouseX, mouseY, this.deleteX, this.deleteY,
                    this.deleteWidth, this.deleteHeight);
            this.renameHover = approach(this.renameHover, renameHovered, deltaMs);
            this.deleteHover = approach(this.deleteHover, deleteHovered, deltaMs);
            this.renameSheen.update(renameHovered);
            this.deleteSheen.update(deleteHovered);
        }
    }

    /**
     * Moves a hover amount toward its target (120 ms in, 420 ms out).
     */
    private static float approach(float current, boolean hovered, float deltaMs) {
        return hovered
                ? Math.min(1.0F, current + deltaMs / 120.0F)
                : Math.max(0.0F, current - deltaMs / 420.0F);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || this.listener == null) {
            return;
        }

        if (!isInside(mouseX, mouseY)) {
            this.listener.onClose();
            return;
        }

        // Close icon.
        if (insideBox(mouseX, mouseY, this.closeX, this.closeY, this.closeSize)) {
            this.listener.onClose();
            return;
        }

        // Forward to the name field (focus handling).
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);

        // Action buttons.
        if (this.mode == Mode.CREATE || this.mode == Mode.ADD) {
            if (insideButton(mouseX, mouseY, this.confirmX, this.confirmY, this.confirmWidth, this.confirmHeight)) {
                this.confirmSheen.restart();
                onConfirm();
                return;
            }
            if (insideButton(mouseX, mouseY, this.cancelX, this.cancelY, this.cancelWidth, this.cancelHeight)) {
                this.cancelSheen.restart();
                this.listener.onClose();
                return;
            }
        } else if (this.mode == Mode.MANAGE) {
            if (insideButton(mouseX, mouseY, this.renameX, this.renameY, this.renameWidth, this.renameHeight)) {
                this.renameSheen.restart();
                if (this.listener != null) {
                    this.listener.onRename(this.playlistName, this.nameField.getText());
                }
                return;
            }
            if (insideButton(mouseX, mouseY, this.deleteX, this.deleteY, this.deleteWidth, this.deleteHeight)) {
                this.deleteSheen.restart();
                this.listener.onDeletePlaylist(this.playlistName);
                return;
            }
        }

        // Rows.
        if (mouseY < this.listTop || mouseY > this.listBottom) {
            return;
        }
        int index = (int) ((mouseY - this.listTop + this.scrollState.getOffset())
                / (this.rowHeight + this.rowGap));
        if (index < 0 || index >= this.rows.size()) {
            return;
        }
        Row row = this.rows.get(index);

        if (this.mode == Mode.CREATE) {
            row.checked = !row.checked;
            return;
        }
        if (this.mode == Mode.ADD) {
            this.selectedPlaylistName = row.playlistName;
            return;
        }
        if (this.mode == Mode.MANAGE) {
            // The remove icon at the right end of the row.
            float padding = 16.0F * this.uiScale;
            float rowStride = this.rowHeight + this.rowGap;
            float rowX = PhiSkew.leftEdgeAt(this.x, this.y, this.height,
                    this.listTop + index * rowStride) + padding
                    + PhiSkew.SLOPE * this.scrollState.getOffset();
            float rowY = this.listTop + index * rowStride - this.scrollState.getOffset();
            float rowWidth = this.width - padding * 2.0F;
            float removeSize = this.rowHeight * 0.5F;
            float removeCenterX = PhiSkew.rightEdgeAt(rowX, rowWidth, rowY, this.rowHeight,
                    rowY + this.rowHeight * 0.5F) - padding * 0.8F - removeSize * 0.5F;
            float centerY = rowY + this.rowHeight * 0.5F;
            if (insideBox(mouseX, mouseY, removeCenterX - removeSize * 0.5F, centerY - removeSize * 0.5F,
                    removeSize)) {
                this.listener.onRemoveFromPlaylist(this.playlistName, row.trackId);
            }
        }
    }

    /**
     * Applies the confirm action of the current mode (CREATE / ADD).
     */
    private void onConfirm() {
        if (this.listener == null) {
            return;
        }
        if (this.mode == Mode.CREATE) {
            List<String> checked = new ArrayList<String>();
            for (Row row : this.rows) {
                if (row.checked) {
                    checked.add(row.trackId);
                }
            }
            this.listener.onCreate(this.nameField.getText(), checked);
            return;
        }
        if (this.mode == Mode.ADD) {
            this.listener.onAddToPlaylist(this.selectedPlaylistName, this.pickTrackId);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            if (this.listener != null) {
                this.listener.onClose();
            }
            return;
        }
        this.nameField.keyTyped(typedChar, keyCode);
    }

    /**
     * Draws a small cross (the close / remove icon).
     */
    private static void drawCross(NanoVGManager nvg, float centerX, float centerY, float half, int color) {
        float w = Math.max(1.0F, half * 0.22F);
        nvg.drawLine(centerX - half, centerY - half, centerX + half, centerY + half, color, w);
        nvg.drawLine(centerX - half, centerY + half, centerX + half, centerY - half, color, w);
    }

    private static boolean insideBox(float pointX, float pointY, float boxX, float boxY, float size) {
        return pointX >= boxX && pointX <= boxX + size && pointY >= boxY && pointY <= boxY + size;
    }

    private static boolean insideButton(float pointX, float pointY, float bx, float by, float bw, float bh) {
        return PhiSkew.contains(pointX, pointY, bx, by, bw, bh);
    }
}
