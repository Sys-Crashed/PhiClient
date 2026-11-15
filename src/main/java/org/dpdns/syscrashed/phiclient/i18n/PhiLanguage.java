package org.dpdns.syscrashed.phiclient.i18n;

/**
 * The languages supported by PhiClient (package {@code org.dpdns.syscrashed.phiclient.i18n}).
 *
 * <p>Each constant carries its stable id (used for persistence and as the {@code .lang} file name),
 * its display name (rendered in the settings selector, written in the language itself) and the
 * classpath of the bundled {@code .lang} resource. {@link #fromId(String)} is lenient: an unknown
 * id falls back to {@link #EN_US} so a corrupt config can never break startup.</p>
 */
public enum PhiLanguage {

    EN_US("en_US", "English", "/assets/phiclient/lang/en_US.lang"),
    ZH_CN("zh_CN", "简体中文", "/assets/phiclient/lang/zh_CN.lang"),
    JA_JP("ja_JP", "日本語", "/assets/phiclient/lang/ja_JP.lang");

    private final String id;
    private final String displayName;
    private final String resourcePath;

    PhiLanguage(String id, String displayName, String resourcePath) {
        this.id = id;
        this.displayName = displayName;
        this.resourcePath = resourcePath;
    }

    /**
     * @return the stable language id (e.g. {@code "en_US"}), used for persistence and resource naming.
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the display name in the language itself (e.g. {@code "简体中文"}).
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * @return the classpath of the bundled {@code .lang} resource, starting with {@code /}.
     */
    public String getResourcePath() {
        return this.resourcePath;
    }

    /**
     * Resolves a language from its id, ignoring case.
     *
     * @param id the persisted id, may be {@code null} or unknown.
     * @return the matching language, or {@link #EN_US} when the id is not recognized.
     */
    public static PhiLanguage fromId(String id) {
        if (id != null) {
            for (PhiLanguage language : values()) {
                if (language.id.equalsIgnoreCase(id)) {
                    return language;
                }
            }
        }
        return EN_US;
    }
}
