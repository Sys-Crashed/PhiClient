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
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiScrollState;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The lightweight playlist overlay of the music page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>Two modes share one parallelogram panel that covers the song list (never the bottom control
 * bar):</p>
 * <ul>
 *   <li><b>MANAGE</b> - renames or deletes the selected playlist and orders its members: the name
 *       field spans the full width of the panel and confirms the rename as soon as the editing ends
 *       (return key or focus loss), the delete button shares the title row and every row offers
 *       move-up, move-down and remove controls (small upright vector icons).</li>
 *   <li><b>PICK</b> - adds one track to a playlist: every row is a target playlist. The mode has no
 *       name field and no create button - playlists are created elsewhere.</li>
 * </ul>
 *
 * <p>The panel follows the global shape and carries no outline. The rows start at the slanted left
 * edge of the panel, so they run along the diagonal of the shape and slide along it while the list
 * scrolls. The list scrolls with the wheel and is clipped to the panel. Clicking outside the panel
 * or on the close icon closes the overlay.</p>
 */
public class PhiPlaylistOverlay extends PhiComponent implements PhiScrollable {

    /** The mode of the overlay. */
    public enum Mode { MANAGE, PICK }

    /** The actions the overlay offers to the music page. */
    public interface Listener {

        /** Closes the overlay. */
        void onClose();

        /**
         * Applies the name field: the rename of the managed playlist (MANAGE mode). The panel
         * confirms the name as soon as the editing ends, so there is no apply button.
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
        private float hoverAmount;
        private final PhiSheen sheen = new PhiSheen();
    }

    private final float uiScale;

    private final PhiSearchField nameField;

    private Mode mode = Mode.MANAGE;

    private String playlistName = "";

    private Listener listener;

    private final List<Row> rows = new ArrayList<Row>();

    private final PhiScrollState scrollState = new PhiScrollState();

    /** Geometry of the last frame (the hit tests reuse it). */
    private float listTop;
    private float rowHeight;
    private float buttonY;
    private float deleteX;
    private float deleteWidth;
    private float closeX;
    private float closeY;
    private float closeSize;

    private final PhiSheen deleteSheen = new PhiSheen();
    private float deleteHover;
    private float closeHover;
    private long lastFrame = System.currentTimeMillis();

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
        // It is a plain name field here: no search icon, and the rename is confirmed as soon as
        // the editing ends (return key or focus loss).
        this.nameField.setShowIcon(false);
        this.nameField.setSubmitListener(new PhiSearchField.SubmitListener() {
            @Override
            public void onSubmit(String text) {
                if (PhiPlaylistOverlay.this.listener != null) {
                    PhiPlaylistOverlay.this.listener.onApplyName(text);
                }
            }
        });
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
        this.scrollState.reset();
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
        this.scrollState.reset();
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
        this.scrollState.scrollBy(notches * this.rowHeight);
        return true;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = 16.0F * this.uiScale;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;
        float buttonHeight = 26.0F * this.uiScale;

        // The close icon sits at the right end of the title row.
        this.closeSize = titleSize;
        this.closeY = this.y + padding * 0.8F;
        // The title row belongs to the slanted panel: align its right action with the right edge at
        // the row's own height, not with the bottom-right content corner far to its left.
        this.closeX = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height,
                this.closeY + this.closeSize * 0.5F) - padding - this.closeSize;

        // The delete button shares the title row, so the name field can use the full width.
        this.deleteX = 0.0F;
        this.deleteWidth = 0.0F;
        float titleRight = this.closeX - padding * 0.6F;
        String deleteLabel = PhiI18n.get("music.playlist.delete");
        if (this.mode == Mode.MANAGE) {
            this.deleteWidth = nvg.textWidth(deleteLabel, bodySize, Fonts.REGULAR) + padding * 1.2F
                    + PhiSkew.offset(buttonHeight);
            this.deleteX = titleRight - this.deleteWidth;
            titleRight = this.deleteX - padding * 0.5F;
        }

        boolean closeHovered = insideBox(mouseX, mouseY, this.closeX, this.closeY, this.closeSize);
        this.closeHover = approach(this.closeHover, closeHovered, deltaMs);
        boolean deleteHovered = this.mode == Mode.MANAGE && PhiSkew.contains(mouseX, mouseY,
                this.deleteX, this.closeY, this.deleteWidth, buttonHeight);
        this.deleteHover = approach(this.deleteHover, deleteHovered, deltaMs);
        this.deleteSheen.update(deleteHovered);

        if (this.mode == Mode.MANAGE) {
            float buttonRadius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
            nvg.drawSkewRect(this.deleteX, this.closeY, this.deleteWidth, buttonHeight,
                    buttonRadius, PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), this.deleteHover));
            this.deleteSheen.drawSkew(nvg, this.deleteX, this.closeY, this.deleteWidth, buttonHeight,
                    buttonRadius, this.deleteHover, PhiTheme.sheen());
            nvg.drawText(deleteLabel, PhiSkew.contentLeft(this.deleteX, buttonHeight, padding * 0.6F),
                    this.closeY + (buttonHeight - bodySize) * 0.5F,
                    PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.danger(), this.deleteHover),
                    bodySize, Fonts.REGULAR);
        }

        drawCross(nvg, this.closeX + this.closeSize * 0.5F, this.closeY + this.closeSize * 0.5F,
                this.closeSize * (0.5F + 0.05F * this.closeHover),
                PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), this.closeHover));

        // The title takes the rest of the title row.
        String title = this.mode == Mode.MANAGE
                ? PhiI18n.get("music.playlist.manage") : PhiI18n.get("music.playlist.add");
        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR,
                        Math.max(0.0F, titleRight - contentLeft)),
                contentLeft, this.y + padding * 0.8F, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        // The name field (MANAGE mode) spans the whole content width. The rename is confirmed by
        // pressing return or by clicking somewhere else - there is no rename button.
        this.rowHeight = 30.0F * this.uiScale;
        float fieldHeight = 26.0F * this.uiScale;
        this.buttonY = this.y + padding * 0.8F + titleSize + padding * 0.7F;

        if (this.mode == Mode.MANAGE) {
            this.nameField.setX(contentLeft);
            this.nameField.setY(this.buttonY);
            this.nameField.setWidth(Math.max(20.0F, contentRight - contentLeft));
            this.nameField.setHeight(fieldHeight);
            this.nameField.draw(nvg, mouseX, mouseY, partialTicks);
        }

        // The member / target list, clipped to the panel.
        this.listTop = this.buttonY + (this.mode == Mode.MANAGE
                ? fieldHeight + padding * 0.6F : padding * 0.4F);
        float listBottom = this.y + this.height - padding * 0.6F;
        this.scrollState.setContentSize(this.rows.size() * this.rowHeight,
                Math.max(0.0F, listBottom - this.listTop));
        this.scrollState.update(deltaMs);

        nvg.save();
        nvg.scissor(this.x, this.listTop, this.width, Math.max(0.0F, listBottom - this.listTop));

        for (int i = 0; i < this.rows.size(); i++) {
            float rowY = this.listTop + i * this.rowHeight - this.scrollState.getOffset();
            if (rowY + this.rowHeight < this.listTop || rowY > listBottom) {
                continue;
            }
            drawRow(nvg, this.rows.get(i), rowY, bodySize, mouseX, mouseY, deltaMs);
        }

        nvg.restore();
    }

    /**
     * Draws one row of the list. Every row starts at the slanted left edge of the panel, so the
     * entries run along the diagonal of the shape - and they slide along it while the list scrolls.
     */
    private void drawRow(NanoVGManager nvg, Row row, float rowY, float bodySize, int mouseX, int mouseY,
                         float deltaMs) {
        float padding = 16.0F * this.uiScale;
        float rowLeft = PhiSkew.leftEdgeAt(this.x, this.y, this.height, rowY) + padding;
        float rowWidth = this.width - padding * 2.0F;

        boolean hovered = PhiSkew.contains(mouseX, mouseY, rowLeft, rowY, rowWidth, this.rowHeight);
        row.hoverAmount = approach(row.hoverAmount, hovered, deltaMs);
        row.sheen.update(hovered);
        float radius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        if (row.hoverAmount > 0.001F) {
            nvg.drawSkewRect(rowLeft, rowY, rowWidth, this.rowHeight, radius,
                    PhiTheme.withAlpha(PhiTheme.level3(), row.hoverAmount));
        }
        row.sheen.drawSkew(nvg, rowLeft, rowY, rowWidth, this.rowHeight, radius,
                row.hoverAmount, PhiTheme.sheen());

        int iconColor = PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), row.hoverAmount);
        float textRight = rowLeft + rowWidth;
        if (this.mode == Mode.MANAGE) {
            // Move-up, move-down and remove icons at the right end of the row.
            float iconSize = this.rowHeight * 0.5F;
            float centerY = rowY + this.rowHeight * 0.5F;
            drawCross(nvg, textRight - iconSize * 0.5F, centerY, iconSize * 0.42F, iconColor);
            drawTriangle(nvg, textRight - iconSize * 1.8F, centerY, iconSize * 0.42F, false, iconColor);
            drawTriangle(nvg, textRight - iconSize * 3.1F, centerY, iconSize * 0.42F, true, iconColor);
            textRight = textRight - iconSize * 3.6F;
        }

        nvg.drawText(nvg.limitText(row.title, bodySize, Fonts.REGULAR,
                        Math.max(0.0F, textRight - rowLeft)),
                rowLeft, rowY + (this.rowHeight - bodySize) * 0.5F,
                PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);
    }

    /**
     * Moves a hover amount toward its target (120 ms in, 300 ms out).
     */
    private static float approach(float current, boolean hovered, float deltaMs) {
        return hovered
                ? Math.min(1.0F, current + deltaMs / 120.0F)
                : Math.max(0.0F, current - deltaMs / 300.0F);
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

        // The delete button of the title row.
        if (this.mode == Mode.MANAGE && this.deleteWidth > 0.0F
                && mouseY >= this.closeY && mouseY <= this.closeY + 26.0F * this.uiScale
                && mouseX >= this.deleteX && mouseX <= this.deleteX + this.deleteWidth) {
            this.listener.onDelete();
            return;
        }

        // Forward to the name field (focus handling, the rename is confirmed on focus loss).
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);

        // Rows.
        if (mouseY < this.listTop) {
            return;
        }
        int index = (int) ((mouseY - this.listTop + this.scrollState.getOffset()) / this.rowHeight);
        if (index < 0 || index >= this.rows.size()) {
            return;
        }
        Row row = this.rows.get(index);
        row.sheen.restart();

        if (this.mode == Mode.PICK) {
            this.listener.onPick(row.playlistName);
            return;
        }

        float rowY = this.listTop + index * this.rowHeight - this.scrollState.getOffset();
        float padding = 16.0F * this.uiScale;
        float rowLeft = PhiSkew.leftEdgeAt(this.x, this.y, this.height, rowY) + padding;
        float rowWidth = this.width - padding * 2.0F;
        float iconSize = this.rowHeight * 0.5F;
        float textRight = rowLeft + rowWidth;

        if (mouseX >= textRight - iconSize) {
            this.listener.onRemove(row.trackId);
        } else if (mouseX >= textRight - iconSize * 2.3F) {
            this.listener.onMove(row.trackId, false);
        } else if (mouseX >= textRight - iconSize * 3.6F) {
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
