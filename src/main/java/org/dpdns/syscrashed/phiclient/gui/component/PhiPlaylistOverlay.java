package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.music.PhiTrack;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The lightweight playlist overlay of the music page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Two modes share one parallelogram panel that covers the song list (never the bottom control
 * bar):</p>
 * <ul>
 *   <li><b>MANAGE</b> - renames or deletes the selected playlist and orders its members: every row
 *       offers move-up, move-down and remove controls (small upright vector icons).</li>
 *   <li><b>PICK</b> - adds one track to a playlist: every row is a target playlist, and the name
 *       field plus the apply button creates a new one.</li>
 * </ul>
 *
 * <p>The panel follows the global shape and carries no outline; the text input is the shared
 * {@link PhiSearchField} (without its icon semantics - it is a plain name field here). The list
 * scrolls with the wheel and is clipped to the panel. Clicking outside the panel or on the close
 * icon closes the overlay.</p>
 */
public class PhiPlaylistOverlay extends PhiComponent implements PhiScrollable {

    /** The mode of the overlay. */
    public enum Mode { MANAGE, PICK }

    /** The actions the overlay offers to the music page. */
    public interface Listener {

        /** Closes the overlay. */
        void onClose();

        /**
         * Applies the name field: rename in MANAGE mode, create-and-add in PICK mode.
         *
         * @param name the text of the name field.
         */
        void onApplyName(String name);

        /** Deletes the managed playlist (MANAGE mode). */
        void onDelete();

        /**
         * Adds the track to the picked playlist (PICK mode).
         *
         * @param playlistName the target playlist.
         */
        void onPick(String playlistName);

        /**
         * Moves a member of the managed playlist (MANAGE mode).
         *
         * @param trackId the member.
         * @param up      true to move towards the beginning.
         */
        void onMove(String trackId, boolean up);

        /**
         * Removes a member of the managed playlist (MANAGE mode).
         *
         * @param trackId the member.
         */
        void onRemove(String trackId);
    }

    /** One row of the member / target list. */
    private static final class Row {
        private String trackId = "";
        private String title = "";
        private String playlistName = "";
    }

    private final float uiScale;

    private final PhiSearchField nameField;

    private Mode mode = Mode.MANAGE;

    private String playlistName = "";

    private Listener listener;

    private final List<Row> rows = new ArrayList<Row>();

    private float scroll;

    /** Geometry of the last frame (the hit tests reuse it). */
    private float listTop;
    private float rowHeight;
    private float applyX;
    private float applyWidth;
    private float buttonY;
    private float deleteX;
    private float closeX;
    private float closeY;
    private float closeSize;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiPlaylistOverlay(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);
        this.uiScale = uiScale;

        float fieldHeight = 26.0F * uiScale;
        this.nameField = new PhiSearchField(0.0F, 0.0F, 0.0F, fieldHeight, uiScale);
    }

    /**
     * @param listener the action callback.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Shows the management mode for a playlist.
     *
     * @param name    the playlist name.
     * @param members the current members (may be empty).
     */
    public void showManage(String name, List<PhiTrack> members) {
        this.mode = Mode.MANAGE;
        this.playlistName = name;
        this.nameField.setText(name);
        this.scroll = 0.0F;
        rebuildRows(members);
    }

    /**
     * Shows the picker mode: the rows are the target playlists.
     *
     * @param targets the available playlist names.
     */
    public void showPick(List<String> targets) {
        this.mode = Mode.PICK;
        this.nameField.setText("");
        this.scroll = 0.0F;
        this.rows.clear();
        for (String target : targets) {
            Row row = new Row();
            row.playlistName = target;
            row.title = target;
            this.rows.add(row);
        }
    }

    /**
     * Rebuilds the member rows (MANAGE mode, after a move / remove).
     *
     * @param members the current members.
     */
    public void rebuildRows(List<PhiTrack> members) {
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
     * @return the mode of the overlay.
     */
    public Mode getMode() {
        return this.mode;
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY)) {
            return false;
        }
        float maxScroll = Math.max(0.0F, this.rows.size() * this.rowHeight
                - (this.y + this.height - this.listTop - 8.0F * this.uiScale));
        this.scroll = Math.max(0.0F, Math.min(maxScroll, this.scroll - notches * this.rowHeight));
        return true;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = 16.0F * this.uiScale;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        // Title and the close icon.
        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        String title = this.mode == Mode.MANAGE
                ? PhiI18n.get("music.playlist.manage") : PhiI18n.get("music.playlist.add");
        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR, contentRight - contentLeft - padding * 2.0F),
                contentLeft, this.y + padding * 0.8F, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        this.closeSize = titleSize;
        this.closeX = contentRight - this.closeSize;
        this.closeY = this.y + padding * 0.8F;
        drawCross(nvg, this.closeX + this.closeSize * 0.5F, this.closeY + this.closeSize * 0.5F,
                this.closeSize * 0.5F, PhiTheme.textSecondary());

        // Name field + apply button (+ delete in MANAGE mode).
        this.rowHeight = 30.0F * this.uiScale;
        float fieldHeight = 26.0F * this.uiScale;
        this.buttonY = this.y + padding * 0.8F + titleSize + padding * 0.7F;
        float buttonHeight = fieldHeight;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        String applyLabel = this.mode == Mode.MANAGE
                ? PhiI18n.get("music.playlist.rename") : PhiI18n.get("music.playlist.create");
        this.applyWidth = nvg.textWidth(applyLabel, bodySize, Fonts.REGULAR) + padding * 1.2F
                + PhiSkew.offset(buttonHeight);
        this.applyX = contentRight - this.applyWidth;

        this.deleteX = 0.0F;
        float fieldRight = this.applyX - padding * 0.5F;
        if (this.mode == Mode.MANAGE) {
            String deleteLabel = PhiI18n.get("music.playlist.delete");
            float deleteWidth = nvg.textWidth(deleteLabel, bodySize, Fonts.REGULAR) + padding * 1.2F
                    + PhiSkew.offset(buttonHeight);
            this.deleteX = this.applyX - deleteWidth - padding * 0.5F;
            fieldRight = this.deleteX - padding * 0.5F;

            nvg.drawSkewRect(this.deleteX, this.buttonY, deleteWidth, buttonHeight,
                    PhiMetrics.NAV_ITEM_RADIUS * this.uiScale, PhiTheme.level3());
            nvg.drawText(deleteLabel, PhiSkew.contentLeft(this.deleteX, buttonHeight, padding * 0.6F),
                    this.buttonY + (buttonHeight - bodySize) * 0.5F,
                    PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);
        }

        this.nameField.setX(contentLeft);
        this.nameField.setY(this.buttonY);
        this.nameField.setWidth(Math.max(20.0F, fieldRight - contentLeft));
        this.nameField.setHeight(fieldHeight);
        this.nameField.draw(nvg, mouseX, mouseY, partialTicks);

        nvg.drawSkewRect(this.applyX, this.buttonY, this.applyWidth, buttonHeight,
                PhiMetrics.NAV_ITEM_RADIUS * this.uiScale, PhiTheme.level4());
        nvg.drawText(applyLabel, PhiSkew.contentLeft(this.applyX, buttonHeight, padding * 0.6F),
                this.buttonY + (buttonHeight - bodySize) * 0.5F,
                PhiTheme.accent(), bodySize, Fonts.REGULAR);

        // The member / target list, clipped to the panel.
        this.listTop = this.buttonY + fieldHeight + padding * 0.6F;
        float listBottom = this.y + this.height - padding * 0.6F;

        nvg.save();
        nvg.scissor(this.x, this.listTop, this.width, Math.max(0.0F, listBottom - this.listTop));

        for (int i = 0; i < this.rows.size(); i++) {
            float rowY = this.listTop + i * this.rowHeight - this.scroll;
            if (rowY + this.rowHeight < this.listTop || rowY > listBottom) {
                continue;
            }
            drawRow(nvg, this.rows.get(i), contentLeft, contentRight, rowY, bodySize, mouseX, mouseY);
        }

        nvg.restore();
    }

    /**
     * Draws one row of the list.
     */
    private void drawRow(NanoVGManager nvg, Row row, float contentLeft, float contentRight,
                         float rowY, float bodySize, int mouseX, int mouseY) {
        boolean hovered = mouseY >= rowY && mouseY <= rowY + this.rowHeight
                && PhiSkew.contains(mouseX, mouseY, this.x, rowY, this.width, this.rowHeight);
        if (hovered) {
            nvg.drawSkewRect(this.x + this.width * 0.06F, rowY, this.width * 0.88F, this.rowHeight,
                    PhiMetrics.NAV_ITEM_RADIUS * this.uiScale, PhiTheme.level3());
        }

        float textRight = contentRight;
        if (this.mode == Mode.MANAGE) {
            // Move-up, move-down and remove icons at the right end.
            float iconSize = this.rowHeight * 0.5F;
            float centerY = rowY + this.rowHeight * 0.5F;
            float removeX = contentRight - iconSize * 0.5F;
            drawCross(nvg, removeX, centerY, iconSize * 0.42F, PhiTheme.textSecondary());
            drawTriangle(nvg, contentRight - iconSize * 1.8F, centerY, iconSize * 0.42F, false,
                    PhiTheme.textSecondary());
            drawTriangle(nvg, contentRight - iconSize * 3.1F, centerY, iconSize * 0.42F, true,
                    PhiTheme.textSecondary());
            textRight = contentRight - iconSize * 3.6F;
        }

        nvg.drawText(nvg.limitText(row.title, bodySize, Fonts.REGULAR,
                        Math.max(0.0F, textRight - contentLeft)),
                contentLeft, rowY + (this.rowHeight - bodySize) * 0.5F,
                PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);
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

        // Apply / delete buttons.
        if (mouseY >= this.buttonY && mouseY <= this.buttonY + 26.0F * this.uiScale) {
            if (mouseX >= this.applyX && mouseX <= this.applyX + this.applyWidth) {
                this.listener.onApplyName(this.nameField.getText());
                return;
            }
            if (this.mode == Mode.MANAGE && mouseX >= this.deleteX
                    && mouseX < this.applyX - 4.0F * this.uiScale) {
                this.listener.onDelete();
                return;
            }
        }

        // Forward to the name field (focus handling).
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);

        // Rows.
        if (mouseY < this.listTop) {
            return;
        }
        int index = (int) ((mouseY - this.listTop + this.scroll) / this.rowHeight);
        if (index < 0 || index >= this.rows.size()) {
            return;
        }
        Row row = this.rows.get(index);

        if (this.mode == Mode.PICK) {
            this.listener.onPick(row.playlistName);
            return;
        }

        float iconSize = this.rowHeight * 0.5F;
        float contentRight = PhiSkew.contentRight(this.x, this.width, 16.0F * this.uiScale);
        if (mouseX >= contentRight - iconSize) {
            this.listener.onRemove(row.trackId);
        } else if (mouseX >= contentRight - iconSize * 2.3F) {
            this.listener.onMove(row.trackId, false);
        } else if (mouseX >= contentRight - iconSize * 3.6F) {
            this.listener.onMove(row.trackId, true);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
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

    /**
     * Draws a small triangle pointing up or down (the move icons).
     */
    private static void drawTriangle(NanoVGManager nvg, float centerX, float centerY, float half,
                                     boolean up, int color) {
        nvg.beginPath();
        if (up) {
            nvg.moveTo(centerX, centerY - half);
            nvg.lineTo(centerX - half, centerY + half * 0.6F);
            nvg.lineTo(centerX + half, centerY + half * 0.6F);
        } else {
            nvg.moveTo(centerX, centerY + half);
            nvg.lineTo(centerX - half, centerY - half * 0.6F);
            nvg.lineTo(centerX + half, centerY - half * 0.6F);
        }
        nvg.closePath();
        nvg.fillPath(color);
    }

    private static boolean insideBox(float pointX, float pointY, float boxX, float boxY, float size) {
        return pointX >= boxX && pointX <= boxX + size && pointY >= boxY && pointY <= boxY + size;
    }
}
