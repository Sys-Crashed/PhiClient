package org.dpdns.syscrashed.phiclient.gui.changelog;

import org.dpdns.syscrashed.phiclient.i18n.PhiI18n;

/**
 * A single entry of the change log (package {@code org.dpdns.syscrashed.phiclient.gui.changelog}).
 *
 * <p>The entry stores an i18n key (e.g. {@code changelog.entry.1}), not the display text itself:
 * {@link #getText()} resolves the key lazily in the current language. This keeps the manager's
 * static initialization free of any i18n dependency (the tables may not be loaded yet when the
 * singleton is created).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getType()} - the type of the change.</li>
 *   <li>{@link #getText()} - the localized description of the change.</li>
 * </ul>
 */
public class PhiChangeLog {

    private final PhiChangeLogType type;

    /** The i18n key of the description (resolved lazily by {@link #getText()}). */
    private final String textKey;

    /**
     * @param type    the type of the change.
     * @param textKey the i18n key of the description.
     */
    public PhiChangeLog(PhiChangeLogType type, String textKey) {
        this.type = type;
        this.textKey = textKey;
    }

    /**
     * @return the type of the change.
     */
    public PhiChangeLogType getType() {
        return this.type;
    }

    /**
     * Resolves the description in the current language. The lookup happens per call, so already
     * stored entries follow a language switch without being rebuilt.
     *
     * @return the localized description of the change.
     */
    public String getText() {
        return PhiI18n.get(this.textKey);
    }
}
