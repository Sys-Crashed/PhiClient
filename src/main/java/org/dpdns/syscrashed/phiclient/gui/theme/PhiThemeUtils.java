package org.dpdns.syscrashed.phiclient.gui.theme;

/**
 * Helpers that are shared by the palettes of the interface
 * (package {@code org.dpdns.syscrashed.phiclient.gui.theme}).
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #scrim()} - the 60 percent overlay that keeps texts readable on a custom background.
 *       It is dark in the dark theme and light in the light theme.</li>
 * </ul>
 */
public final class PhiThemeUtils {

    private PhiThemeUtils() {
    }

    /**
     * @return the 60 percent overlay color of the active palette.
     */
    public static int scrim() {
        return PhiTheme.isLight() ? PhiTheme.withAlpha(0xFFFFFFFF, 0.6F) : PhiTheme.withAlpha(0xFF000000, 0.6F);
    }
}
