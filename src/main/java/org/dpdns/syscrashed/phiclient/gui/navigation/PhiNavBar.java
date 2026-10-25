package org.dpdns.syscrashed.phiclient.gui.navigation;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.screen.category.PhiCategory;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiRipple;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiTooltip;

/**
 * The navigation band of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.navigation}).
 *
 * <p>The band lives on the <b>left</b> side of the hub and follows the slanted left edge of the window.
 * Every entry is a parallelogram of the global shape family whose left edge lies exactly on that
 * slanted edge, so the band keeps a constant width at every height. The entries share one vertical
 * alignment line on their left side.</p>
 *
 * <p>Interaction (design specification): the entries show their icon only, they are transparent by
 * default, use the hover level when the pointer is over them and the selected level (plus the accent
 * color for the icon) when they are the active page. A press produces a short 0.08 s feedback. There
 * is no disabled state - a feature that is not available is simply not listed.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #setCategories(List)} / {@link #setSelectionListener(SelectionListener)} - configuration.</li>
 *   <li>{@link #setSelected(PhiCategory)} / {@link #getSelected()} - the active page.</li>
 *   <li>{@link #layout(PhiHubLayout)} - recomputes the geometry of the entries.</li>
 *   <li>{@link #update(int, int, float)} - the hover detection and the tooltip.</li>
 *   <li>{@link #draw(NanoVGManager, int, int, float)} / {@link #drawTooltip(NanoVGManager, int, int)}
 *       - rendering.</li>
 *   <li>{@link #mouseClicked(int, int, int)} / {@link #mouseReleased(int, int, int)} - interaction.</li>
 * </ul>
 */
public class PhiNavBar {

    /** Callback that is invoked when the user selects a navigation entry. */
    public interface SelectionListener {
        /**
         * @param category the entry that was selected.
         */
        void onCategorySelected(PhiCategory category);
    }

    /** Duration of the press feedback. */
    private static final float PRESS_FEEDBACK_MS = 80.0F;

    private final List<PhiCategory> categories = new ArrayList<PhiCategory>();

    private final PhiTooltip tooltip = new PhiTooltip();

    private SelectionListener listener;

    private PhiCategory selected;

    private float itemHeight;
    private float itemSpacing;
    private float itemsTopY;

    private float[] itemTops = new float[0];
    private float[] itemWidths = new float[0];
    private float[] itemLeftXs = new float[0];

    private int hoveredIndex = -1;
    private int pressedIndex = -1;
    private int pressTargetIndex = -1;
    private long pressStart;

    private float tooltipAnchorX;
    private float tooltipAnchorY;

    /** Progress of the hover reveal of the active entry (0 = no hover, 1 = fully hovered). */
    private float hoverAmount;

    /** The point the hover reveal of the active entry spreads from. */
    private float hoverOriginX;
    private float hoverOriginY;

    private int hoverIndexPrev = -1;

    /**
     * @return the entries of the navigation band.
     */
    public List<PhiCategory> getCategories() {
        return this.categories;
    }

    /**
     * Replaces the entries of the navigation band.
     *
     * @param categories the new entries (in display order).
     */
    public void setCategories(List<PhiCategory> categories) {
        this.categories.clear();
        this.categories.addAll(categories);

        this.itemTops = new float[this.categories.size()];
        this.itemWidths = new float[this.categories.size()];
        this.itemLeftXs = new float[this.categories.size()];
    }

    /**
     * @param listener the callback that receives the selected entry.
     */
    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    /**
     * @param category the entry that should be marked as active (may be null).
     */
    public void setSelected(PhiCategory category) {
        this.selected = category;
    }

    /**
     * @return the entry that is currently active.
     */
    public PhiCategory getSelected() {
        return this.selected;
    }

    /**
     * Recomputes the geometry of the entries for the current hub layout.
     *
     * @param layout the current geometry of the hub.
     */
    public void layout(PhiHubLayout layout) {
        this.itemHeight = PhiMetrics.NAV_ITEM_HEIGHT * layout.getUiScale();
        this.itemSpacing = PhiMetrics.NAV_ITEM_SPACING * layout.getUiScale();
        this.itemsTopY = layout.getHubY() + layout.getPadding();

        float padding = PhiMetrics.NAV_PADDING * layout.getUiScale();
        float width = Math.max(1.0F, layout.getBandWidth() - padding * 2.0F);

        for (int i = 0; i < this.categories.size(); i++) {
            float top = this.itemsTopY + i * (this.itemHeight + this.itemSpacing);
            this.itemTops[i] = top;
            this.itemWidths[i] = width;

            // The band is a ribbon of constant width that hugs the slanted left edge of the hub, so
            // every entry starts a little further to the right the higher it is placed.
            this.itemLeftXs[i] = layout.getBandOuterEdgeXAt(top + this.itemHeight) + padding;
        }
    }

    /**
     * Updates the hover state, the scroll inertia and the tooltip.
     *
     * @param mouseX   the pointer x.
     * @param mouseY   the pointer y.
     * @param deltaMs  the time in milliseconds since the previous frame.
     */
    public void update(int mouseX, int mouseY, float deltaMs) {
        if (this.pressedIndex >= 0 && System.currentTimeMillis() - this.pressStart > PRESS_FEEDBACK_MS) {
            // The short press feedback has expired, but the button is still held down.
            this.pressedIndex = -1;
        }

        this.hoveredIndex = findEntry(mouseX, mouseY);

        // Animate the hover reveal toward the active entry.
        if (this.hoveredIndex >= 0) {
            if (this.hoverIndexPrev != this.hoveredIndex) {
                this.hoverOriginX = mouseX;
                this.hoverOriginY = mouseY;
            }
            float k = 1.0F - (float) Math.exp(-deltaMs / 90.0F);
            this.hoverAmount += (1.0F - this.hoverAmount) * Math.min(1.0F, k);
        } else {
            float k = 1.0F - (float) Math.exp(-deltaMs / 90.0F);
            this.hoverAmount += (0.0F - this.hoverAmount) * Math.min(1.0F, k);
        }
        this.hoverIndexPrev = this.hoveredIndex;

        String text = "";

        if (this.hoveredIndex >= 0) {
            PhiCategory category = this.categories.get(this.hoveredIndex);
            text = category.getName();

            float top = this.itemTops[this.hoveredIndex];
            float width = this.itemWidths[this.hoveredIndex];
            float offset = PhiSkew.offset(this.itemHeight);

            // The anchor is only updated while an entry is hovered. When the pointer leaves, the
            // tooltip keeps its position and fades out in place instead of following the mouse.
            this.tooltipAnchorX = this.itemLeftXs[this.hoveredIndex] + width * 0.5F + offset * 0.5F;
            this.tooltipAnchorY = top + this.itemHeight * 0.5F;
        }

        this.tooltip.update(text, this.hoveredIndex >= 0, deltaMs);
    }

    /**
     * Draws the entries of the navigation band.
     *
     * @param nvg          the NanoVG drawing context.
     * @param mouseX       the pointer x.
     * @param mouseY       the pointer y.
     * @param partialTicks the render interpolation factor.
     */
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        float uiScale = this.itemHeight / Math.max(1.0F, PhiMetrics.NAV_ITEM_HEIGHT);
        float iconSize = PhiMetrics.NAV_ICON_SIZE * uiScale;
        float radius = PhiMetrics.NAV_ITEM_RADIUS * uiScale;
        float offset = PhiSkew.offset(this.itemHeight);

        for (int i = 0; i < this.categories.size(); i++) {
            PhiCategory category = this.categories.get(i);
            boolean isSelected = category == this.selected;
            boolean isHovered = i == this.hoveredIndex;

            float top = this.itemTops[i];
            float width = this.itemWidths[i];

            if (width <= 0.0F) {
                continue;
            }

            // Base background: the selected entry uses the selected level, otherwise the card level.
            int base = isSelected ? PhiTheme.level5() : PhiTheme.level3();
            nvg.drawSkewRect(this.itemLeftXs[i], top, width, this.itemHeight, radius, base);

            // The hover color spreads from the pointer position outward. A press flashes the accent.
            if (i == this.hoveredIndex && this.hoverAmount > 0.001F) {
                int revealColor = (i == this.pressedIndex)
                        ? PhiTheme.accent()
                        : (isSelected ? PhiTheme.mix(PhiTheme.level5(), PhiTheme.accent(), 0.45F) : PhiTheme.level4());
                PhiRipple.drawReveal(nvg, this.itemLeftXs[i], top, width, this.itemHeight, radius,
                        this.hoverOriginX, this.hoverOriginY, this.hoverAmount, revealColor);
            }

            int iconColor = isSelected ? PhiTheme.accent() : (isHovered ? PhiTheme.textPrimary() : PhiTheme.textSecondary());
            float centerX = this.itemLeftXs[i] + width * 0.5F + offset * 0.5F;
            float centerY = top + this.itemHeight * 0.5F;

            category.getIcon().draw(nvg, centerX, centerY, iconSize, iconColor);
        }
    }

    /**
     * Draws the tooltip of the hovered entry (called after the content so that it stays on top).
     *
     * @param nvg          the NanoVG drawing context.
     * @param screenWidth  the width of the game window.
     * @param screenHeight the height of the game window.
     */
    public void drawTooltip(NanoVGManager nvg, int screenWidth, int screenHeight) {
        if (!this.tooltip.isVisible()) {
            return;
        }

        float uiScale = this.itemHeight / Math.max(1.0F, PhiMetrics.NAV_ITEM_HEIGHT);
        this.tooltip.draw(nvg, this.tooltipAnchorX, this.tooltipAnchorY, screenWidth, screenHeight, uiScale);
    }

    /**
     * Handles a mouse click on the navigation band.
     *
     * @param mouseX      the click x.
     * @param mouseY      the click y.
     * @param mouseButton the mouse button index.
     * @return true if the click was consumed by the navigation band.
     */
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }

        int index = findEntry(mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        this.pressedIndex = index;
        this.pressTargetIndex = index;
        this.pressStart = System.currentTimeMillis();
        return true;
    }

    /**
     * Handles a mouse release on the navigation band and selects the released entry.
     *
     * @param mouseX      the release x.
     * @param mouseY      the release y.
     * @param mouseButton the mouse button index.
     * @return true if the release was consumed by the navigation band.
     */
    public boolean mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || this.pressTargetIndex < 0) {
            return false;
        }

        int index = findEntry(mouseX, mouseY);
        int pressed = this.pressTargetIndex;

        this.pressedIndex = -1;
        this.pressTargetIndex = -1;

        if (index >= 0 && index == pressed) {
            PhiCategory category = this.categories.get(index);
            this.selected = category;

            if (this.listener != null) {
                this.listener.onCategorySelected(category);
            }
        }

        return true;
    }

    /**
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     * @return the index of the entry below the pointer (-1 if there is none).
     */
    private int findEntry(int mouseX, int mouseY) {
        for (int i = 0; i < this.categories.size(); i++) {
            if (PhiSkew.contains(mouseX, mouseY, this.itemLeftXs[i], this.itemTops[i], this.itemWidths[i], this.itemHeight)) {
                return i;
            }
        }

        return -1;
    }
}
