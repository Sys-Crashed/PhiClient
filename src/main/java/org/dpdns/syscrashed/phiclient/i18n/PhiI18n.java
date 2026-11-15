package org.dpdns.syscrashed.phiclient.i18n;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.dpdns.syscrashed.phiclient.utils.ResourceUtils;

/**
 * The central internationalization lookup (package {@code org.dpdns.syscrashed.phiclient.i18n}).
 *
 * <p>All three bundled {@code .lang} files are loaded once into memory by {@link #init()} (one
 * immutable {@code Map} per language); switching the language only swaps the reference of
 * {@link #current}, no further I/O happens. Lookup is O(1) per draw call and argument-free calls
 * are zero-allocation. The fallback chain is {@code current language -> en_US -> the key itself}
 * (with a de-duplicated warning), so the UI can never go blank or crash on a missing key - unlike
 * SoarClient, which returns the string {@code "null"}.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #init()} - loads the tables, validates them and reads the persisted language.</li>
 *   <li>{@link #get(String)} / {@link #tr(String, Object...)} - resolve a key in the current language.</li>
 *   <li>{@link #current()} / {@link #available()} - the active and the supported languages.</li>
 *   <li>{@link #setLanguage(PhiLanguage)} - switch language, persist it and notify listeners.</li>
 *   <li>{@link #addListener(LanguageListener)} - register a language-change listener.</li>
 * </ul>
 */
public final class PhiI18n {

    /**
     * Receives a notification after the language changed. Listeners must not throw; exceptions are
     * caught and logged by {@link #setLanguage(PhiLanguage)}.
     */
    public interface LanguageListener {
        void onLanguageChanged(PhiLanguage language);
    }

    private static final String WARN_CATEGORY = "PhiI18n";

    private static final EnumMap<PhiLanguage, Map<String, String>> TABLES =
            new EnumMap<PhiLanguage, Map<String, String>>(PhiLanguage.class);

    private static final List<LanguageListener> LISTENERS = new ArrayList<LanguageListener>();

    /** Keys that already produced a missing-key warning, so the log is not spammed every frame. */
    private static final Set<String> WARNED_KEYS = new HashSet<String>();

    private static PhiLanguage current = PhiLanguage.EN_US;

    private static boolean initialized;

    private PhiI18n() {
    }

    /**
     * Loads the three {@code .lang} resources into memory, validates that their key sets agree and
     * restores the persisted language. Called once from {@code PhiClient.start()} before the GUI
     * subsystem initializes, so every later draw call can resolve keys.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        for (PhiLanguage language : PhiLanguage.values()) {
            String content = ResourceUtils.readUtf8Text(language.getResourcePath(), PhiI18n.class);
            TABLES.put(language, parse(content, language.getId()));
        }

        validate();

        // The stored language is restored only after all tables are loaded, so the fallback chain
        // is complete from the very first lookup.
        current = PhiLanguageConfig.load();

        PhiLogger.info("PhiI18n initialized: " + PhiLanguage.EN_US + " has "
                + TABLES.get(PhiLanguage.EN_US).size() + " keys, language = " + current.getId());
    }

    /**
     * Resolves a key in the current language, falling back to {@code en_US} and finally to the key
     * itself (with a de-duplicated warning).
     *
     * @param key the translation key (must not be {@code null}).
     * @return the translated text; never {@code null}.
     */
    public static String get(String key) {
        if (!initialized) {
            // Defensive: if something resolves before init, fall back to en_US so the UI still works.
            String fallback = TABLES.get(PhiLanguage.EN_US).get(key);
            if (fallback == null) {
                warnMissing(key);
                return key;
            }
            return fallback;
        }
        Map<String, String> table = TABLES.get(current);
        String value = table != null ? table.get(key) : null;
        if (value != null) {
            return value;
        }
        Map<String, String> english = TABLES.get(PhiLanguage.EN_US);
        value = english != null ? english.get(key) : null;
        if (value != null) {
            return value;
        }
        warnMissing(key);
        return key;
    }

    /**
     * Resolves a key in the current language and formats it with {@link String#format}. When no
     * arguments are given this behaves exactly like {@link #get(String)} (zero allocation).
     *
     * @param key  the translation key.
     * @param args the format arguments for the {@code %s}/{@code %d} placeholders.
     * @return the translated and formatted text.
     */
    public static String tr(String key, Object... args) {
        if (args == null || args.length == 0) {
            return get(key);
        }
        try {
            return String.format(get(key), args);
        } catch (Exception e) {
            PhiLogger.error(WARN_CATEGORY + " - failed to format key '" + key + "'", e);
            return get(key);
        }
    }

    /**
     * @return the currently active language.
     */
    public static PhiLanguage current() {
        return current;
    }

    /**
     * @return all supported languages, in declaration order (en_US, zh_CN, ja_JP).
     */
    public static PhiLanguage[] available() {
        return PhiLanguage.values();
    }

    /**
     * Switches the active language, persists the choice and notifies the registered listeners.
     * The language change is reflected by the next frame: {@code GuiPhiHub} listens and forces a
     * full re-layout, which rebuilds every component and thereby refreshes all text caches.
     *
     * @param language the new language; {@code null} falls back to {@link PhiLanguage#EN_US}.
     */
    public static void setLanguage(PhiLanguage language) {
        PhiLanguage next = language != null ? language : PhiLanguage.EN_US;
        if (next == current) {
            return;
        }
        current = next;
        PhiLanguageConfig.save(current);
        notifyListeners(current);
        PhiLogger.info("PhiI18n language switched to " + current.getId());
    }

    /**
     * Registers a language-change listener. Registrations are permanent for the session.
     *
     * @param listener the listener, ignored if {@code null}.
     */
    public static void addListener(LanguageListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     * Verifies that the key sets of {@code zh_CN} and {@code ja_JP} agree with the {@code en_US}
     * baseline (which is the source of truth). Missing keys degrade to the English text; extra keys
     * are reported so stale translations are cleaned up. Never throws.
     */
    public static void validate() {
        Map<String, String> baseline = TABLES.get(PhiLanguage.EN_US);
        if (baseline == null || baseline.isEmpty()) {
            PhiLogger.error("PhiI18n validation skipped: en_US table is empty.");
            return;
        }
        for (PhiLanguage language : PhiLanguage.values()) {
            if (language == PhiLanguage.EN_US) {
                continue;
            }
            Map<String, String> table = TABLES.get(language);
            if (table == null) {
                PhiLogger.error("PhiI18n validation: " + language.getId() + " table missing.");
                continue;
            }
            Set<String> missing = new HashSet<String>(baseline.keySet());
            missing.removeAll(table.keySet());
            Set<String> extra = new HashSet<String>(table.keySet());
            extra.removeAll(baseline.keySet());
            if (!missing.isEmpty()) {
                PhiLogger.error("PhiI18n validation: " + language.getId() + " misses "
                        + missing.size() + " keys: " + abbreviate(missing));
            }
            if (!extra.isEmpty()) {
                PhiLogger.warn(WARN_CATEGORY + " validation: " + language.getId() + " has "
                        + extra.size() + " extra keys: " + abbreviate(extra));
            }
        }
    }

    /**
     * Parses a {@code .lang} file body into a key-value map. Lines are {@code key=value} pairs;
     * {@code #} starts a comment and blank lines are skipped. Values may contain {@code \n} escapes
     * and the first {@code =} splits key and value, so values may contain further {@code =} signs.
     * Duplicate keys keep the first occurrence and warn.
     *
     * @param content   the file body, may be {@code null} (yields an empty map).
     * @param resource  the resource name for log messages.
     * @return the parsed map, never {@code null}.
     */
    private static Map<String, String> parse(String content, String resource) {
        Map<String, String> result = new HashMap<String, String>();
        if (content == null) {
            return result;
        }
        String[] lines = content.split("\\r?\\n");
        int lineNumber = 0;
        for (String raw : lines) {
            lineNumber++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                PhiLogger.warn(WARN_CATEGORY + " - malformed line in " + resource + ":"
                        + lineNumber + ": '" + line + "'");
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = unescape(line.substring(separator + 1));
            if (result.containsKey(key)) {
                PhiLogger.warn(WARN_CATEGORY + " - duplicate key '" + key + "' in " + resource
                        + ":" + lineNumber + ", keeping the first value");
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Reverts the {@code \n} escapes stored in {@code .lang} files back to real newlines.
     *
     * @param value the raw value.
     * @return the value with escapes resolved.
     */
    private static String unescape(String value) {
        return value.replace("\\n", "\n");
    }

    /**
     * Logs a missing-key warning once per key, so per-frame lookups of an unknown key do not spam
     * the log.
     *
     * @param key the missing key.
     */
    private static void warnMissing(String key) {
        synchronized (WARNED_KEYS) {
            if (WARNED_KEYS.add(key)) {
                PhiLogger.warn(WARN_CATEGORY + " - missing translation for key '" + key
                        + "', falling back to en_US or the key itself");
            }
        }
    }

    /**
     * Notifies all listeners, isolating failures so a broken listener cannot abort the switch.
     *
     * @param language the new language.
     */
    private static void notifyListeners(PhiLanguage language) {
        for (LanguageListener listener : new ArrayList<LanguageListener>(LISTENERS)) {
            try {
                listener.onLanguageChanged(language);
            } catch (Exception e) {
                PhiLogger.error(WARN_CATEGORY + " - language listener failed", e);
            }
        }
    }

    /**
     * Formats a set of keys for a log line, truncating long lists.
     *
     * @param keys the keys to format.
     * @return a comma-separated excerpt.
     */
    private static String abbreviate(Set<String> keys) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String key : keys) {
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(key);
            count++;
            if (count >= 8) {
                builder.append(", ...");
                break;
            }
        }
        return builder.toString();
    }
}
