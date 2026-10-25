package org.dpdns.syscrashed.phiclient.gui.screen.category;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.component.PhiCard;
import org.dpdns.syscrashed.phiclient.gui.component.PhiComponent;
import org.dpdns.syscrashed.phiclient.gui.component.PhiScrollable;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;

/**
 * A page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
 *
 * <p>Every page knows its display name and its navigation icon, owns a list of components and lays
 * them out into the content area of the hub. The content area is not a plain rectangle: the left
 * border of the hub is slanted, so a component that is placed flush against it has to follow the skew.
 * The helpers {@link #contentLeft(PhiHubLayout, float)} and {@link #contentRight(PhiHubLayout)} take
 * care of that.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getName()} / {@link #getIcon()} - the name and the icon of the navigation entry.</li>
 *   <li>{@link #layout(PhiHubLayout)} - rebuilds the component tree for the current hub geometry.</li>
 *   <li>{@link #draw(NanoVGManager, PhiHubLayout, int, int, float)} - renders the page.</li>
 *   <li>{@link #mouseClicked(int, int, int)} / {@link #mouseReleased(int, int, int)} /
 *       {@link #keyTyped(char, int)} - forwards the interaction to the components.</li>
 *   <li>{@link #clearComponents()} - drops all components (called before a re-layout).</li>
 * </ul>
 */
public abstract class PhiCategory {

    /**
     * Callback that opens another page of the hub (package
     * {@code org.dpdns.syscrashed.phiclient.gui.screen.category}).
     */
    public interface PageSwitcher {

        /**
         * @param pageId the id of the page ("modules", "music", "hud" or "setting").
         */
        void openPage(String pageId);
    }

    private final String name;

    private final PhiIcon icon;

    /** The components of this page, positioned in absolute (scaled) coordinates. */
    protected final List<PhiComponent> components = new ArrayList<PhiComponent>();

    /** The callback that opens another page (may be null). */
    private PageSwitcher pageSwitcher;

    /**
     * @param name the display name of the page (also used as the tooltip of the navigation entry).
     * @param icon the icon of the navigation entry.
     */
    protected PhiCategory(String name, PhiIcon icon) {
        this.name = name;
        this.icon = icon;
    }

    /**
     * @return the display name of the page.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the icon of the navigation entry.
     */
    public PhiIcon getIcon() {
        return this.icon;
    }

    /**
     * @param pageSwitcher the callback that opens another page of the hub.
     */
    public void setPageSwitcher(PageSwitcher pageSwitcher) {
        this.pageSwitcher = pageSwitcher;
    }

    /**
     * @return the callback that opens another page of the hub (null when there is none).
     */
    protected PageSwitcher getPageSwitcher() {
        return this.pageSwitcher;
    }

    /**
     * Rebuilds the component tree of the page. Called whenever the hub geometry changed.
     *
     * @param layout the current geometry of the hub.
     */
    public void layout(PhiHubLayout layout) {
    }

    /**
     * Draws every component of the page.
     *
     * @param nvg          the NanoVG drawing context.
     * @param layout       the current geometry of the hub.
     * @param mouseX       the pointer x.
     * @param mouseY       the pointer y.
     * @param partialTicks the render interpolation factor.
     */
    public void draw(NanoVGManager nvg, PhiHubLayout layout, int mouseX, int mouseY, float partialTicks) {
        for (PhiComponent component : this.components) {
            component.draw(nvg, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Forwards a click to every component of the page.
     *
     * @param mouseX      the click x.
     * @param mouseY      the click y.
     * @param mouseButton the mouse button index.
     */
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (PhiComponent component : this.components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Forwards a release to every component of the page.
     *
     * @param mouseX      the release x.
     * @param mouseY      the release y.
     * @param mouseButton the mouse button index.
     */
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (PhiComponent component : this.components) {
            component.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Forwards a typed key to every component of the page.
     *
     * @param typedChar the typed character.
     * @param keyCode   the key code.
     */
    public void keyTyped(char typedChar, int keyCode) {
        for (PhiComponent component : this.components) {
            component.keyTyped(typedChar, keyCode);
        }
    }

    /**
     * Forwards a wheel event to every component that can be scrolled.
     *
     * @param notches the number of wheel notches (positive scrolls towards the end of the content).
     * @param mouseX  the pointer x.
     * @param mouseY  the pointer y.
     * @return true if one of the components consumed the wheel.
     */
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        boolean handled = false;

        for (PhiComponent component : this.components) {
            if (component instanceof PhiScrollable) {
                handled |= ((PhiScrollable) component).onWheel(notches, mouseX, mouseY);
            }
        }

        return handled;
    }

    /**
     * Removes all components of the page.
     */
    protected void clearComponents() {
        this.components.clear();
    }

    /**
     * @param layout the current geometry of the hub.
     * @param y      the vertical position of the element that is placed against the navigation band.
     * @return the left x of the content area at the given vertical position.
     */
    protected float contentLeft(PhiHubLayout layout, float y) {
        return layout.getBandInnerEdgeXAt(y) + layout.getPadding();
    }

    /**
     * @param layout the current geometry of the hub.
     * @param y      the vertical position of the element that is placed against the right side.
     * @return the right x of the content area at the given vertical position.
     */
    protected float contentRight(PhiHubLayout layout, float y) {
        return layout.getRightEdgeXAt(y) - layout.getPadding();
    }

    /**
     * Adds a standard content card to the page. The card is placed flush against the slanted left
     * edge of the hub, so it stays inside the window at every height.
     *
     * @param layout     the current geometry of the hub.
     * @param top        the top y of the card.
     * @param height     the height of the card.
     * @param title      the headline of the card.
     * @param description the secondary line of the card.
     * @param status     the right aligned status text of the card.
     */
    protected void addCard(PhiHubLayout layout, float top, float height, String title, String description, String status) {
        float bottom = top + height;
        float x = contentLeft(layout, bottom);
        float width = contentRight(layout, bottom) - x;

        this.components.add(new PhiCard(x, top, width, height, layout.getUiScale(), title, description, status));
    }
}
