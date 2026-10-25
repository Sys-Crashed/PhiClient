package org.dpdns.syscrashed.phiclient.gui.theme;

/**
 * Color palettes of the PhiClient user interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.theme}).
 *
 * <p>Two palettes are available: {@link #DARK} and {@link #LIGHT}. Both follow the design
 * specification and share the same rules:</p>
 * <ul>
 *   <li><b>No outlines.</b> Every boundary, focus, selection and hover state is expressed through a
 *       difference in the background level and / or in the text color - never through a stroke.</li>
 *   <li><b>One ladder per theme.</b> {@code L0 - L5} are six background levels. In the dark theme
 *       they get brighter from {@code L0} to {@code L5}, in the light theme they get darker - so a
 *       component always uses the same level for the same state in both themes.</li>
 * </ul>
 *
 * <p>Switching the theme is animated: the old palette is kept until the transition is over and every
 * color is interpolated between the two palettes. The window itself is not simply cross faded - it is
 * wiped diagonally from the lower left corner to the upper right corner (see
 * {@link #transition()} and {@link #isTransitioning()}).</p>
 *
 * <p>Provided members:</p>
 * <ul>
 *   <li>{@link #DARK} / {@link #LIGHT} - the two palettes.</li>
 *   <li>{@link #active()} / {@link #setActive(Palette)} / {@link #isLight()} - the active palette.</li>
 *   <li>{@link #previous()} - the palette that is left behind by a running transition.</li>
 *   <li>{@link #transition()} / {@link #isTransitioning()} - the state of the transition.</li>
 *   <li>{@link #level0()} ... {@link #level5()} - the six background levels of the active palette.</li>
 *   <li>{@link #accent()} / {@link #textPrimary()} / {@link #textSecondary()} - the accent and the
 *       text colors of the active palette.</li>
 *   <li>{@link #withAlpha(int, float)} - creates a transparent variant of a color.</li>
 *   <li>{@link #mix(int, int, float)} - interpolates between two colors.</li>
 * </ul>
 *
 * <p>All colors are packed ARGB ints (0xAARRGGBB).</p>
 */
public final class PhiTheme {

    /**
     * The dark palette (L0 - L5 from the darkest to the lightest level).
     *
     * <p>The accent color is temporarily pure white - the teal of the design specification will be
     * restored once the color scheme is finalized.</p>
     */
    public static final Palette DARK = new Palette(
            0xFF0E0F12, 0xFF16181D, 0xFF1D2027, 0xFF262A33, 0xFF313641, 0xFF3E4553,
            0xFFFFFFFF, 0xFFE8EAED, 0xFFA0A6B0);

    /** The light palette (L0 - L5 from the brightest to the darkest level). */
    public static final Palette LIGHT = new Palette(
            0xFFFFFFFF, 0xFFF7F8FA, 0xFFEEF0F4, 0xFFE2E5EA, 0xFFD4D8E0, 0xFFC3C8D4,
            0xFF0D9488, 0xFF1C1E23, 0xFF626875);

    /** The duration of a theme transition in milliseconds. */
    private static final long TRANSITION_MS = 300L;

    /** The palette that is currently used by the interface. */
    private static Palette active = LIGHT;

    /** The palette that was used before the running transition. */
    private static Palette previous = LIGHT;

    /** The moment the running transition started (0 when there is none). */
    private static long transitionStart;

    private PhiTheme() {
    }

    /**
     * @return the palette that is currently used by the interface.
     */
    public static Palette active() {
        return active;
    }

    /**
     * @return the palette that is left behind by a running transition.
     */
    public static Palette previous() {
        return previous;
    }

    /**
     * Switches the interface to another palette and starts the transition animation.
     *
     * @param palette the palette to use (null is ignored).
     */
    public static void setActive(Palette palette) {
        if (palette == null || palette == active) {
            return;
        }

        previous = active;
        active = palette;
        transitionStart = System.currentTimeMillis();
    }

    /**
     * @return true if the light palette is active.
     */
    public static boolean isLight() {
        return active == LIGHT;
    }

    /**
     * @return the progress of the theme transition (0 = the old palette, 1 = the new one).
     */
    public static float transition() {
        if (transitionStart == 0L) {
            return 1.0F;
        }

        float progress = (System.currentTimeMillis() - transitionStart) / (float) TRANSITION_MS;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    /**
     * @return true while the theme is changing.
     */
    public static boolean isTransitioning() {
        return transition() < 1.0F;
    }

    /**
     * @return the deepest background of the active palette.
     */
    public static int level0() {
        return blend(previous.level0, active.level0);
    }

    /**
     * @return the background of the hub window.
     */
    public static int level1() {
        return blend(previous.level1, active.level1);
    }

    /**
     * @return the background of the navigation band and of the content area.
     */
    public static int level2() {
        return blend(previous.level2, active.level2);
    }

    /**
     * @return the default background of cards and controls.
     */
    public static int level3() {
        return blend(previous.level3, active.level3);
    }

    /**
     * @return the background of a hovered element.
     */
    public static int level4() {
        return blend(previous.level4, active.level4);
    }

    /**
     * @return the background of a selected element.
     */
    public static int level5() {
        return blend(previous.level5, active.level5);
    }

    /**
     * @return the accent color of the active palette.
     */
    public static int accent() {
        return blend(previous.accent, active.accent);
    }

    /**
     * @return the primary text color of the active palette.
     */
    public static int textPrimary() {
        return blend(previous.textPrimary, active.textPrimary);
    }

    /**
     * @return the secondary text color of the active palette.
     */
    public static int textSecondary() {
        return blend(previous.textSecondary, active.textSecondary);
    }

    /**
     * Creates a transparent variant of a color by scaling its alpha channel.
     *
     * @param argb  the source color (0xAARRGGBB).
     * @param alpha the new alpha in the range {@code [0, 1]}.
     * @return the color with the scaled alpha channel.
     */
    public static int withAlpha(int argb, float alpha) {
        float clamped = Math.max(0.0F, Math.min(1.0F, alpha));
        int a = (int) Math.round(((argb >> 24) & 0xFF) * clamped);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Interpolates between two colors (including their alpha channels).
     *
     * @param from the color at {@code t = 0}.
     * @param to   the color at {@code t = 1}.
     * @param t    the interpolation factor, clamped into {@code [0, 1]}.
     * @return the interpolated color.
     */
    public static int mix(int from, int to, float t) {
        float f = Math.max(0.0F, Math.min(1.0F, t));
        int a = lerpChannel((from >> 24) & 0xFF, (to >> 24) & 0xFF, f);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, f);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, f);
        int b = lerpChannel(from & 0xFF, to & 0xFF, f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Interpolates between the two palettes with the progress of the running transition.
     *
     * @param oldColor the color of the previous palette.
     * @param newColor the color of the active palette.
     * @return the interpolated color.
     */
    private static int blend(int oldColor, int newColor) {
        if (!isTransitioning()) {
            return newColor;
        }

        return mix(oldColor, newColor, transition());
    }

    /**
     * Interpolates a single color channel.
     *
     * @param from the start value.
     * @param to   the end value.
     * @param t    the interpolation factor.
     * @return the interpolated channel value.
     */
    private static int lerpChannel(int from, int to, float t) {
        return (int) Math.round(from + (to - from) * t);
    }

    /**
     * An immutable set of colors - one complete palette of the interface.
     */
    public static final class Palette {

        /** The deepest / most prominent background level. */
        public final int level0;

        /** The background of the hub window. */
        public final int level1;

        /** The background of the navigation band and of the content area. */
        public final int level2;

        /** The default background of cards and controls. */
        public final int level3;

        /** The background of a hovered element. */
        public final int level4;

        /** The background of a selected element. */
        public final int level5;

        /** The accent color. */
        public final int accent;

        /** The primary text color. */
        public final int textPrimary;

        /** The secondary text color. */
        public final int textSecondary;

        /**
         * @param level0        the deepest background level.
         * @param level1        the hub window background.
         * @param level2        the navigation band background.
         * @param level3        the default control background.
         * @param level4        the hover background.
         * @param level5        the selected background.
         * @param accent        the accent color.
         * @param textPrimary   the primary text color.
         * @param textSecondary the secondary text color.
         */
        Palette(int level0, int level1, int level2, int level3, int level4, int level5,
                int accent, int textPrimary, int textSecondary) {
            this.level0 = level0;
            this.level1 = level1;
            this.level2 = level2;
            this.level3 = level3;
            this.level4 = level4;
            this.level5 = level5;
            this.accent = accent;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
        }
    }
}
