package org.dpdns.syscrashed.phiclient.gui.component;

import java.util.ArrayList;
import java.util.List;

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
 * The modules page of the PhiHub: a full-width panel whose list holds one row per module (name,
 * description, toggle switch and - for modules with parameters - a gear icon). The gear opens the
 * "more settings" panel, which slides in from the right and holds the parameters of the module in a
 * two-column grid (toggle / slider / combo box, like the Soar client). The panel is left with the
 * back icon or a right click. The rows follow the slanted left edge of the panel and scroll along
 * the same diagonal.
 */
public class PhiModulePanel extends PhiComponent implements PhiScrollable {

    // ------------------------------------------------------------------ metrics (design pixels)

    /** Height of a module row. */
    private static final float ROW_HEIGHT = 56.0F;

    /** Vertical gap between two module rows. */
    private static final float ROW_GAP = 10.0F;

    /** Inset between the panel and its rows. */
    private static final float PANEL_PADDING = 18.0F;

    /** Inset between a row and its content. */
    private static final float ROW_PADDING = 12.0F;

    /** Edge length of the gear button. */
    private static final float GEAR_SIZE = 22.0F;

    /** Width / height of a toggle switch. */
    private static final float TOGGLE_WIDTH = 34.0F;
    private static final float TOGGLE_HEIGHT = 18.0F;

    /** Horizontal distance between the controls of a row. */
    private static final float CONTROL_GAP = 8.0F;

    /** Height of a row of the settings grid. */
    private static final float SETTING_ROW_HEIGHT = 42.0F;

    /** Horizontal gap between the two columns of the settings grid. */
    private static final float SETTING_COL_GAP = 28.0F;

    /** Edge length of the back / reset buttons of the settings panel. */
    private static final float ICON_BUTTON = 26.0F;

    /** Height of a combo box and of one of its options. */
    private static final float COMBO_HEIGHT = 22.0F;
    private static final float COMBO_OPTION_HEIGHT = 24.0F;

    /** Height of a slider track. */
    private static final float SLIDER_HEIGHT = 6.0F;

    /** Edge length of the handle of a slider. */
    private static final float HANDLE_SIZE = 14.0F;

    /** Scroll distance of a single wheel notch. */
    private static final float SCROLL_PER_NOTCH = 30.0F;

    /** Duration of the slide between the list and the settings panel. */
    private static final int TRANSITION_MS = 320;

    // ------------------------------------------------------------------ state

    private final List<PhiModule> modules;

    private final float uiScale;

    /** The toggle switch of every module row (one per module). */
    private final List<PhiToggle> rowToggles = new ArrayList<PhiToggle>();

    /** The toggle switches of the settings panel (rebuilt when a module is opened). */
    private final List<PhiToggle> settingToggles = new ArrayList<PhiToggle>();

    private final PhiScrollState listScroll = new PhiScrollState();

    private final PhiScrollState settingsScroll = new PhiScrollState();

    /** The slide between the list and the settings panel (0 = list, 1 = settings). */
    private final EaseOutCubic transition = new EaseOutCubic(TRANSITION_MS, 1.0D);

    /** The index of the module whose settings are open (-1 = list view). */
    private int openModule = -1;

    /** The index of the combo box that shows its options (-1 = none). */
    private int comboOpenIndex = -1;

    /** The index of the setting whose slider is being dragged (-1 = none). */
    private int draggingSlider = -1;

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

        for (final PhiModule module : modules) {
            PhiToggle toggle = new PhiToggle(0.0F, 0.0F, TOGGLE_WIDTH * uiScale, TOGGLE_HEIGHT * uiScale,
                    module.isToggled());
            toggle.setOnToggled(new PhiToggle.Listener() {
                @Override
                public void onToggled(boolean value) {
                    module.setToggled(value);
                }
            });

            this.rowToggles.add(toggle);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.openModule < 0) {
            clickedList(mouseX, mouseY, mouseButton);
            return;
        }

        if (this.transition.getValueFloat() < 1.0F) {
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

        float delta = notches * SCROLL_PER_NOTCH * this.uiScale;

        if (this.openModule >= 0) {
            this.settingsScroll.scrollBy(delta);
        } else {
            this.listScroll.scrollBy(delta);
        }

        return true;
    }

    /**
     * Handles a click while the list view is shown.
     */
    private void clickedList(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        float cursor = listTop() - this.listScroll.getOffset();

        for (int i = 0; i < this.modules.size(); i++) {
            float[] row = rowBounds(i, cursor);

            cursor = row[0] + row[3] + ROW_GAP * this.uiScale;

            if (!PhiSkew.contains(mouseX, mouseY, row[0], row[1], row[2], row[3])) {
                continue;
            }

            // The gear opens the settings panel; it takes precedence over the row body.
            if (row[4] >= 0.0F && PhiSkew.contains(mouseX, mouseY, row[4], row[5],
                    GEAR_SIZE * this.uiScale, GEAR_SIZE * this.uiScale)) {
                openSettings(i);
                return;
            }

            // The row body toggles the module, the same way the Soar client does it.
            this.modules.get(i).setToggled(!this.modules.get(i).isToggled());
            this.rowToggles.get(i).setValue(this.modules.get(i).isToggled());
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

                if (mouseX >= layer[0] && mouseX <= layer[0] + layer[2]
                        && mouseY >= layer[1] && mouseY <= layer[1] + layer[3]) {
                    return;
                }
            }

            // A click outside of the layer closes it - or opens another combo box.
            this.comboOpenIndex = -1;
        }

        float cursor = listTop() - this.settingsScroll.getOffset();

        // The back and reset buttons of the header.
        float[] back = iconBounds(1, cursor);
        if (PhiSkew.contains(mouseX, mouseY, back[0], back[1], ICON_BUTTON * this.uiScale,
                ICON_BUTTON * this.uiScale)) {
            closeSettings();
            return;
        }

        float[] reset = iconBounds(0, cursor);
        if (PhiSkew.contains(mouseX, mouseY, reset[0], reset[1], ICON_BUTTON * this.uiScale,
                ICON_BUTTON * this.uiScale)) {
            module.reset();
            syncSettingToggles(module);
            return;
        }

        List<PhiModuleSetting> settings = module.getSettings();

        for (int i = 0; i < settings.size(); i++) {
            PhiModuleSetting setting = settings.get(i);

            float[] row = settingRow(i, cursor);

            switch (setting.getType()) {
                case TOGGLE:
                    if (i < this.settingToggles.size()) {
                        this.settingToggles.get(i).mouseClicked(mouseX, mouseY, mouseButton);
                    }
                    break;
                case SLIDER:
                    float[] slider = sliderBounds(row, setting);

                    if (mouseX >= slider[0] - HANDLE_SIZE * this.uiScale && mouseX <= slider[0] + slider[2]
                            + HANDLE_SIZE * this.uiScale && mouseY >= slider[1] - HANDLE_SIZE * this.uiScale
                            && mouseY <= slider[1] + slider[3] + HANDLE_SIZE * this.uiScale) {
                        this.draggingSlider = i;
                        updateSlider(setting, slider, mouseX);
                    }
                    break;
                case COMBO:
                    float[] combo = comboBounds(row);

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
        this.comboOpenIndex = -1;
        this.draggingSlider = -1;
        this.settingsScroll.reset();
        syncSettingToggles(this.modules.get(index));

        this.transition.reset();
        this.transition.setDirection(Direction.FORWARDS);
    }

    /**
     * Leaves the settings panel and slides back to the list.
     */
    private void closeSettings() {
        if (this.openModule < 0) {
            return;
        }

        this.comboOpenIndex = -1;
        this.draggingSlider = -1;
        this.transition.setDirection(Direction.BACKWARDS);
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
        float ratio = (mouseX - slider[0]) / slider[2];

        setting.setFloat(setting.getMin() + (setting.getMax() - setting.getMin()) * ratio);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void draw(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float deltaMs = Math.min(100.0F, now - this.lastFrame);
        this.lastFrame = now;

        this.listScroll.update(deltaMs);
        this.settingsScroll.update(deltaMs);

        if (this.openModule >= 0 && this.transition.isDone(Direction.BACKWARDS)) {
            this.openModule = -1;
            this.comboOpenIndex = -1;
            this.draggingSlider = -1;
        }

        float radius = PhiMetrics.CORNER_RADIUS * this.uiScale;

        nvg.drawSkewRect(this.x, this.y, this.width, this.height, radius, PhiTheme.level3());

        float t = this.openModule >= 0 ? this.transition.getValueFloat() : 0.0F;
        float shift = this.width * 0.6F;

        if (t < 1.0F) {
            drawListView(nvg, mouseX, mouseY, -t * shift, partialTicks);
        }

        if (t > 0.0F && this.openModule >= 0) {
            drawSettingsView(nvg, mouseX, mouseY, (1.0F - t) * shift, partialTicks);
        }
    }

    /**
     * Draws the list of the modules, translated by {@code shiftX} (used for the slide transition).
     */
    private void drawListView(NanoVGManager nvg, int mouseX, int mouseY, float shiftX, float partialTicks) {
        float u = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * u;

        float contentHeight = this.modules.size() * ROW_HEIGHT * u + (this.modules.size() - 1) * ROW_GAP * u;
        this.listScroll.setContentSize(contentHeight + PANEL_PADDING * u, listHeight());

        float cursor = listTop() - this.listScroll.getOffset();

        // The hover of the rows is probed before drawing, so the reveal is drawn with its data.
        boolean active = this.openModule < 0 && this.comboOpenIndex < 0;
        int hoverRow = -1;
        int hoverGear = -1;

        if (active) {
            float probe = cursor;

            for (int i = 0; i < this.modules.size(); i++) {
                float[] row = rowBounds(i, probe);

                probe = row[0] + row[3] + ROW_GAP * u;

                if (PhiSkew.contains(mouseX, mouseY, row[0] + shiftX, row[1], row[2], row[3])) {
                    hoverRow = i;

                    if (row[4] >= 0.0F && PhiSkew.contains(mouseX, mouseY, row[4] + shiftX, row[5],
                            GEAR_SIZE * u, GEAR_SIZE * u)) {
                        hoverGear = i;
                    }
                }
            }
        }

        this.rowHover.update(active ? hoverRow : -1, mouseX, mouseY);
        this.gearHover.update(active ? hoverGear : -1, mouseX, mouseY);

        nvg.save();
        nvg.translate(shiftX, 0.0F);

        drawHeader(nvg, mouseX, mouseY, "Modules", this.modules.size() + " mods", false);

        // The list is clipped to the slanted panel.
        nvg.save();
        float shapeOffset = PhiSkew.offset(this.height);
        nvg.translate(this.x + shapeOffset, this.y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);
        nvg.scissor(0.0F, listTop() - this.y, this.width, listHeight());
        nvg.skewX(-PhiSkew.SHEAR_DEGREES);
        nvg.translate(-(this.x + shapeOffset), -this.y);

        for (int i = 0; i < this.modules.size(); i++) {
            float[] row = rowBounds(i, cursor);

            cursor = row[0] + row[3] + ROW_GAP * u;

            PhiModule module = this.modules.get(i);

            nvg.drawSkewRect(row[0], row[1], row[2], row[3], radius, PhiTheme.level4());

            if (this.rowHover.target == i) {
                PhiRipple.drawReveal(nvg, row[0], row[1], row[2], row[3], radius,
                        this.rowHover.enterX - shiftX, this.rowHover.enterY, this.rowHover.progress, PhiTheme.level5());
            }

            // Name and description share one content rectangle.
            float cL = PhiSkew.contentLeft(row[0], row[3], ROW_PADDING * u);
            float titleSize = PhiMetrics.FONT_TITLE * u;
            float bodySize = PhiMetrics.FONT_BODY * u;

            float controlsLeft = row[4] >= 0.0F ? row[4] : row[6];
            controlsLeft -= CONTROL_GAP * u;

            float textHeight = titleSize * 1.25F + bodySize * 1.25F;
            float nameY = row[1] + (row[3] - textHeight) * 0.5F;
            float descY = nameY + titleSize * 1.25F;

            String name = nvg.limitText(module.getName(), titleSize, Fonts.REGULAR, controlsLeft - cL);
            String description = nvg.limitText(module.getDescription(), bodySize, Fonts.REGULAR, controlsLeft - cL);

            nvg.drawText(name, cL, nameY, PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);
            nvg.drawText(description, cL, descY, PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

            // The gear - the "more settings" entry of the modules with parameters.
            if (row[4] >= 0.0F) {
                nvg.drawSkewRect(row[4], row[5], GEAR_SIZE * u, GEAR_SIZE * u, radius * 0.6F, PhiTheme.level3());

                if (this.gearHover.target == i) {
                    PhiRipple.drawReveal(nvg, row[4], row[5], GEAR_SIZE * u, GEAR_SIZE * u, radius * 0.6F,
                            this.gearHover.enterX - shiftX, this.gearHover.enterY, this.gearHover.progress,
                            PhiTheme.level5());
                }

                float centerX = row[4] + (GEAR_SIZE * u + PhiSkew.offset(GEAR_SIZE * u)) * 0.5F;
                float centerY = row[5] + GEAR_SIZE * u * 0.5F;

                PhiIcons.MORE_SETTINGS.draw(nvg, centerX, centerY, GEAR_SIZE * u * 0.78F,
                        this.gearHover.target == i ? PhiTheme.accent() : PhiTheme.textSecondary());
            }

            // The toggle switch of the row.
            PhiToggle toggle = this.rowToggles.get(i);
            toggle.setX(row[6]);
            toggle.setY(row[7]);
            toggle.draw(nvg, mouseX - (int) shiftX, mouseY, partialTicks);
        }

        nvg.restore();
        nvg.restore();
    }

    /**
     * Draws the header line of a view: the title on the left, the counter or the icon buttons on the
     * right, and a separator below.
     */
    private void drawHeader(NanoVGManager nvg, int mouseX, int mouseY, String title, String counter,
                            boolean settings) {
        float u = this.uiScale;
        float[] line = headerLineBounds();

        float cL = line[0] + ROW_PADDING * u;
        float cR = line[0] + line[1] - ROW_PADDING * u;

        float titleSize = PhiMetrics.FONT_TITLE * u;

        nvg.drawText(nvg.limitText(title, titleSize, Fonts.REGULAR, cR - cL), cL, this.y + 10.0F * u,
                PhiTheme.textPrimary(), titleSize, Fonts.REGULAR);

        if (!settings) {
            float bodySize = PhiMetrics.FONT_BODY * u;

            nvg.drawText(counter, cR - nvg.textWidth(counter, bodySize, Fonts.REGULAR), this.y + 13.0F * u,
                    PhiTheme.textSecondary(), bodySize, Fonts.REGULAR);

            nvg.drawLine(cL, listTop() - 4.0F * u, cR, listTop() - 4.0F * u, PhiTheme.level4(), 1.0F * u);
        }

        if (settings) {
            // The reset and back buttons of the settings panel.
            float cursor = listTop() - this.settingsScroll.getOffset();
            int hoverIcon = -1;

            if (this.openModule >= 0 && this.transition.getValueFloat() >= 1.0F) {
                for (int i = 0; i < 2; i++) {
                    float[] icon = iconBounds(i, cursor);

                    if (PhiSkew.contains(mouseX, mouseY, icon[0], icon[1], ICON_BUTTON * u, ICON_BUTTON * u)) {
                        hoverIcon = i;
                    }
                }
            }

            this.iconHover.update(hoverIcon, mouseX, mouseY);

            float radius = PhiMetrics.CORNER_RADIUS * u;

            for (int i = 0; i < 2; i++) {
                float[] icon = iconBounds(i, cursor);

                nvg.drawSkewRect(icon[0], icon[1], ICON_BUTTON * u, ICON_BUTTON * u, radius * 0.6F,
                        PhiTheme.level3());

                if (this.iconHover.target == i) {
                    PhiRipple.drawReveal(nvg, icon[0], icon[1], ICON_BUTTON * u, ICON_BUTTON * u, radius * 0.6F,
                            this.iconHover.enterX, this.iconHover.enterY, this.iconHover.progress, PhiTheme.level5());
                }

                float centerX = icon[0] + (ICON_BUTTON * u + PhiSkew.offset(ICON_BUTTON * u)) * 0.5F;
                float centerY = icon[1] + ICON_BUTTON * u * 0.5F;
                int color = this.iconHover.target == i ? PhiTheme.accent() : PhiTheme.textSecondary();

                (i == 1 ? PhiIcons.PREVIOUS : PhiIcons.RESET).draw(nvg, centerX, centerY, ICON_BUTTON * u * 0.6F,
                        color);
            }
        }
    }

    /**
     * Draws the settings panel of the open module, translated by {@code shiftX}.
     */
    private void drawSettingsView(NanoVGManager nvg, int mouseX, int mouseY, float shiftX, float partialTicks) {
        if (this.openModule < 0) {
            return;
        }

        float u = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * u;

        PhiModule module = this.modules.get(this.openModule);
        List<PhiModuleSetting> settings = module.getSettings();

        int rows = (settings.size() + 1) / 2;
        this.settingsScroll.setContentSize(rows * SETTING_ROW_HEIGHT * u, listHeight());

        float cursor = listTop() - this.settingsScroll.getOffset();

        // The sliders take the pointer while they are dragged.
        for (int i = 0; i < settings.size(); i++) {
            if (i == this.draggingSlider && settings.get(i).getType() == PhiModuleSetting.Type.SLIDER) {
                updateSlider(settings.get(i), sliderBounds(settingRow(i, cursor), settings.get(i)), mouseX);
            }
        }

        int hoverCombo = -1;

        if (this.comboOpenIndex < 0 && this.transition.getValueFloat() >= 1.0F) {
            for (int i = 0; i < settings.size(); i++) {
                if (settings.get(i).getType() != PhiModuleSetting.Type.COMBO) {
                    continue;
                }

                float[] combo = comboBounds(settingRow(i, cursor));

                if (PhiSkew.contains(mouseX, mouseY, combo[0] + shiftX, combo[1], combo[2], combo[3])) {
                    hoverCombo = i;
                }
            }
        }

        this.comboHover.update(hoverCombo, mouseX, mouseY);

        nvg.save();
        nvg.translate(shiftX, 0.0F);

        drawHeader(nvg, mouseX, mouseY, module.getName(), null, true);

        nvg.save();
        float shapeOffset = PhiSkew.offset(this.height);
        nvg.translate(this.x + shapeOffset, this.y);
        nvg.skewX(PhiSkew.SHEAR_DEGREES);
        nvg.scissor(0.0F, listTop() - this.y, this.width, listHeight());
        nvg.skewX(-PhiSkew.SHEAR_DEGREES);
        nvg.translate(-(this.x + shapeOffset), -this.y);

        float bodySize = PhiMetrics.FONT_BODY * u;

        for (int i = 0; i < settings.size(); i++) {
            PhiModuleSetting setting = settings.get(i);
            float[] row = settingRow(i, cursor);

            switch (setting.getType()) {
                case TOGGLE: {
                    String name = nvg.limitText(setting.getName(), bodySize, Fonts.REGULAR, row[1] * 0.55F);

                    nvg.drawText(name, row[0], row[2] + (SETTING_ROW_HEIGHT * u - bodySize) * 0.5F,
                            PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

                    PhiToggle toggle = i < this.settingToggles.size() ? this.settingToggles.get(i) : null;

                    if (toggle != null) {
                        toggle.setX(row[0] + row[1] - TOGGLE_WIDTH * u);
                        toggle.setY(row[2] + (SETTING_ROW_HEIGHT * u - TOGGLE_HEIGHT * u) * 0.5F);
                        toggle.draw(nvg, mouseX - (int) shiftX, mouseY, partialTicks);
                    }
                    break;
                }
                case SLIDER: {
                    String name = nvg.limitText(setting.getName(), bodySize, Fonts.REGULAR, row[1] * 0.55F);

                    nvg.drawText(name, row[0], row[2] + (SETTING_ROW_HEIGHT * u - bodySize) * 0.5F,
                            PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

                    drawSlider(nvg, setting, sliderBounds(row, setting));
                    break;
                }
                case COMBO: {
                    float[] combo = comboBounds(row);

                    nvg.drawSkewRect(combo[0], combo[1], combo[2], combo[3], radius * 0.5F, PhiTheme.level4());

                    if (this.comboHover.target == i || this.comboOpenIndex == i) {
                        PhiRipple.drawReveal(nvg, combo[0], combo[1], combo[2], combo[3], radius * 0.5F,
                                this.comboHover.enterX - shiftX, this.comboHover.enterY, this.comboHover.progress,
                                PhiTheme.level5());
                    }

                    String value = setting.getOptions()[setting.getSelectedIndex()];
                    String shown = nvg.limitText(value, bodySize, Fonts.REGULAR, combo[2] - ROW_PADDING * u);

                    nvg.drawTextCentered(shown,
                            combo[0] + (combo[2] + PhiSkew.offset(combo[3])) * 0.5F,
                            combo[1] + (combo[3] - bodySize) * 0.5F, PhiTheme.textPrimary(), bodySize, Fonts.REGULAR);

                    break;
                }
                default:
                    break;
            }
        }

        nvg.restore();
        nvg.restore();

        // The open combo box draws its options on top of everything, outside of the scissor.
        drawComboLayer(nvg, mouseX, mouseY, shiftX);
    }

    /**
     * Draws a slider: the track, the filled part and the handle, all slanted.
     */
    private void drawSlider(NanoVGManager nvg, PhiModuleSetting setting, float[] slider) {
        float u = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * u;
        float ratio = (setting.getFloat() - setting.getMin()) / (setting.getMax() - setting.getMin());

        nvg.drawSkewRect(slider[0], slider[1], slider[2], slider[3], radius * 0.4F, PhiTheme.level4());
        nvg.drawSkewRect(slider[0], slider[1], slider[2] * ratio, slider[3], radius * 0.4F, PhiTheme.accent());

        float handleX = slider[0] + (slider[2] - HANDLE_SIZE * u) * ratio;
        float handleY = slider[1] - (HANDLE_SIZE * u - slider[3]) * 0.5F;

        nvg.drawSkewRect(handleX, handleY, HANDLE_SIZE * u, HANDLE_SIZE * u, radius * 0.5F,
                PhiTheme.withAlpha(PhiTheme.accent(), 0.85F));
    }

    /**
     * Draws the open option layer of a combo box, if any.
     */
    private void drawComboLayer(NanoVGManager nvg, int mouseX, int mouseY, float shiftX) {
        if (this.comboOpenIndex < 0 || this.openModule < 0) {
            return;
        }

        PhiModule module = this.modules.get(this.openModule);

        if (this.comboOpenIndex >= module.getSettings().size()) {
            return;
        }

        PhiModuleSetting setting = module.getSettings().get(this.comboOpenIndex);

        if (setting.getType() != PhiModuleSetting.Type.COMBO) {
            return;
        }

        float u = this.uiScale;
        float radius = PhiMetrics.CORNER_RADIUS * u;
        float[] layer = comboLayerBounds(this.comboOpenIndex);

        if (layer == null) {
            return;
        }

        int hoverOption = optionAt(layer, mouseX, mouseY);

        this.optionHover.update(hoverOption, mouseX, mouseY);

        nvg.save();
        nvg.translate(shiftX, 0.0F);

        nvg.drawSkewRect(layer[0], layer[1], layer[2], layer[3], radius * 0.7F, PhiTheme.level4());

        float bodySize = PhiMetrics.FONT_BODY * u;
        float optY = layer[1] + ROW_PADDING * u * 0.5F;

        for (int i = 0; i < setting.getOptions().length; i++) {
            if (this.optionHover.target == i) {
                PhiRipple.drawReveal(nvg, layer[0], optY - ROW_PADDING * u * 0.5F, layer[2],
                        COMBO_OPTION_HEIGHT * u, radius * 0.5F, this.optionHover.enterX - shiftX,
                        this.optionHover.enterY, this.optionHover.progress, PhiTheme.level5());
            }

            String option = nvg.limitText(setting.getOptions()[i], bodySize, Fonts.REGULAR,
                    layer[2] - ROW_PADDING * u);

            nvg.drawText(option, layer[0] + ROW_PADDING * u * 0.5F, optY + (COMBO_OPTION_HEIGHT * u - bodySize) * 0.5F,
                    i == setting.getSelectedIndex() ? PhiTheme.accent() : PhiTheme.textSecondary(), bodySize,
                    Fonts.REGULAR);

            optY += COMBO_OPTION_HEIGHT * u;
        }

        nvg.restore();
    }

    // ------------------------------------------------------------------ geometry

    /**
     * @return the top y of the scrollable area (below the header).
     */
    private float listTop() {
        return this.y + 48.0F * this.uiScale;
    }

    /**
     * @return the height of the scrollable area.
     */
    private float listHeight() {
        return this.height - 48.0F * this.uiScale - PANEL_PADDING * this.uiScale;
    }

    /**
     * @return the content line of the header: its left x on the slanted edge and its width
     *         ({@code {x, width}}).
     */
    private float[] headerLineBounds() {
        float rowX = PhiSkew.leftEdgeAt(this.x, this.y, this.height, listTop()) + PANEL_PADDING * this.uiScale;
        float rowW = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height, listTop())
                - PANEL_PADDING * this.uiScale - rowX;

        return new float[] { rowX, rowW };
    }

    /**
     * @param index  the index of the module.
     * @param cursor the top y of the row in list layout space.
     * @return the bounds of the row: {@code {x, y, width, height, gearX, gearY, toggleX, toggleY}}.
     *         {@code gearX} is negative when the module has no settings.
     */
    private float[] rowBounds(int index, float cursor) {
        float u = this.uiScale;
        float rowH = ROW_HEIGHT * u;
        float rowX = PhiSkew.leftEdgeAt(this.x, this.y, this.height, cursor) + PANEL_PADDING * u;
        float rowW = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height, cursor)
                - PANEL_PADDING * u - rowX;
        float cR = PhiSkew.contentRight(rowX, rowW, ROW_PADDING * u);

        float gearX = -1.0F;
        float gearY = -1.0F;
        float toggleX;

        if (this.modules.get(index).hasSettings()) {
            // The right x is cleared of the slant, so the top-right corner of the gear stays inside.
            gearX = cR - GEAR_SIZE * u - PhiSkew.offset(GEAR_SIZE * u);
            gearY = cursor + (rowH - GEAR_SIZE * u) * 0.5F;
            toggleX = gearX - PhiSkew.offset(TOGGLE_HEIGHT * u) - CONTROL_GAP * u - TOGGLE_WIDTH * u;
        } else {
            toggleX = cR - PhiSkew.offset(TOGGLE_HEIGHT * u) - TOGGLE_WIDTH * u;
        }

        float toggleY = cursor + (rowH - TOGGLE_HEIGHT * u) * 0.5F;

        return new float[] { rowX, cursor, rowW, rowH, gearX, gearY, toggleX, toggleY };
    }

    /**
     * @param index  0 = reset, 1 = back.
     * @param cursor the top y of the header line.
     * @return the bounds of the icon button ({@code {x, y}}).
     */
    private float[] iconBounds(int index, float cursor) {
        float u = this.uiScale;
        float[] line = headerLineBounds();
        float cR = line[0] + line[1] - ROW_PADDING * u;

        float backX = cR - ICON_BUTTON * u - PhiSkew.offset(ICON_BUTTON * u);
        float iconY = listTop() - 42.0F * u;
        float resetX = backX - PhiSkew.offset(ICON_BUTTON * u) - CONTROL_GAP * u - ICON_BUTTON * u;

        return index == 1 ? new float[] { backX, iconY } : new float[] { resetX, iconY };
    }

    /**
     * @param index  the index of the setting.
     * @param cursor the top y of the grid in layout space.
     * @return the bounds of the cell of the setting: {@code {columnX, columnWidth, rowTop}}.
     */
    private float[] settingRow(int index, float cursor) {
        float u = this.uiScale;
        int col = index % 2;
        int rowIdx = index / 2;

        float rowTop = cursor + rowIdx * SETTING_ROW_HEIGHT * u;
        float rowX = PhiSkew.leftEdgeAt(this.x, this.y, this.height, rowTop) + PANEL_PADDING * u;
        float rowW = PhiSkew.rightEdgeAt(this.x, this.width, this.y, this.height, rowTop)
                - PANEL_PADDING * u - rowX;
        float colW = (rowW - ROW_PADDING * u * 2.0F - SETTING_COL_GAP * u) * 0.5F;
        float colX = rowX + ROW_PADDING * u + col * (colW + SETTING_COL_GAP * u);

        return new float[] { colX, colW, rowTop };
    }

    /**
     * @param row     the cell of the setting.
     * @param setting the slider setting.
     * @return the bounds of the track ({@code {x, y, width, height}}).
     */
    private float[] sliderBounds(float[] row, PhiModuleSetting setting) {
        float u = this.uiScale;
        float trackW = row[1] * 0.45F;
        float trackX = row[0] + row[1] - trackW;
        float trackY = row[2] + (SETTING_ROW_HEIGHT * u - SLIDER_HEIGHT * u) * 0.5F;

        return new float[] { trackX, trackY, trackW, SLIDER_HEIGHT * u };
    }

    /**
     * @param row the cell of the setting.
     * @return the bounds of the combo box button ({@code {x, y, width, height}}).
     */
    private float[] comboBounds(float[] row) {
        float u = this.uiScale;
        float w = row[1] * 0.5F;
        float h = COMBO_HEIGHT * u;
        float x = row[0] + row[1] - w;
        float y = row[2] + (SETTING_ROW_HEIGHT * u - h) * 0.5F;

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
        float cursor = listTop() - this.settingsScroll.getOffset();
        float[] combo = comboBounds(settingRow(index, cursor));

        float w = combo[2] + ROW_PADDING * u * 2.0F;
        float h = setting.getOptions().length * COMBO_OPTION_HEIGHT * u + ROW_PADDING * u;
        float x = combo[0] - ROW_PADDING * u;
        float y = combo[1] + combo[3] + 4.0F * u;

        if (y + h > this.y + this.height - PANEL_PADDING * u) {
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

        if (mouseX < layer[0] || mouseX > layer[0] + layer[2] || mouseY < layer[1] || mouseY > layer[1] + layer[3]) {
            return -1;
        }

        int index = (int) ((mouseY - layer[1] - ROW_PADDING * u * 0.5F) / (COMBO_OPTION_HEIGHT * u));

        return index;
    }

    // ------------------------------------------------------------------ hover tracking

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
