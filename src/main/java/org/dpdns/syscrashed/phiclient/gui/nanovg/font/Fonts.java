package org.dpdns.syscrashed.phiclient.gui.nanovg.font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Font registry for the UI (package {@code org.dpdns.syscrashed.phiclient.gui.nanovg.font}).
 *
 * <p>Holds the fonts used by PhiClient's NanoVG-based interface. Only PhiClient's own font is used
 * for now: "Source Han Sans &amp; Saira Hybrid" (a blend of Source Han Sans and Saira).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getFonts()} - returns a fresh list of all registered fonts (currently just {@link #REGULAR}).</li>
 * </ul>
 */
public final class Fonts {

    /** The single regular-weight UI font, loaded from the bundled TTF on the classpath. */
    public static final Font REGULAR = new Font("regular", "/assets/phiclient/fonts/SourceHanSansSaira-Regular.ttf");

    private Fonts() {
    }

    /**
     * @return a new list containing every registered font (so callers can iterate without mutating the registry).
     */
    public static List<Font> getFonts() {
        return new ArrayList<Font>(Arrays.asList(REGULAR));
    }
}
