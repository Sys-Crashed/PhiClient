package org.dpdns.syscrashed.phiclient.gui.tips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The tips that are shown in the tip card of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.tips}).
 *
 * <p>The first entry is the donation hint - the link is not available yet, so it only announces that
 * it is coming. The other entries are intentionally useless tips.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getTips()} - the list of tips.</li>
 * </ul>
 */
public final class PhiTips {

    private static final List<String> TIPS = new ArrayList<String>();

    static {
        TIPS.add("Support the development - the donation link is coming soon.");
        TIPS.add("Did you know? The whole interface leans to the right. That is not a bug, that is the design.");
        TIPS.add("The hub covers 35 percent of your screen, so you can still see the game behind it.");
        TIPS.add("The music player really plays the audio files of the PhiClient/music folder.");
        TIPS.add("If you are reading this, three seconds of your life are gone. Thank you for your support.");
        TIPS.add("Your skin looks better in the hub than in the inventory screen. Just saying.");
        TIPS.add("There are eight tips in this list. You will never see all of them. Probably.");
        TIPS.add("Loading screens are just the game asking you to look at it for a moment.");
    }

    private PhiTips() {
    }

    /**
     * @return the list of tips.
     */
    public static List<String> getTips() {
        return Collections.unmodifiableList(TIPS);
    }
}
