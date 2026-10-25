package org.dpdns.syscrashed.phiclient.gui.changelog;

/**
 * A single entry of the change log (package {@code org.dpdns.syscrashed.phiclient.gui.changelog}).
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getType()} - the type of the change.</li>
 *   <li>{@link #getText()} - the description of the change.</li>
 * </ul>
 */
public class PhiChangeLog {

    private final PhiChangeLogType type;

    private final String text;

    /**
     * @param type the type of the change.
     * @param text the description of the change.
     */
    public PhiChangeLog(PhiChangeLogType type, String text) {
        this.type = type;
        this.text = text;
    }

    /**
     * @return the type of the change.
     */
    public PhiChangeLogType getType() {
        return this.type;
    }

    /**
     * @return the description of the change.
     */
    public String getText() {
        return this.text;
    }
}
