package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.shape.PhiRoundedSquareShape;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * The scrollable song list of the music page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The list has no visible scrollbar: the wheel adds velocity to the scroll offset, the velocity
 * decays by 0.92 per frame and the offset springs back at the borders. Only the rows inside the
 * viewport are drawn (clipped by a scissor), so long libraries stay cheap. The list scrolls along
 * the diagonal of the hub (like the change log): one pixel up also moves a card {@link PhiSkew#SLOPE}
 * pixels to the right, so the cards stay flush against the slanted left edge instead of drifting
 * sideways.</p>
 *
 * <p>The owner builds the {@link PhiSongCard} rows and hands them over with {@link #setCards(List)};
 * the list only positions and draws them and forwards the mouse events (the cards are already at
 * their scrolled position, so their own hit tests stay valid).</p>
 */
public class PhiSongList extends PhiComponent implements PhiScrollable {

    /** The velocity decay per frame (the inertia of the design specification). */
    private static final float FRICTION = 0.92F;

    /** The speed one wheel notch adds (in pixels per frame). */
    private static final float WHEEL_SPEED = 9.0F;

    /** The spring factor of the border bounce. */
    private static final float BOUNCE = 0.25F;

    private final List<PhiSongCard> cards = new ArrayList<PhiSongCard>();

    private float rowHeight;

    private float rowGap;

    private float scroll;

    private float velocity;

    /**
     * @param x       the left x of the viewport.
     * @param y       the top y of the viewport.
     * @param width   the width of the viewport.
     * @param height  the height of the viewport.
     * @param rowHeight the height of one row.
     * @param rowGap    the vertical gap between two rows.
     */
    public PhiSongList(float x, float y, float width, float height, float rowHeight, float rowGap) {
        // The viewport is an axis aligned rectangle, so the wheel hit test is axis aligned too.
        super(x, y, width, height, PhiRoundedSquareShape.INSTANCE);
        this.rowHeight = rowHeight;
        this.rowGap = rowGap;
    }

    /**
     * Replaces the rows of the list (called when the view or the filter changes).
     *
     * @param newCards the new rows.
     */
    public void setCards(List<PhiSongCard> newCards) {
        this.cards.clear();
        if (newCards != null) {
            this.cards.addAll(newCards);
        }
        clampScroll();
    }

    /**
     * @return the number of rows.
     */
    public int getCardCount() {
        return this.cards.size();
    }

    /**
     * Moves the viewport (used by a re-layout).
     *
     * @param height the new viewport height.
     */
    @Override
    public void setHeight(float height) {
        super.setHeight(height);
        clampScroll();
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY)) {
            return false;
        }
        this.velocity += notches * WHEEL_SPEED;
        return true;
    }

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        advance();

        // The visible area is the slanted band between the left and right edge of the hub, not the
        // axis aligned viewport: a card near the bottom leans further left. The scissor is therefore
        // expressed in the sheared coordinate system, exactly like the change log list.
        nvg.save();
        nvg.translate(this.x, this.y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);
        nvg.scissor(0.0F, 0.0F, this.width, this.height);
        nvg.skewX(-PhiSkew.SHEAR_DEGREES);
        nvg.translate(-this.x, -this.y);

        float stride = this.rowHeight + this.rowGap;
        float slide = PhiSkew.SLOPE * this.scroll;
        for (int i = 0; i < this.cards.size(); i++) {
            float rowY = this.y + i * stride - this.scroll;
            if (rowY + this.rowHeight < this.y || rowY > this.y + this.height) {
                continue; // outside the viewport
            }
            PhiSongCard card = this.cards.get(i);
            // The x anchor is measured at the un-scrolled row position and slides along the diagonal
            // with the scroll offset, so a card always stays flush against the slanted left edge.
            float rowX = this.x + PhiSkew.SLOPE * (this.scroll - i * stride);
            card.setX(rowX);
            card.setY(rowY);
            card.draw(nvg, mouseX, mouseY, partialTicks);
        }

        nvg.restore();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        forwardClick(mouseX, mouseY, mouseButton, false);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        forwardClick(mouseX, mouseY, mouseButton, true);
    }

    /**
     * Forwards a mouse event to the visible cards only. The hit boxes of the cards follow their
     * scrolled position, so the rows outside the viewport must be skipped - their stale coordinates
     * would otherwise swallow the click.
     */
    private void forwardClick(int mouseX, int mouseY, int mouseButton, boolean release) {
        float stride = this.rowHeight + this.rowGap;
        for (int i = 0; i < this.cards.size(); i++) {
            float rowY = this.y + i * stride - this.scroll;
            if (rowY + this.rowHeight < this.y || rowY > this.y + this.height) {
                continue; // outside the viewport
            }
            PhiSongCard card = this.cards.get(i);
            if (release) {
                card.mouseReleased(mouseX, mouseY, mouseButton);
            } else {
                card.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    /**
     * Advances the inertia and the border bounce by one frame.
     */
    private void advance() {
        this.scroll += this.velocity;
        this.velocity *= FRICTION;
        if (Math.abs(this.velocity) < 0.05F) {
            this.velocity = 0.0F;
        }

        float maxScroll = Math.max(0.0F, this.cards.size() * (this.rowHeight + this.rowGap)
                - this.rowGap - this.height);
        if (this.scroll > maxScroll) {
            this.scroll += (maxScroll - this.scroll) * BOUNCE;
            this.velocity = 0.0F;
        } else if (this.scroll < 0.0F) {
            this.scroll += (0.0F - this.scroll) * BOUNCE;
            this.velocity = 0.0F;
        }
    }

    /**
     * Clamps the scroll offset into the valid range (used after the rows changed).
     */
    private void clampScroll() {
        float maxScroll = Math.max(0.0F, this.cards.size() * (this.rowHeight + this.rowGap)
                - this.rowGap - this.height);
        this.scroll = Math.max(0.0F, Math.min(this.scroll, maxScroll));
        this.velocity = 0.0F;
    }
}
