package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcon;
import org.dpdns.syscrashed.phiclient.gui.icon.PhiIcons;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiMetrics;
import org.dpdns.syscrashed.phiclient.gui.layout.PhiSkew;
import org.dpdns.syscrashed.phiclient.gui.module.PhiModule;
import org.dpdns.syscrashed.phiclient.gui.module.PhiModuleSetting;
import org.dpdns.syscrashed.phiclient.gui.nanovg.NanoVGManager;
import org.dpdns.syscrashed.phiclient.gui.nanovg.font.Fonts;
import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;
import org.dpdns.syscrashed.phiclient.gui.util.PhiRipple;
import org.dpdns.syscrashed.phiclient.gui.widget.PhiScrollState;
import org.dpdns.syscrashed.phiclient.utils.animation.Direction;
import org.dpdns.syscrashed.phiclient.utils.animation.EaseOutCubic;

/**
 * The modules page of the PhiHub (package {@code org.dpdns.syscrashed.phiclient.gui.component}).
 *
 * <p>The page is split into two areas that share one content rectangle:</p>
 * <ul>
 *   <li><b>the dial list</b> on the left: a center-focused list of the modules. The selected entry
 *       sits in the vertical center on a {@code L0} background and shows the name, the description
 *       and - for modules with parameters - a square {@code L5} "more settings" button on its right
 *       edge. A click on an entry slides it to the center (0.2 s ease-out), a stopped wheel snaps the
 *       nearest entry to the center, and the upper / lower edges fade the entries out with a gradient
 *       mask;</li>
 *   <li><b>the detail panel</b> on the right: a large card with the icon of the selected module, a
 *       plain decorative strip and a toggle switch that enables / disables the module.</li>
 * </ul>
 *
 * <p>The "more settings" button slides the Soar-style settings panel over the page: the list and the
 * detail panel move out to the left while the panel - the name of the module, a reset and a back
 * button and a two-column grid of the parameters (toggle / slider / combo box) - moves in from the
 * right. The panel is left with the back button or with a right click.</p>
 */
public class PhiModulePanel extends PhiComponent implements PhiScrollable {

    // ------------------------------------------------------------------ metrics (design pixels)

    /** Height of the header line. */
    private static final float HEADER_HEIGHT = 48.0F;

    /** Inset between the panel and its content. */
    private static final float PANEL_PADDING = 18.0F;

    /** Share of the content width the dial list takes. */
    private static final float LIST_RATIO = 0.44F;

    /** Horizontal distance between the dial list and the detail panel. */
    private static final float DETAIL_GAP = 20.0F;

    /** Height of a row of the dial list. */
    private static final float ROW_HEIGHT = 46.0F;

    /** Vertical gap between two rows of the dial list. */
    private static final float ROW_GAP = 6.0F;

    /** Inset between a row and its text. */
    private static final float ROW_PADDING = 12.0F;

    /** Edge length of the square "more settings" button. */
    private static final float GEAR_SIZE = 28.0F;

    /** Edge length of the square reset / back buttons of the settings panel. */
    private static final float ICON_BUTTON = 24.0F;

    /** Width / height of a toggle switch. */
    private static final float TOGGLE_WIDTH = 34.0F;
    private static final float TOGGLE_HEIGHT = 18.0F;

    /** Height of a row of the settings grid. */
    private static final float SETTING_ROW_HEIGHT = 46.0F;

    /** Horizontal gap between the two columns of the settings grid. */
    private static final float SETTING_COL_GAP = 28.0F;

    /** Height of a combo box and of one of its options. */
    private static final float COMBO_HEIGHT = 22.0F;
    private static final float COMBO_OPTION_HEIGHT = 24.0F;

    /** Height of a slider track and edge length of its handle. */
    private static final float SLIDER_HEIGHT = 6.0F;
    private static final float HANDLE_SIZE = 14.0F;

    /** Height of the gradient fades on the upper / lower edge of the dial list. */
    private static final float FADE_HEIGHT = 40.0F;

    /** Height of the decorative strip and edge length of the icon of the detail panel. */
    private static final float DETAIL_STRIP_HEIGHT = 14.0F;
    private static final float DETAIL_ICON_SIZE = 48.0F;

    /** Scroll distance of a single wheel notch. */
    private static final float SCROLL_PER_NOTCH = 30.0F;

    /** Duration of the snap-to-center animation in milliseconds. */
    private static final int SNAP_MS = 200;

    /** Duration of the slide between the page and the settings panel. */
    private static final int TRANSITION_MS = 320;

    /** How long after the last wheel input the list snaps to the nearest entry. */
    private static final long SNAP_IDLE_MS = 120L;

    // ------------------------------------------------------------------ state

    private final List<PhiModule> modules;

    private final float uiScale;

    /** The toggle switches of the settings panel (rebuilt when a module is opened). */
    private final List<PhiToggle> settingToggles = new ArrayList<PhiToggle>();

    /** The scroll state of the dial list. */
    private final PhiScrollState listScroll = new PhiScrollState();

    /** The slide between the page and the settings panel (0 = page, 1 = settings). */
    private final EaseOutCubic transition = new EaseOutCubic(TRANSITION_MS, 1.0D);

    /** The toggle switch of the detail panel. */
    private final PhiToggle detailToggle;

    /** The snap-to-center animation of the dial list (null = none). */
    private EaseOutCubic snapAnim;

    /** The offsets the running snap animation moves between. */
    private float snapFrom;
    private float snapTo;

    /** The index of the module whose settings are open (-1 = page view). */
    private int openModule = -1;

    /**
     * Whether the transition plays the closing direction. The underlying animation only runs forwards
     * (reversing a finished animation makes its value jump to the start instantly), so the closing
     * slide maps the progress to {@code 1 - value} instead.
     */
    private boolean closing;

    /** The index of the combo box that shows its options (-1 = none). */
    private int comboOpenIndex = -1;

    /** The index of the setting whose slider is being dragged (-1 = none). */
    private int draggingSlider = -1;

    /** The index of the selected (centered) module of the dial list. */
    private int focus;

    /** Whether the initial scroll position was set. */
    private boolean scrollInitialized;

    /** The moment of the last wheel input. */
    private long lastWheelTime;

    /** The hover trackers of the interactive elements. */
    private final HoverTracker rowHover = new HoverTracker();
    private final HoverTracker gearHover = new HoverTracker();
    private final HoverTracker iconHover = new HoverTracker();
    private final HoverTracker comboHover = new HoverTracker();
    private final HoverTracker optionHover = new HoverTracker();

    private long lastFrame;

    /**
     * @param x       the left x of the bounding box.
     * @param y       the top y of the bounding box.
     * @param width   the width of the bounding box.
     * @param height  the height of the bounding box.
     * @param uiScale the design scale factor of the interface.
     * @param modules the modules to show.
     */
    public PhiModulePanel(float x, float y, float width, float height, float uiScale, List<PhiModule> modules) {
        super(x, y, width, height);

        this.uiScale = uiScale;
        this.modules = modules;
        this.lastFrame = System.currentTimeMillis();

        this.detailToggle = new PhiToggle(0.0F, 0.0F, TOGGLE_WIDTH * uiScale, TOGGLE_HEIGHT * uiScale, false);
        this.detailToggle.setOnToggled(new PhiToggle.Listener() {
            @Override
            public void onToggled(boolean value) {
                if (PhiModulePanel.this.focus >= 0 && PhiModulePanel.this.focus < PhiModulePanel.this.modules.size()) {
                    PhiModulePanel.this.modules.get(PhiModulePanel.this.focus).setToggled(value);
                }
            }
        });
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.openModule < 0) {
            clickedPage(mouseX, mouseY, mouseButton);
            return;
        }

        if (transitionValue() < 1.0F) {
            return;
        }

        clickedSettings(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.draggingSlider = -1;
    }

    @Override
    public boolean onWheel(float notches, int mouseX, int mouseY) {
        if (!PhiSkew.contains(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return false;
        }

        if (this.openModule < 0) {
            this.snapAnim = null;
            this.lastWheelTime = System.currentTimeMillis();
            this.listScroll.scrollBy(notches * SCROLL_PER_NOTCH * this.uiScale);
        }

        return true;
    }

    /**
     * Handles a click while the page view is shown.
     */
    private void clickedPage(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        float t = this.openModule >= 0 ? transitionValue() : 0.0F;
        int mx = mouseX + (int) (t * slideShift());

        // The square "more settings" button of the selected row opens the settings panel.
        if (this.focus >= 0 && this.focus < this.modules.size()) {
            PhiModule module = this.modules.get(this.focus);

            if (module.hasSettings()) {
                float[] gear = gearBounds();

                if (PhiSkew.contains(mx, mouseY, gear[0], gear[1], GEAR_SIZE * this.uiScale,
                        GEAR_SIZE * this.uiScale)) {
                    openSettings(this.focus);
                    return;
                }
            }
        }

        // The toggle switch of the detail panel.
        this.detailToggle.mouseClicked(mx, mouseY, mouseButton);

        // The row body selects the module and slides it to the center.
        for (int i = 0; i < this.modules.size(); i++) {
            float rowY = rowY(i);

            if (!PhiSkew.contains(mx, mouseY, listX(), rowY, listW(), ROW_HEIGHT * this.uiScale)) {
                continue;
            }

            if (i != this.focus) {
                snapTo(i);
            }

            return;
        }
    }

    /**
     * Handles a click while the settings panel is shown.
     */
    private void clickedSettings(int mouseX, int mouseY, int mouseButton) {
        PhiModule module = this.modules.get(this.openModule);

        // A right click leaves the settings panel, like in the Soar client.
        if (mouseButton == 1) {
            closeSettings();
            return;
        }

        if (mouseButton != 0) {
            return;
        }

        // The open combo box handles its own hits first.
        if (this.comboOpenIndex >= 0) {
            float[] layer = comboLayerBounds(this.comboOpenIndex);

            if (layer != null) {
                int option = optionAt(layer, mouseX, mouseY);

                if (option >= 0) {
                    module.getSettings().get(this.comboOpenIndex).setSelectedIndex(option);
                    this.comboOpenIndex = -1;
                    return;
                }

                if (PhiSkew.contains(mouseX, mouseY, layer[0], layer[1], layer[2], layer[3])) {
                    return;
                }
            }

            this.comboOpenIndex = -1;
        }

        // The square back and reset buttons of the header.
        for (int i = 0; i < 2; i++) {
            float[] icon = iconBounds(i);

            if (PhiSkew.contains(mouseX, mouseY, icon[0], icon[1], ICON_BUTTON * this.uiScale,
                    ICON_BUTTON * this.uiScale)) {
                if (i == 1) {
                    closeSettings();
                } else {
                    module.reset();
                    syncSettingToggles(module);
                }

                return;
            }
        }

        List<PhiModuleSetting> settings = module.getSettings();

        for (int i = 0; i < settings.size(); i++) {
            PhiModuleSetting setting = settings.get(i);
            float[] cell = settingCell(i);

            switch (setting.getType()) {
                case TOGGLE:
                    if (i < this.settingToggles.size()) {
                        this.settingToggles.get(i).mouseClicked(mouseX, mouseY, mouseButton);
                    }
                    break;
                case SLIDER:
                    float[] slider = sliderBounds(cell, setting);

                    if (mouseX >= slider[0] - HANDLE_SIZE * this.uiScale && mouseX <= slider[0] + slider[2]
                            + HANDLE_SIZE * this.uiScale && mouseY >= slider[1] - HANDLE_SIZE * this.uiScale
                            && mouseY <= slider[1] + slider[3] + HANDLE_SIZE * this.uiScale) {
                        this.draggingSlider = i;
                        updateSlider(setting, slider, mouseX);
                    }
                    break;
                case COMBO:
                    float[] combo = comboBounds(cell);

                    if (PhiSkew.contains(mouseX, mouseY, combo[0], combo[1], combo[2], combo[3])) {
                        this.comboOpenIndex = (this.comboOpenIndex == i ? -1 : i);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Opens the settings panel of a module.
     *
     * @param index the index of the module.
     */
    private void openSettings(int index) {
        this.openModule = index;
        this.closing = false;
        this.comboOpenIndex = -1;
        this.draggingSlider = -1;
        this.snapAnim = null;
        syncSettingToggles(this.modules.get(index));

        this.transition.reset();
        this.transition.setDirection(Direction.FORWARDS);
    }

    /**
     * Leaves the settings panel and slides back to the page.
     */
    private void closeSettings() {
        if (this.openModule < 0) {
            return;
        }

        this.comboOpenIndex = -1;
        this.draggingSlider = -1;
        this.closing = true;
        this.transition.reset();
    }

    /**
     * @return the progress of the page / settings transition (0 = page, 1 = settings panel).
     */
    private float transitionValue() {
        float value = this.transition.getValueFloat();
        return this.closing ? 1.0F - value : value;
    }

    /**
     * Rebuilds the toggle switches of the settings panel from the parameters of a module.
     *
     * @param module the module whose panel is open.
     */
    private void syncSettingToggles(final PhiModule module) {
        this.settingToggles.clear();

        for (final PhiModuleSetting setting : module.getSettings()) {
            if (setting.getType() != PhiModuleSetting.Type.TOGGLE) {
                this.settingToggles.add(null);
                continue;
            }

            PhiToggle toggle = new PhiToggle(0.0F, 0.0F, TOGGLE_WIDTH * this.uiScale, TOGGLE_HEIGHT * this.uiScale,
                    setting.getBoolean());
            toggle.setOnToggled(new PhiToggle.Listener() {
                @Override
                public void onToggled(boolean value) {
                    setting.setBoolean(value);
                }
            });

            this.settingToggles.add(toggle);
        }
    }

    /**
     * Sets the value of a slider from the pointer position while it is dragged.
     *
     * @param setting the slider setting.
     * @param slider  the bounds of the track ({@code x, y, width, height}).
     * @param mouseX  the current pointer x.
     */
    private void updateSlider(PhiModuleSetting setting, float[] slider, int mouseX) {
        float ratio = Math.max(0.0F, Math.min(1.0F, (mouseX - slider[0]) / slider[2]));
        float value = setting.getMin() + (setting.getMax() - setting.getMin()) * ratio;

        setting.setFloat(Math.max(setting.getMin(), Math.min(setting.getMax(), value)));
    }

    /**
     * Selects the module and starts the snap animation that slides it to the vertical center.
     *
     * @param index the index of the module.
     */
    private void snapTo(int index) {
        this.focus = index;
        this.snapFrom = this.listScroll.getOffset();
        this.snapTo = offsetTarget(index);

        if (Math.abs(this.snapTo - this.snapFrom) < 0.5F) {
            this.snapAnim = null;
            return;
        }

        this.snapAnim = new EaseOutCubic(SNAP_MS, 1.0D);
        this.snapAnim.setDirection(Direction.FORWARDS);
    }

    // ------------------------------------------------------------------ geometry

    /**
     * @return the top y of the content area (below the header).
     */
    private float listTop() {
        return this.y + HEADER_HEIGHT * this.uiScale;
    }

    /**
     * @return the bottom y of the content area.
     */
    private float listBottom() {
        return this.y + this.height - PANEL_PADDING * this.uiScale;
    }

    /**
     * @return the height of the visible list.
     */
    private float viewportHeight() {
        return Math.max(1.0F, listBottom() - listTop());
    }

    /**
     * @return the pitch between two rows of the dial list.
     */
    private float pitch() {
        return (ROW_HEIGHT + ROW_GAP) * this.uiScale;
    }

    /**
     * @return the left x of the content rectangle the whole page shares.
     */
    private float contentLeft() {
        return PhiSkew.contentLeft(this.x, this.height, PANEL_PADDING * this.uiScale);
    }

    /**
     * @return the right x of the content rectangle the whole page shares.
     */
    private float contentRight() {
        return PhiSkew.contentRight(this.x, this.width, PANEL_PADDING * this.uiScale);
    }

    /**
     * @return the left x of the dial list.
     */
    private float listX() {
        return contentLeft();
    }

    /**
     * @return the width of the dial list.
     */
    private float listW() {
        return (contentRight() - contentLeft()) * LIST_RATIO - DETAIL_GAP * 0.5F * this.uiScale;
    }

    /**
     * @return the left x of the detail panel.
     */
    private float detailX() {
        return listX() + listW() + DETAIL_GAP * this.uiScale;
    }

    /**
     * @return the width of the detail panel.
     */
    private float detailW() {
        return contentRight() - detailX();
    }

    /**
     * @return the left x of the text of a dial row.
     */
    private float rowContentLeft() {
        return listX() + PhiSkew.offset(ROW_HEIGHT * this.uiScale) + ROW_PADDING * this.uiScale;
    }

    /**
     * @param index the index of a module.
     * @return the scroll offset that centers the module in the viewport.
     */
    private float offsetTarget(int index) {
        return index * pitch() + ROW_HEIGHT * this.uiScale * 0.5F - viewportHeight() * 0.5F;
    }

    /**
     * @param index the index of a module.
     * @return the top y of its row.
     */
    private float rowY(int index) {
        return listTop() + index * pitch() - this.listScroll.getOffset();
    }

    /**
     * @return the index of the module that is closest to the vertical center.
     */
    private int focusIndex() {
        int index = Math.round((this.listScroll.getOffset() - offsetTarget(0)) / pitch());
        return Math.max(0, Math.min(this.modules.size() - 1, index));
    }

    /**
     * @return the bounds of the square "more settings" button of the selected row
     *         ({@code {x, y}}).
     */
    private float[] gearBounds() {
        float u = this.uiScale;
        float x = listX() + listW() - GEAR_SIZE * u * 0.5F;
        float y = rowY(this.focus) + (ROW_HEIGHT * u - GEAR_SIZE * u) * 0.5F;

        return new float[] { x, y };
    }

    /**
     * @return how far the page and the settings panel travel during the slide transition.
     */
    private float slideShift() {
        return this.width * 0.55F;
    }

    /**
     * @param index 0 = reset, 1 = back.
     * @return the bounds of the square header button ({@code {x, y}}).
     */
    private float[] iconBounds(int index) {
        float u = this.uiScale;
        float cR = contentRight();

        float backX = cR - ICON_BUTTON * u;
        float iconY = this.y + 12.0F * u;
        float resetX = backX - ICON_BUTTON * u - 8.0F * u;

        return index == 1 ? new float[] { backX, iconY } : new float[] { resetX, iconY };
    }

    /**
     * @param index the index of the setting.
     * @return the bounds of the cell of the setting: {@code {x, width, top}}.
     */
    private float[] settingCell(int index) {
        float u = this.uiScale;
        int col = index % 2;
        int rowIdx = index / 2;

        float contentW = contentRight() - contentLeft();
        float cellW = (contentW - ROW_PADDING * u * 2.0F - SETTING_COL_GAP * u) * 0.5F;
        float cellX = contentLeft() + ROW_PADDING * u + col * (cellW + SETTING_COL_GAP * u);
        float top = listTop() + 10.0F * u + rowIdx * SETTING_ROW_HEIGHT * u;

        return new float[] { cellX, cellW, top };
    }

    /**
     * @param cell    the cell of the setting.
     * @param setting the slider setting.
     * @return the bounds of the track ({@code {x, y, width, height}}).
     */
    private float[] sliderBounds(float[] cell, PhiModuleSetting setting) {
        float u = this.uiScale;
        float trackW = cell[1] * 0.45F;
        float trackX = cell[0] + cell[1] - trackW;
        float trackY = cell[2] + (SETTING_ROW_HEIGHT * u - SLIDER_HEIGHT * u) * 0.5F;

        return new float[] { trackX, trackY, trackW, SLIDER_HEIGHT * u };
    }

    /**
     * @param cell the cell of the setting.
     * @return the bounds of the combo box button ({@code {x, y, width, height}}).
     */
    private float[] comboBounds(float[] cell) {
        float u = this.uiScale;
        float w = cell[1] * 0.5F;
        float h = COMBO_HEIGHT * u;
        float x = cell[0] + cell[1] - w;
        float y = cell[2] + (SETTING_ROW_HEIGHT * u - h) * 0.5F;

        return new float[] { x, y, w, h };
    }

    /**
     * @param index the index of the open combo box.
     * @return the bounds of the option layer ({@code {x, y, width, height}}), flipped upwards when it
     *         would leave the panel.
     */
    private float[] comboLayerBounds(int index) {
        PhiModule module = this.openModule >= 0 ? this.modules.get(this.openModule) : null;

        if (module == null || index >= module.getSettings().size()) {
            return null;
        }

        PhiModuleSetting setting = module.getSettings().get(index);

        if (setting.getType() != PhiModuleSetting.Type.COMBO) {
            return null;
        }

        float u = this.uiScale;
        float[] combo = comboBounds(settingCell(index));

        float w = combo[2] + ROW_PADDING * u * 2.0F;
        float h = setting.getOptions().length * COMBO_OPTION_HEIGHT * u + ROW_PADDING * u;
        float x = combo[0] - ROW_PADDING * u;
        float y = combo[1] + combo[3] + 4.0F * u;

        if (y + h > listBottom()) {
            y = combo[1] - 4.0F * u - h;
        }

        return new float[] { x, y, w, h };
    }

    /**
     * @param layer the bounds of the option layer.
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     * @return the index of the option under the pointer (-1 = none).
     */
    private int optionAt(float[] layer, int mouseX, int mouseY) {
        float u = this.uiScale;

        if (!PhiSkew.contains(mouseX, mouseY, layer[0], layer[1], layer[2], layer[3])) {
            return -1;
        }

        int index = (int) ((mouseY - layer[1] - ROW_PADDING * u * 0.5F) / (COMBO_OPTION_HEIGHT * u));

        return index;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        drawFrame(nvg, mouseX, mouseY, partialTicks);
    }

    /**
     * Advances the animations and draws the page: the dial list and the detail panel slide out to the
     * left while the settings panel slides in from the right.
     */
    private void drawFrame(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        update(deltaMs, now);

        float t = transitionValue();
        float shift = slideShift() * t;

        // The component is clipped to its bounding box, so the sliding views never draw over the
        // navigation band or the rest of the hub.
        nvg.save();
        nvg.scissor(this.x, this.y, this.width, this.height);

        nvg.translate(-shift, 0.0F);
        drawPage(nvg, mouseX + shift, mouseY, partialTicks);

        // The settings panel travels the rest of the way back, so it ends exactly where the page was.
        nvg.translate(slideShift(), 0.0F);

        if (this.openModule >= 0) {
            drawSettingsPanel(nvg, mouseX, mouseY, partialTicks);
        }

        nvg.restore();
        nvg.resetScissor();
    }

    /**
     * Advances the scroll state, the snap-to-center animation and the page / settings transition.
     */
    private void update(float deltaMs, long now) {
        if (!this.scrollInitialized) {
            this.scrollInitialized = true;

            this.listScroll.setContentSize(this.modules.size() * pitch(), viewportHeight());
            this.listScroll.setOffsetRange(offsetTarget(0), offsetTarget(this.modules.size() - 1));
            this.listScroll.setOffset(offsetTarget(0));
        }

        // Once the closing slide is over, the page view is shown again.
        if (this.closing && this.transition.getValueFloat() >= 1.0F) {
            this.closing = false;
            this.openModule = -1;
        }

        // The snap animation controls the offset frame by frame.
        if (this.snapAnim != null) {
            float progress = this.snapAnim.getValueFloat();

            this.listScroll.setOffset(this.snapFrom + (this.snapTo - this.snapFrom) * progress);

            if (progress >= 1.0F) {
                this.snapAnim = null;
            }

            return;
        }

        this.listScroll.update(deltaMs);

        if (this.openModule >= 0) {
            return;
        }

        // While the list moves, the focus follows the row that currently sits in the center; once the
        // wheel input settled, the nearest row is snapped to the center.
        if (this.listScroll.getVelocity() != 0.0F) {
            this.focus = focusIndex();
            return;
        }

        if (now - this.lastWheelTime < SNAP_IDLE_MS) {
            return;
        }

        int target = focusIndex();

        if (Math.abs(this.listScroll.getOffset() - offsetTarget(target)) > 0.5F) {
            snapTo(target);
        } else {
            this.focus = target;
        }
    }

    /**
     * Draws the page view: the dial list with its fade masks and the detail panel.
     */
    private void drawPage(NanoVGManager nvg, float mouseX, float mouseY, float partialTicks) {
        float u = this.uiScale;
        float rowH = ROW_HEIGHT * u;

        int first = Math.max(0, (int) Math.floor((this.listScroll.getOffset() - rowH) / pitch()));
        int last = Math.min(this.modules.size() - 1,
                (int) Math.ceil((viewportHeight() + this.listScroll.getOffset()) / pitch()));

        int hoverIndex = -1;

        for (int i = first; i <= last; i++) {
            if (PhiSkew.contains(mouseX, mouseY, listX(), rowY(i), listW(), rowH)) {
                hoverIndex = i;
                break;
            }
        }

        this.rowHover.update(hoverIndex, mouseX, mouseY);

        for (int i = first; i <= last; i++) {
            drawRow(nvg, i, mouseX, mouseY);
        }

        drawListFades(nvg);
        drawDetail(nvg, mouseX, mouseY, partialTicks);
    }

    /**
     * Draws a single row of the dial list.
     *
     * @param nvg    the NanoVG drawing context.
     * @param index  the index of the module.
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     */
    private void drawRow(NanoVGManager nvg, int index, float mouseX, float mouseY) {
        float u = this.uiScale;
        float x = listX();
        float w = listW();
        float y = rowY(index);
        float h = ROW_HEIGHT * u;
        float radius = PhiMetrics.CORNER_RADIUS * u;

        PhiModule module = this.modules.get(index);
        float textX = rowContentLeft();
        float textW = w - (textX - x) - GEAR_SIZE * u;

        if (index == this.focus) {
            // The selected row: L0 background, name, description and the "more settings" square.
            nvg.drawSkewRect(x, y, w, h, radius, PhiTheme.level0());

            float nameSize = 13.0F * u;
            float descSize = 10.5F * u;

            nvg.drawText(nvg.limitText(module.getName(), nameSize, Fonts.REGULAR, textW),
                    textX, y + 6.0F * u, PhiTheme.textPrimary(), nameSize, Fonts.REGULAR);
            nvg.drawText(nvg.limitText(module.getDescription(), descSize, Fonts.REGULAR, textW),
                    textX, y + 23.0F * u, PhiTheme.textSecondary(), descSize, Fonts.REGULAR);

            if (module.hasSettings()) {
                drawGearButton(nvg, mouseX, mouseY);
            }

            return;
        }

        // An unselected row: hover reveal and the dim name.
        if (this.rowHover.target == index) {
            PhiRipple.drawReveal(nvg, x, y, w, h, radius, this.rowHover.enterX, this.rowHover.enterY,
                    this.rowHover.progress, PhiTheme.level4());
        }

        float nameSize = 12.5F * u;

        nvg.drawText(nvg.limitText(module.getName(), nameSize, Fonts.REGULAR,
                w - (textX - x) - ROW_PADDING * u), textX, y + (h - nameSize) * 0.5F,
                PhiTheme.textSecondary(), nameSize, Fonts.REGULAR);
    }

    /**
     * Draws the square "more settings" button on the right edge of the selected row: a plain
     * parallelogram (L5) marked with three dots - no gear icon.
     *
     * @param nvg    the NanoVG drawing context.
     * @param mouseX the pointer x.
     * @param mouseY the pointer y.
     */
    private void drawGearButton(NanoVGManager nvg, float mouseX, float mouseY) {
        float u = this.uiScale;
        float[] bounds = gearBounds();
        float x = bounds[0];
        float y = bounds[1];
        float size = GEAR_SIZE * u;
        float radius = size * 0.3F;

        boolean inside = PhiSkew.contains(mouseX, mouseY, x, y, size, size);
        this.gearHover.update(inside ? 0 : -1, mouseX, mouseY);

        int background = this.gearHover.target >= 0
                ? PhiTheme.mix(PhiTheme.level5(), PhiTheme.textPrimary(), 0.12F)
                : PhiTheme.level5();

        nvg.drawSkewRect(x, y, size, size, radius, background);

        float centerX = x + size * 0.5F;
        float centerY = y + size * 0.5F;
        float gap = size * 0.20F;
        float dotRadius = Math.max(1.0F, size * 0.06F);
        int dots = PhiTheme.textPrimary();

        nvg.drawCircle(centerX - gap, centerY, dotRadius, dots);
        nvg.drawCircle(centerX, centerY, dotRadius, dots);
        nvg.drawCircle(centerX + gap, centerY, dotRadius, dots);
    }

    /**
     * Draws the gradient masks that fade the upper and the lower edge of the dial list into the
     * background of the page.
     */
    private void drawListFades(NanoVGManager nvg) {
        float u = this.uiScale;
        float fade = FADE_HEIGHT * u;
        float x = listX();
        float w = listW();
        int background = PhiTheme.level2();
        int transparent = PhiTheme.withAlpha(background, 0.0F);

        // Upper edge: opaque background fading into transparency.
        nvg.beginPath();
        nvg.addRoundedRect(x, listTop(), w, fade, 0.0F);
        nvg.fillPathGradient(background, transparent, x, listTop(), x, listTop() + fade);

        // Lower edge: transparency fading into the opaque background.
        nvg.beginPath();
        nvg.addRoundedRect(x, listBottom() - fade, w, fade, 0.0F);
        nvg.fillPathGradient(transparent, background, x, listBottom() - fade, x, listBottom());
    }

    /**
     * Draws the detail panel: the large card with the icon of the selected module, the decorative
     * strip and the toggle switch in the lower right corner.
     */
    private void drawDetail(NanoVGManager nvg, float mouseX, float mouseY, float partialTicks) {
        float u = this.uiScale;
        float x = detailX();
        float w = detailW();
        float top = listTop();
        float radius = PhiMetrics.CORNER_RADIUS * u;

        PhiModule module = this.focus >= 0 && this.focus < this.modules.size()
                ? this.modules.get(this.focus) : null;

        // The large card with the centered module icon.
        float cardHeight = viewportHeight() * 0.55F;
        nvg.drawSkewRect(x, top, w, cardHeight, radius, PhiTheme.level3());

        if (module != null) {
            module.getIcon().draw(nvg, x + w * 0.5F, top + cardHeight * 0.5F,
                    DETAIL_ICON_SIZE * u, PhiTheme.textPrimary());
        }

        // The plain decorative strip.
        float stripTop = top + cardHeight + 14.0F * u;
        nvg.drawSkewRect(x, stripTop, w, DETAIL_STRIP_HEIGHT * u, DETAIL_STRIP_HEIGHT * u * 0.5F,
                PhiTheme.level3());

        // The toggle switch in the lower right corner.
        float toggleX = x + w - TOGGLE_WIDTH * u - ROW_PADDING * u;
        float toggleY = listBottom() - TOGGLE_HEIGHT * u - ROW_PADDING * u;

        this.detailToggle.setX(toggleX);
        this.detailToggle.setY(toggleY);
        this.detailToggle.setValue(module != null && module.isToggled());
        this.detailToggle.draw(nvg, (int) mouseX, (int) mouseY, partialTicks);
    }

    /**
     * Draws the settings panel: the header, the parameter grid and the open combo box layer.
     */
    private void drawSettingsPanel(NanoVGManager nvg, float mouseX, float mouseY, float partialTicks) {
        PhiModule module = this.modules.get(this.openModule);
        float u = this.uiScale;

        // The module name on the left.
        float titleSize = 15.0F * u;
        float titleMaxW = iconBounds(0)[0] - contentLeft() - 12.0F * u;

        nvg.drawText(nvg.limitText(module.getName(), titleSize, Fonts.REGULAR, titleMaxW),
                contentLeft(), this.y + 12.0F * u, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        // The square reset and back buttons.
        for (int i = 0; i < 2; i++) {
            float[] bounds = iconBounds(i);
            float x = bounds[0];
            float y = bounds[1];
            float size = ICON_BUTTON * u;

            boolean inside = PhiSkew.contains(mouseX, mouseY, x, y, size, size);
            this.iconHover.update(inside ? i : -1, mouseX, mouseY);

            int background = this.iconHover.target == i ? PhiTheme.level4() : PhiTheme.level3();
            nvg.drawSkewRect(x, y, size, size, size * 0.3F, background);

            PhiIcon icon = i == 0 ? PhiIcons.RESET : PhiIcons.PREVIOUS;
            icon.draw(nvg, x + size * 0.5F, y + size * 0.5F, size * 0.55F, PhiTheme.textPrimary());
        }

        // The two-column grid of the parameters.
        List<PhiModuleSetting> settings = module.getSettings();

        for (int i = 0; i < settings.size(); i++) {
            drawSetting(nvg, settings.get(i), i, mouseX, mouseY, partialTicks);
        }

        // The open combo box layer covers everything else.
        if (this.comboOpenIndex >= 0) {
            drawComboLayer(nvg, module, this.comboOpenIndex, mouseX, mouseY);
        }
    }

    /**
     * Draws one cell of the settings grid: the name of the parameter and its control.
     */
    private void drawSetting(NanoVGManager nvg, PhiModuleSetting setting, int index,
            float mouseX, float mouseY, float partialTicks) {
        float u = this.uiScale;
        float[] cell = settingCell(index);
        float x = cell[0];
        float width = cell[1];
        float top = cell[2];
        float rowH = SETTING_ROW_HEIGHT * u;
        float nameSize = 12.5F * u;

        float nameMaxW = width * 0.55F;
        nvg.drawText(nvg.limitText(setting.getName(), nameSize, Fonts.REGULAR, nameMaxW),
                x, top + 3.0F * u, PhiTheme.textSecondary(), nameSize, Fonts.REGULAR);

        switch (setting.getType()) {
            case TOGGLE: {
                PhiToggle toggle = this.settingToggles.get(index);

                toggle.setX(x + width - TOGGLE_WIDTH * u);
                toggle.setY(top + (rowH - TOGGLE_HEIGHT * u) * 0.5F);
                toggle.draw(nvg, (int) mouseX, (int) mouseY, partialTicks);

                nvg.drawText(toggle.isValue() ? "On" : "Off", x, top + 23.0F * u,
                        PhiTheme.textSecondary(), 10.5F * u, Fonts.REGULAR);
                break;
            }
            case SLIDER:
                drawSlider(nvg, setting, cell, index);
                break;
            case COMBO:
                drawCombo(nvg, setting, cell, index, mouseX, mouseY);
                break;
            default:
                break;
        }
    }

    /**
     * Draws a slider setting: the track, the filled portion, the handle and the current value.
     */
    private void drawSlider(NanoVGManager nvg, PhiModuleSetting setting, float[] cell, int index) {
        float u = this.uiScale;
        float[] track = sliderBounds(cell, setting);
        float x = track[0];
        float y = track[1];
        float w = track[2];
        float h = track[3];
        float ratio = sliderRatio(setting);

        nvg.drawRect(x, y, w, h, PhiTheme.level3());

        if (ratio > 0.0F) {
            nvg.drawRect(x, y, w * ratio, h, PhiTheme.level5());
        }

        float handleSize = HANDLE_SIZE * u;
        float handleX = x + w * ratio;
        int handle = this.draggingSlider == index ? PhiTheme.level4() : PhiTheme.level5();

        nvg.drawSkewRect(handleX - handleSize * 0.5F, y + h * 0.5F - handleSize * 0.5F,
                handleSize, handleSize, handleSize * 0.3F, handle);

        nvg.drawText(formatValue(setting.getFloat()), cell[0], cell[2] + 23.0F * u,
                PhiTheme.textSecondary(), 10.5F * u, Fonts.REGULAR);
    }

    /**
     * Draws a combo box setting: the button that shows the selected option.
     */
    private void drawCombo(NanoVGManager nvg, PhiModuleSetting setting, float[] cell, int index,
            float mouseX, float mouseY) {
        float u = this.uiScale;
        float[] bounds = comboBounds(cell);
        float x = bounds[0];
        float y = bounds[1];
        float w = bounds[2];
        float h = bounds[3];

        boolean inside = PhiSkew.contains(mouseX, mouseY, x, y, w, h);
        this.comboHover.update(inside ? index : -1, mouseX, mouseY);

        boolean open = this.comboOpenIndex == index;
        int background = inside || open ? PhiTheme.level4() : PhiTheme.level3();

        nvg.drawSkewRect(x, y, w, h, h * 0.3F, background);

        String option = setting.getOptions()[setting.getSelectedIndex()];
        float textSize = 10.5F * u;

        nvg.drawText(nvg.limitText(option, textSize, Fonts.REGULAR, w - 10.0F * u),
                x + 8.0F * u, y + (h - textSize) * 0.5F, PhiTheme.textPrimary(), textSize, Fonts.REGULAR);
    }

    /**
     * Draws the option layer of an open combo box on top of everything else.
     */
    private void drawComboLayer(NanoVGManager nvg, PhiModule module, int index, float mouseX, float mouseY) {
        PhiModuleSetting setting = module.getSettings().get(index);
        float[] layer = comboLayerBounds(index);

        if (layer == null) {
            return;
        }

        float u = this.uiScale;
        float x = layer[0];
        float y = layer[1];
        float w = layer[2];
        float h = layer[3];
        float optionH = COMBO_OPTION_HEIGHT * u;
        float textSize = 10.5F * u;
        float optionTop = y + ROW_PADDING * u * 0.5F;
        String[] options = setting.getOptions();

        nvg.drawSkewRect(x, y, w, h, h * 0.12F, PhiTheme.level1());

        for (int i = 0; i < options.length; i++) {
            float oy = optionTop + i * optionH;
            float inset = 4.0F * u;

            boolean hovered = PhiSkew.contains(mouseX, mouseY, x + inset, oy + 1.0F * u,
                    w - inset * 2.0F, optionH - 2.0F * u);
            this.optionHover.update(hovered ? index * 64 + i : -1, mouseX, mouseY);

            boolean selected = i == setting.getSelectedIndex();

            if (selected) {
                nvg.drawSkewRect(x + inset, oy + 1.0F * u, w - inset * 2.0F, optionH - 2.0F * u,
                        (optionH - 2.0F * u) * 0.3F, PhiTheme.level5());
            } else if (this.optionHover.target == index * 64 + i) {
                PhiRipple.drawReveal(nvg, x + inset, oy + 1.0F * u, w - inset * 2.0F, optionH - 2.0F * u,
                        (optionH - 2.0F * u) * 0.3F, this.optionHover.enterX, this.optionHover.enterY,
                        this.optionHover.progress, PhiTheme.level4());
            }

            int color = selected ? PhiTheme.textPrimary() : PhiTheme.textSecondary();
            nvg.drawText(nvg.limitText(options[i], textSize, Fonts.REGULAR, w - 16.0F * u),
                    x + 10.0F * u, oy + (optionH - textSize) * 0.5F, color, textSize, Fonts.REGULAR);
        }
    }

    /**
     * @param setting the slider setting.
     * @return the normalized position of the value between 0 and 1.
     */
    private static float sliderRatio(PhiModuleSetting setting) {
        float span = setting.getMax() - setting.getMin();

        if (span <= 0.0F) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, (setting.getFloat() - setting.getMin()) / span));
    }

    /**
     * Formats a slider value: whole numbers without decimals, everything else with one decimal.
     *
     * @param value the value to format.
     * @return the formatted value.
     */
    private static String formatValue(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;

        if (rounded == (int) rounded) {
            return String.valueOf((int) rounded);
        }

        return String.valueOf(rounded);
    }

    /**
     * Tracks the hover state of one kind of interactive element, following the same rules as
     * {@link PhiComponent}: the reveal grows for 120 ms when the pointer enters, shrinks for 420 ms
     * when it leaves, and starts at least at 12 percent.
     */
    private static final class HoverTracker {

        /** The index of the element under the pointer (-1 = none). */
        private int target = -1;

        /** The progress of the reveal (0 = none, 1 = full). */
        private float progress;

        /** The point the reveal spreads from. */
        private float enterX;
        private float enterY;

        private long time;

        /**
         * Advances the tracker towards the given target.
         *
         * @param newTarget the element under the pointer (-1 = none).
         * @param mouseX    the pointer x.
         * @param mouseY    the pointer y.
         */
        private void update(int newTarget, float mouseX, float mouseY) {
            long now = System.currentTimeMillis();
            float delta = this.time == 0L ? 16.0F : Math.min(100.0F, now - this.time);
            this.time = now;

            boolean inside = newTarget >= 0;

            if (inside && this.target != newTarget) {
                // The origin moves only while the reveal is still small, so a fast move does not reset it.
                if (this.progress < 0.2F || this.target < 0) {
                    this.enterX = mouseX;
                    this.enterY = mouseY;
                    this.progress = Math.max(this.progress, 0.12F);
                }
            }

            this.target = newTarget;

            float speed = inside ? delta / 120.0F : -delta / 420.0F;

            this.progress = Math.max(0.0F, Math.min(1.0F, this.progress + speed));
        }
    }
}
