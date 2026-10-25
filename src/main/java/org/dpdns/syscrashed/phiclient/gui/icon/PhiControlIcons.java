package org.dpdns.syscrashed.phiclient.gui.icon;

/**
 * The icons of the control blocks of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.icon}).
 *
 * <p>Every icon is an SVG resource in {@code /assets/phiclient/images}. The files are rasterized once
 * and tinted with the color of the state, so they follow the theme of the interface.</p>
 *
 * <p>Provided members:</p>
 * <ul>
 *   <li>{@link #SIGNAL} / {@link #CROSSHAIR} - the icons of the two copy blocks.</li>
 *   <li>{@link #CHECK} - the icon that confirms a successful copy.</li>
 *   <li>{@link #SUN} / {@link #MOON} - the icons of the theme block.</li>
 *   <li>{@link #CLOCK} / {@link #WEATHER} / {@link #GLOBE} - the icons of the three value blocks.</li>
 * </ul>
 */
public final class PhiControlIcons {

    /** The icon of the copy block of the address (the strength of the signal). */
    public static final PhiIcon SIGNAL = new PhiSvgIcon("/assets/phiclient/images/signal.svg");

    /** The icon of the copy block of the position (a crosshair). */
    public static final PhiIcon CROSSHAIR = new PhiSvgIcon("/assets/phiclient/images/crosshair.svg");

    /** The icon that confirms a successful copy. */
    public static final PhiIcon CHECK = new PhiSvgIcon("/assets/phiclient/images/check.svg");

    /** The icon of the theme block in the light theme. */
    public static final PhiIcon SUN = new PhiSvgIcon("/assets/phiclient/images/sun.svg");

    /** The icon of the theme block in the dark theme. */
    public static final PhiIcon MOON = new PhiSvgIcon("/assets/phiclient/images/moon.svg");

    /** The icon of the block that shows the time of the game world. */
    public static final PhiIcon CLOCK = new PhiSvgIcon("/assets/phiclient/images/clock.svg");

    /** The icon of the block that shows the weather of the game world. */
    public static final PhiIcon WEATHER = new PhiSvgIcon("/assets/phiclient/images/weather.svg");

    /** The icon of the block that shows the dimension of the game world. */
    public static final PhiIcon GLOBE = new PhiSvgIcon("/assets/phiclient/images/globe.svg");

    private PhiControlIcons() {
    }
}
