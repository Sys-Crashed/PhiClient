package org.dpdns.syscrashed.phiclient.gui.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.PhiClient;
import org.dpdns.syscrashed.phiclient.gui.PhiGuiManager;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiHubLayout;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.navigation.PhiNavBar;
import org.dpdns.syscrashed.phiclient.gui.screen.category.HomeCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.HudCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.ModulesCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.MusicCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.PhiCategory;
import org.dpdns.syscrashed.phiclient.gui.screen.category.SettingsCategory;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiToast;
import org.dpdns.syscrashed.phiclient.utils.animation.Animation;
import org.dpdns.syscrashed.phiclient.utils.animation.Direction;
import org.dpdns.syscrashed.phiclient.utils.animation.EaseOutCubic;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

/**
 * The central hub of PhiClient - opened with the Right-Shift key
 * (package {@code org.dpdns.syscrashed.phiclient.gui.screen}).
 *
 * <p>Architecture of the window (design specification):</p>
 * <ul>
 *   <li>the window is a single parallelogram with 75 / 105 degree angles whose bounding box has a 16:9
 *       ratio and covers roughly half of the game window; its size follows the game window with a
 *       smooth transition and it is always centered ({@link PhiHubLayout});</li>
 *   <li>the <b>right</b> part is the content area of the active page ({@link PhiCategory}), the
 *       <b>left</b> part is the slanted navigation band ({@link PhiNavBar});</li>
 *   <li>the background is a Minecraft shader blur, everything else is drawn with NanoVG, so the fonts
 *       of the game are not affected.</li>
 * </ul>
 *
 * <p>Why a real {@link GuiScreen}: while a screen is open, Minecraft skips the raw mouse / keyboard
 * handling of the game, so the player cannot act on the world while the hub is visible. Because this
 * class overrides {@link #handleInput()}, it also has to dispatch the mouse events to itself
 * (click, release and wheel) - otherwise the interface would not react to the mouse at all.</p>
 *
 * <p>Provided overrides:</p>
 * <ul>
 *   <li>{@link #initGui()} - resets the open animation.</li>
 *   <li>{@link #drawScreen(int, int, float)} - drives the blur, the NanoVG frame and the layout.</li>
 *   <li>{@link #mouseClicked(int, int, int)} / {@link #mouseReleased(int, int, int)} - the navigation
 *       band and the page.</li>
 *   <li>{@link #handleInput()} - dispatches the mouse events and recognizes ESC / Right-Shift as
 *       "close".</li>
 *   <li>{@link #doesGuiPauseGame()} - returns false (the game keeps running while open).</li>
 *   <li>{@link #onGuiClosed()} - clears held keys so the player does not drift after closing.</li>
 * </ul>
 */
public class GuiPhiHub extends GuiScreen {

    /** A single mouse wheel notch is reported with this delta by LWJGL. */
    private static final float WHEEL_DELTA_PER_NOTCH = 120.0F;

    private final Minecraft mc = Minecraft.getMinecraft();

    private final PhiHubLayout layout = new PhiHubLayout();

    private final List<PhiCategory> categories = new ArrayList<PhiCategory>();

    private final PhiNavBar navBar = new PhiNavBar();

    private PhiCategory currentCategory;

    private final Animation introAnimation = new EaseOutCubic(260, 1.0D);

    /** Animation of the current page sliding up after a navigation switch (0 = just switched). */
    private final EaseOutCubic categoryTransition = new EaseOutCubic(320, 1.0D);

    /** The width the pages were laid out for (a change triggers a re-layout). */
    private float laidOutWidth = -1.0F;

    /** The height the pages were laid out for (a change triggers a re-layout). */
    private float laidOutHeight = -1.0F;

    private long lastFrameTime;

    /**
     * The Right-Shift press that opened the screen may leak in as a "repeat" event, so every
     * Right-Shift press is ignored until one key release was received.
     */
    private boolean awaitingRightShiftRelease = true;

    /**
     * Creates the hub and registers the pages of the navigation band.
     */
    public GuiPhiHub() {
        HomeCategory home = new HomeCategory();

        // The quick access tools of the home page open the other pages of the hub.
        home.setPageSwitcher(new PhiCategory.PageSwitcher() {
            @Override
            public void openPage(String pageId) {
                for (PhiCategory category : GuiPhiHub.this.categories) {
                    if (category.getName().equalsIgnoreCase(pageId)) {
                        selectCategory(category);
                        return;
                    }
                }
            }
        });

        this.categories.add(home);
        this.categories.add(new ModulesCategory());
        this.categories.add(new MusicCategory());
        this.categories.add(new SettingsCategory());
        this.categories.add(new HudCategory());

        this.currentCategory = this.categories.get(0);

        this.navBar.setCategories(this.categories);
        this.navBar.setSelected(this.currentCategory);
        this.navBar.setSelectionListener(new PhiNavBar.SelectionListener() {
            @Override
            public void onCategorySelected(PhiCategory category) {
                selectCategory(category);
            }
        });
    }

    @Override
    public void initGui() {
        super.initGui();

        this.introAnimation.reset();
        this.introAnimation.setDirection(Direction.FORWARDS);
        this.lastFrameTime = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PhiGuiManager gui = PhiClient.getInstance().getGuiManager();

        // Once the close animation finished, actually close the screen.
        if (this.introAnimation.isDone(Direction.BACKWARDS)) {
            this.mc.displayGuiScreen(null);
            return;
        }

        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrameTime);
        this.lastFrameTime = now;

        this.layout.update(this.mc.displayWidth, this.mc.displayHeight, this.width, this.height, deltaMs);
        updateLayout();

        this.navBar.layout(this.layout);
        this.navBar.update(mouseX, mouseY, deltaMs);

        float intro = (float) Math.min(1.0D, this.introAnimation.getValue());

        // 1) Background blur (written back to MC's main framebuffer, covering the sharp scene).
        gui.getBlurRenderer().render(partialTicks, 1.0F + intro * 19.0F);

        // 2) Draw the whole interface with NanoVG.
        NanoVGManager nvg = gui.getNanoVGManager();
        if (!nvg.ensureReady()) {
            return;
        }

        final int mouseXFinal = mouseX;
        final int mouseYFinal = mouseY;
        nvg.setupAndDraw(new Runnable() {
            @Override
            public void run() {
                drawHub(nvg, mouseXFinal, mouseYFinal, partialTicks, intro);
            }
        }, true);
    }

    /**
     * Draws the hub window: the window body, the navigation band, the active page and the tooltip.
     *
     * @param nvg          the NanoVG drawing context.
     * @param mouseX       the pointer x.
     * @param mouseY       the pointer y.
     * @param partialTicks the render interpolation factor.
     * @param intro        the progress of the open animation.
     */
    private void drawHub(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks, float intro) {
        float centerX = this.layout.getHubX() + this.layout.getHubWidth() * 0.5F;
        float centerY = this.layout.getHubY() + this.layout.getHubHeight() * 0.5F;
        float scale = 0.94F + 0.06F * intro;

        nvg.save();
        nvg.translate(centerX, centerY);
        nvg.scale(scale, scale);
        nvg.translate(-centerX, -centerY);

        drawWindow(nvg);

        // Content area of the active page. After a navigation switch the new page slides up from
        // below and fades in, while the previous page is hidden immediately.
        float pageProgress = (float) this.categoryTransition.getValueFloat();
        float pageShift = (1.0F - pageProgress) * 38.0F * this.layout.getUiScale();

        nvg.save();
        nvg.setGlobalAlpha(pageProgress);
        nvg.translate(0.0F, pageShift);
        this.currentCategory.draw(nvg, this.layout, mouseX, mouseY, partialTicks);
        nvg.restore();
        nvg.resetGlobalAlpha();

        // Navigation band entries.
        this.navBar.draw(nvg, mouseX, mouseY, partialTicks);

        nvg.restore();

        // The tooltip is drawn above everything - it may overlap the content area.
        this.navBar.drawTooltip(nvg, this.width, this.height);

        // The toast confirms an action at the top of the game window.
        PhiToast toast = PhiClient.getInstance().getGuiManager().getToast();
        toast.update();
        toast.draw(nvg, this.width, this.height, this.layout.getUiScale());
    }

    /**
     * Draws the window body and the navigation band. Both are built from sheared rounded rectangles,
     * so the band shares the exact corner arcs of the window it is placed in.
     *
     * <p>While the theme changes, the window is not simply cross faded: it is wiped diagonally from the
     * lower left corner to the upper right corner.</p>
     */
    private void drawWindow(NanoVGManager nvg) {
        float hubX = this.layout.getHubX();
        float hubY = this.layout.getHubY();
        float hubWidth = this.layout.getHubWidth();
        float hubHeight = this.layout.getHubHeight();
        float offset = this.layout.getSkewOffset();
        float radius = PhiMetrics.CORNER_RADIUS * this.layout.getUiScale();
        float bandWidth = this.layout.getBandWidth();

        nvg.save();
        nvg.translate(hubX + offset, hubY);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);

        if (PhiTheme.isTransitioning()) {
            drawWiped(nvg, 0.0F, 0.0F, hubWidth, hubHeight, radius,
                    PhiTheme.previous().level1, PhiTheme.active().level1, false);
            drawWiped(nvg, 0.0F, 0.0F, bandWidth, hubHeight, radius,
                    PhiTheme.previous().level2, PhiTheme.active().level2, true);
        } else {
            // Window body (the global rounded parallelogram).
            nvg.drawRoundedRect(0.0F, 0.0F, hubWidth, hubHeight, radius, PhiTheme.level1());

            // Navigation band on the left side: it hugs the slanted left edge of the window, so its
            // outer corners share the corner arcs of the window while its inner edge stays straight.
            nvg.drawRoundedRectVarying(0.0F, 0.0F, bandWidth, hubHeight, radius, 0.0F, 0.0F, radius, PhiTheme.level2());
        }

        nvg.restore();
    }

    /**
     * Draws a shape that is wiped diagonally: the lower left corner changes first, the upper right
     * corner last.
     *
     * @param nvg      the NanoVG drawing context.
     * @param x        the left x of the shape.
     * @param y        the top y of the shape.
     * @param width    the width of the shape.
     * @param height   the height of the shape.
     * @param radius   the corner radius of the shape.
     * @param oldColor the color before the transition.
     * @param newColor the color after the transition.
     * @param varying  whether the shape uses different radii for its corners.
     */
    private void drawWiped(NanoVGManager nvg, float x, float y, float width, float height, float radius,
                           int oldColor, int newColor, boolean varying) {
        final int strips = 48;
        float stripHeight = height / strips;
        float progress = PhiTheme.transition();

        for (int i = 0; i < strips; i++) {
            float stripY = i * stripHeight;

            // The axis of the wipe - the lower left corner comes first, the upper right one last.
            float axis = (1.0F - (i + 0.5F) / strips) * 0.5F;
            float local = (progress - axis * 0.55F) / 0.45F;
            local = Math.max(0.0F, Math.min(1.0F, local));

            int color = PhiTheme.mix(oldColor, newColor, local);

            nvg.scissor(x, y + stripY, width, stripHeight + 1.0F);

            if (varying) {
                nvg.drawRoundedRectVarying(x, y, width, height, radius, 0.0F, 0.0F, radius, color);
            } else {
                nvg.drawRoundedRect(x, y, width, height, radius, color);
            }
        }

        nvg.resetScissor();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.navBar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }

        this.currentCategory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (this.navBar.mouseReleased(mouseX, mouseY, mouseButton)) {
            return;
        }

        this.currentCategory.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        this.currentCategory.keyTyped(typedChar, keyCode);
    }

    /**
     * Dispatches the mouse events to the interface and swallows every other input: while the hub is
     * open the player cannot act on the game world. Only ESC and Right-Shift close the screen.
     *
     * @throws IOException never thrown, kept for the signature of the base class.
     */
    @Override
    public void handleInput() throws IOException {
        if (Mouse.isCreated()) {
            while (Mouse.next()) {
                int button = Mouse.getEventButton();
                int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
                int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

                if (button == -1) {
                    // A wheel event (the button index is -1 for those). Only the content of the page
                    // reacts to it - the navigation band does not scroll.
                    int wheel = Mouse.getEventDWheel();
                    if (wheel != 0) {
                        this.currentCategory.onWheel(-wheel / WHEEL_DELTA_PER_NOTCH, x, y);
                    }
                    continue;
                }

                if (Mouse.getEventButtonState()) {
                    this.mouseClicked(x, y, button);
                } else {
                    this.mouseReleased(x, y, button);
                }
            }
        }

        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                int key = Keyboard.getEventKey();
                boolean pressed = Keyboard.getEventKeyState();

                if (key == Keyboard.KEY_ESCAPE && pressed) {
                    close();
                    return;
                }

                if (key == Keyboard.KEY_RSHIFT) {
                    if (pressed) {
                        if (!this.awaitingRightShiftRelease) {
                            close();
                            return;
                        }
                    } else {
                        this.awaitingRightShiftRelease = false;
                    }
                }
            }
        }
    }

    /** The game keeps running normally (does not pause) while this screen is open. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        // Keys like W / Shift may be held when the screen opened; clear them so the player does not
        // drift after closing.
        KeyBinding.unPressAllKeys();
    }

    /**
     * Selects a page and shows it.
     *
     * @param category the page to show.
     */
    private void selectCategory(PhiCategory category) {
        if (category == null || category == this.currentCategory) {
            return;
        }

        this.currentCategory = category;
        this.navBar.setSelected(category);

        // Restart the slide-up entrance of the new page.
        this.categoryTransition.reset();
        this.categoryTransition.setDirection(Direction.FORWARDS);
    }

    /**
     * Rebuilds the layout of the pages when the geometry of the hub changed.
     */
    private void updateLayout() {
        if (Math.abs(this.laidOutWidth - this.layout.getHubWidth()) < 0.5F
                && Math.abs(this.laidOutHeight - this.layout.getHubHeight()) < 0.5F) {
            return;
        }

        this.laidOutWidth = this.layout.getHubWidth();
        this.laidOutHeight = this.layout.getHubHeight();

        for (PhiCategory category : this.categories) {
            category.layout(this.layout);
        }
    }

    /**
     * Closes the screen: plays the exit animation, after which {@link #drawScreen} returns to the game.
     */
    private void close() {
        this.introAnimation.setDirection(Direction.BACKWARDS);
    }
}
