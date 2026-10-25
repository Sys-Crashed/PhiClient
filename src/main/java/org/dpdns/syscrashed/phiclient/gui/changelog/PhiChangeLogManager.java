package org.dpdns.syscrashed.phiclient.gui.changelog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The change log of the client (package {@code org.dpdns.syscrashed.phiclient.gui.changelog}).
 *
 * <p>The manager holds the entries of the current development state. They are shown in the change
 * log panel of the home page.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getChangeLogs()} - the entries, newest first.</li>
 *   <li>{@link #add(PhiChangeLogType, String)} - adds an entry.</li>
 * </ul>
 */
public final class PhiChangeLogManager {

    private static final PhiChangeLogManager INSTANCE = new PhiChangeLogManager();

    private final List<PhiChangeLog> changeLogs = new ArrayList<PhiChangeLog>();

    private PhiChangeLogManager() {
        this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.ADDED, "Central hub with a slanted navigation band"));
        this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.ADDED, "Home page with skin portrait and change log"));
        this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.ADDED, "Dark and light palette with a live switch"));
        this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.IMPROVED, "Hub follows the game window with a 35 percent area"));
        this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.FIXED, "Mouse clicks and the scroll wheel reach the interface"));

        // Placeholder entries: they make the list long enough to check the scrolling of the panel.
        for (int i = 1; i <= 100; i++) {
            this.changeLogs.add(new PhiChangeLog(PhiChangeLogType.ADDED,
                    "Placeholder entry " + i + " - a longer description that wraps over several lines "
                            + "so the scrolling of the change log can be checked properly"));
        }
    }

    /**
     * @return the shared instance of the manager.
     */
    public static PhiChangeLogManager getInstance() {
        return INSTANCE;
    }

    /**
     * @return the entries of the change log, newest first.
     */
    public List<PhiChangeLog> getChangeLogs() {
        return Collections.unmodifiableList(this.changeLogs);
    }

    /**
     * Adds an entry to the change log.
     *
     * @param type the type of the change.
     * @param text the description of the change.
     */
    public void add(PhiChangeLogType type, String text) {
        this.changeLogs.add(0, new PhiChangeLog(type, text));
    }
}
