package org.dpdns.syscrashed.phiclient.gui.component;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiSheen;
import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;
import org.lwjgl.input.Keyboard;

/**
 * The generic confirmation overlay of the interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>It asks the user to confirm an action that cannot be undone - the deletion of a song file or
 * the replacement of an already imported song. The panel follows the global parallelogram shape,
 * the title and the message share one content column and the two buttons sit at the bottom right of
 * the panel. A destructive action paints its confirm button in the danger color of the theme.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #show(String, String, String, boolean)} - fills the panel with a new question.</li>
 *   <li>{@link #setListener(Listener)} - the callbacks of the confirm and the cancel button.</li>
 * </ul>
 */
public class PhiConfirmOverlay extends PhiComponent {

    /** The actions of the confirmation panel. */
    public interface Listener {

        /** The action was confirmed. */
        void onConfirm();

        /** The action was cancelled. */
        void onCancel();
    }

    private final float uiScale;

    private Listener listener;

    private String title = "";

    private String message = "";

    private String confirmLabel = "";

    private boolean dangerous;

    private final PhiSheen cancelSheen = new PhiSheen();
    private final PhiSheen confirmSheen = new PhiSheen();
    private float cancelHover;
    private float confirmHover;
    private long lastFrame = System.currentTimeMillis();

    /** Geometry of the last frame (the hit tests reuse it). */
    private float buttonY;
    private float buttonHeight;
    private float confirmX;
    private float confirmWidth;
    private float cancelX;
    private float cancelWidth;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     */
    public PhiConfirmOverlay(float x, float y, float width, float height, float uiScale) {
        super(x, y, width, height);
        this.uiScale = uiScale;
    }

    /**
     * @param listener the action callback.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Fills the panel with a new question.
     *
     * @param title        the headline of the question.
     * @param message      the explanation below the headline.
     * @param confirmLabel the label of the confirm button.
     * @param dangerous    true if the action destroys something (the confirm button turns red).
     */
    public void show(String title, String message, String confirmLabel, boolean dangerous) {
        this.title = title != null ? title : "";
        this.message = message != null ? message : "";
        this.confirmLabel = confirmLabel != null ? confirmLabel : "";
        this.dangerous = dangerous;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;
        float padding = 16.0F * this.uiScale;
        float contentLeft = PhiSkew.contentLeft(this.x, this.height, padding);
        float contentRight = PhiSkew.contentRight(this.x, this.width, padding);
        float contentWidth = Math.max(0.0F, contentRight - contentLeft);

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level2());

        float titleSize = PhiMetrics.FONT_TITLE * this.uiScale;
        float bodySize = PhiMetrics.FONT_BODY * this.uiScale;

        nvg.drawText(nvg.limitText(this.title, titleSize, Fonts.REGULAR, contentWidth),
                contentLeft, this.y + padding * 0.9F, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);
        nvg.drawText(nvg.limitText(this.message, bodySize, Fonts.REGULAR, contentWidth),
                contentLeft, this.y + padding * 0.9F + titleSize * 1.45F,
                PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

        // The buttons at the bottom right: cancel first, confirm last.
        this.buttonHeight = 26.0F * this.uiScale;
        this.buttonY = this.y + this.height - padding * 0.8F - this.buttonHeight;

        float labelWidth = nvg.textWidth(this.confirmLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                + PhiSkew.offset(this.buttonHeight);
        this.confirmWidth = labelWidth;
        this.confirmX = contentRight - this.confirmWidth;

        String cancelLabel = PhiI18n.get("music.confirm.no");
        this.cancelWidth = nvg.textWidth(cancelLabel, bodySize, Fonts.REGULAR) + padding * 1.4F
                + PhiSkew.offset(this.buttonHeight);
        this.cancelX = this.confirmX - this.cancelWidth - padding * 0.5F;

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;
        boolean cancelHovered = PhiSkew.contains(mouseX, mouseY, this.cancelX, this.buttonY,
                this.cancelWidth, this.buttonHeight);
        boolean confirmHovered = PhiSkew.contains(mouseX, mouseY, this.confirmX, this.buttonY,
                this.confirmWidth, this.buttonHeight);
        this.cancelHover = approach(this.cancelHover, cancelHovered, deltaMs);
        this.confirmHover = approach(this.confirmHover, confirmHovered, deltaMs);
        this.cancelSheen.update(cancelHovered);
        this.confirmSheen.update(confirmHovered);

        float buttonRadius = PhiMetrics.NAV_ITEM_RADIUS * this.uiScale;
        nvg.drawSkewRect(this.cancelX, this.buttonY, this.cancelWidth, this.buttonHeight,
                buttonRadius, PhiTheme.mix(PhiTheme.level3(), PhiTheme.level4(), this.cancelHover));
        this.cancelSheen.drawSkew(nvg, this.cancelX, this.buttonY, this.cancelWidth, this.buttonHeight,
                buttonRadius, this.cancelHover, PhiTheme.sheen());
        nvg.drawText(cancelLabel, PhiSkew.contentLeft(this.cancelX, this.buttonHeight, padding * 0.7F),
                this.buttonY + (this.buttonHeight - bodySize) * 0.5F,
                PhiTheme.mix(PhiTheme.textSecondary(), PhiTheme.textPrimary(), this.cancelHover),
                bodySize, Fonts.REGULAR);

        int confirmBase = this.dangerous ? PhiTheme.level3() : PhiTheme.level4();
        int confirmHoveredColor = this.dangerous ? PhiTheme.level4() : PhiTheme.level5();
        nvg.drawSkewRect(this.confirmX, this.buttonY, this.confirmWidth, this.buttonHeight,
                buttonRadius, PhiTheme.mix(confirmBase, confirmHoveredColor, this.confirmHover));
        this.confirmSheen.drawSkew(nvg, this.confirmX, this.buttonY, this.confirmWidth, this.buttonHeight,
                buttonRadius, this.confirmHover, PhiTheme.sheen());
        nvg.drawText(this.confirmLabel, PhiSkew.contentLeft(this.confirmX, this.buttonHeight, padding * 0.7F),
                this.buttonY + (this.buttonHeight - bodySize) * 0.5F,
                this.dangerous ? PhiTheme.danger() : PhiTheme.accent(), bodySize, Fonts.REGULAR);
    }

    /**
     * Moves a button hover amount toward its target (120 ms in, 300 ms out).
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
            this.listener.onCancel();
            return;
        }

        // The button hit test matches the hover test (PhiSkew.contains), so the pointer and the
        // highlight always agree.
        if (PhiSkew.contains(mouseX, mouseY, this.confirmX, this.buttonY, this.confirmWidth, this.buttonHeight)) {
            this.confirmSheen.restart();
            this.listener.onConfirm();
            return;
        }
        if (PhiSkew.contains(mouseX, mouseY, this.cancelX, this.buttonY, this.cancelWidth, this.buttonHeight)) {
            this.cancelSheen.restart();
            this.listener.onCancel();
            return;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.listener == null) {
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            this.listener.onConfirm();
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            this.listener.onCancel();
        }
    }
}
