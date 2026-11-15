package org.dpdns.syscrashed.phiclient.gui.tips;

import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * The tips that are shown in the tip card of the home page
 * (package {@code org.dpdns.syscrashed.phiclient.gui.tips}).
 *
 * <p>The tips are stored as i18n keys ({@code tips.1} ... {@code tips.8}); {@link #getTips()}
 * resolves them in the current language per call, so a language switch is reflected without any
 * rebuild. The first entry is the donation hint - the link is not available yet, so it only
 * announces that it is coming. The other entries are intentionally useless tips.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getTips()} - the list of tips in the current language.</li>
 * </ul>
 */
public final class PhiTips {

    /** The number of tips (the {@code .lang} files must provide exactly this many keys). */
    private static final int TIP_COUNT = 8;

    private PhiTips() {
    }

    /**
     * Resolves the tips in the current language. The lookup happens per call (eight O(1) map
     * lookups), so the returned list always reflects the active language.
     *
     * @return the list of tips in the current language.
     */
    public static List<String> getTips() {
        List<String> tips = new ArrayList<String>(TIP_COUNT);
        for (int i = 1; i <= TIP_COUNT; i++) {
            tips.add(PhiI18n.get("tips." + i));
        }
        return tips;
    }
}
