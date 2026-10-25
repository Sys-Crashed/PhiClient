package org.dpdns.syscrashed.phiclient.gui.changelog;

import org.dpdns.syscrashed.phiclient.gui.theme.PhiTheme;

/**
 * The type of a change log entry (package {@code org.dpdns.syscrashed.phiclient.gui.changelog}).
 *
 * <p>Every type has a badge color and the color of the text on that badge, so the badges stay
 * readable in both the dark and the light palette.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getLabel()} - the short text of the badge.</li>
 *   <li>{@link #getBadgeColor()} - the background color of the badge.</li>
 *   <li>{@link #getTextColor()} - the text color on the badge.</li>
 * </ul>
 */
public enum PhiChangeLogType {

    /** Something was added. */
    ADDED("Added"),

    /** Something was fixed. */
    FIXED("Fixed"),

    /** Something was improved. */
    IMPROVED("Improved"),

    /** Something was changed. */
    CHANGED("Changed"),

    /** Something was removed. */
    REMOVED("Removed");

    private final String label;

    PhiChangeLogType(String label) {
        this.label = label;
    }

    /**
     * @return the short text of the badge.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * @return the background color of the badge.
     */
    public int getBadgeColor() {
        switch (this) {
            case ADDED:
                return PhiTheme.accent();
            case FIXED:
                return PhiTheme.level5();
            case IMPROVED:
                return PhiTheme.level4();
            case CHANGED:
                return PhiTheme.level3();
            default:
                return PhiTheme.level2();
        }
    }

    /**
     * @return the color of the text on the badge.
     */
    public int getTextColor() {
        if (this == ADDED) {
            // The accent color is bright, so it needs a dark text.
            return 0xFF101418;
        }

        return PhiTheme.textPrimary();
    }
}
