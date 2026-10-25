package org.dpdns.syscrashed.phiclient.compat.optifine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Runtime compatibility layer for OptiFine (1.8.9 HD U M5)
 * (package {@code org.dpdns.syscrashed.phiclient.compat.optifine}).
 *
 * <p>Why everything is reflection-based:</p>
 * <ul>
 *   <li>the dev environment runs SRG (MCP) named Minecraft, while OptiFine's {@code patch/*.xdelta}
 *       are binary delta patches against <strong>obfuscated (notch) class names + original byte MD5</strong>,
 *       so OptiFine cannot take effect (and should not be a compile-time dependency) in dev;</li>
 *   <li>only the release environment (the real obfuscated 1.8.9 on the player's machine) has those
 *       classes and the patches.</li>
 * </ul>
 *
 * <p>Therefore every interaction with OptiFine here must be "reflect on demand + fail silently / degrade".</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #detectOptiFine()} - early existence check via resources (no class loading).</li>
 *   <li>{@link #hook(LaunchClassLoader)} - registers OptiFine's class transformer onto LaunchWrapper, before Mixin init.</li>
 *   <li>{@link #isOptiFineDetected()} - cached result of {@link #detectOptiFine()}.</li>
 *   <li>{@link #isOptiFineOnClasspath()} - live re-check of OptiFine presence (needed under Forge).</li>
 *   <li>{@link #isLoaded()} / {@link #getVersion()} - runtime availability and version string.</li>
 *   <li>{@link #isFastRenderEnabled()} / {@link #isShadersEnabled()} - feature flags.</li>
 *   <li>{@link #setFastRenderEnabled(boolean)} - toggles OptiFine fast render (custom 2D rendering usually needs it off).</li>
 * </ul>
 */
public final class OptiFineCompat {

    /** OptiFine's class transformer; the only thing {@code optifine.OptiFineTweaker} does is register it. */
    private static final String CLASS_TRANSFORMER = "optifine.OptiFineClassTransformer";
    /** OptiFine's main config class, in the default package (e.g. {@code Config.getVersion()} / {@code isFastRender()}). */
    private static final String CLASS_CONFIG = "Config";
    /** The "fast render" switch field injected into GameSettings (this name survives obfuscation). */
    private static final String FIELD_FAST_RENDER = "ofFastRender";
    /** The transformer class's resource path (used for probing; checking the resource never triggers class loading). */
    static final String RESOURCE_TRANSFORMER = "optifine/OptiFineClassTransformer.class";
    /**
     * The prefix handed to {@code LaunchClassLoader#addClassLoaderExclusion}.
     *
     * <p>This matches exactly what launchwrapper computes when handling
     * {@code --tweakClass optifine.OptiFineTweaker} (the package name, no trailing dot). Do <b>not</b> write
     * {@code net.optifine}: classes under that package still must be loaded by LaunchClassLoader, otherwise
     * future {@code @Pseudo} mixins would fail.</p>
     */
    private static final String EXCLUSION_OPTIFINE = "optifine";

    private static boolean hookChecked;
    private static boolean hooked;
    private static boolean detected;

    private OptiFineCompat() {
    }

    /**
     * Early existence probe (decides whether OptiFine is ready, in the tweaker's earliest callback).
     *
     * <p>The probe point is the resource {@code optifine/OptiFineClassTransformer.class}: it is always present
     * in OptiFine's jar, is independent of whether the game has started, and so is safe to check in the
     * tweaker's earliest callback.</p>
     *
     * @return true if OptiFine was found on the classpath.
     */
    public static boolean detectOptiFine() {
        detected = findOptiFineResource() != null;
        return detected;
    }

    /**
     * Probes by resource only - never calls {@code Class.forName}.
     *
     * <p>Reason: once OptiFine's class is loaded by LaunchClassLoader <em>before</em>
     * {@code addClassLoaderExclusion}, it is cached as "defined by LaunchClassLoader", and excluding it
     * afterwards is too late - the transformer then cannot obtain a {@code file:} CodeSource.</p>
     *
     * @return the resource URL, or {@code null} if not found.
     */
    private static java.net.URL findOptiFineResource() {
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            if (system != null) {
                java.net.URL url = system.getResource(RESOURCE_TRANSFORMER);
                if (url != null) {
                    return url;
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }

        try {
            return OptiFineCompat.class.getClassLoader().getResource(RESOURCE_TRANSFORMER);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Attempts to register OptiFine's class transformer onto the LaunchWrapper transform chain.
     *
     * <p>Must be called by {@code PhiTweaker#injectIntoClassLoader} <strong>before</strong> Mixin is
     * initialized: OptiFine's delta patches target the <em>unmodified</em> vanilla bytes (with MD5
     * verification), so applying Mixin first would mis-match; the correct order is OptiFine patches, then
     * Mixin weaves.</p>
     *
     * @param classLoader the LaunchClassLoader to register the transformer on.
     * @return true if OptiFine is usable (registered now, or already registered by another tweaker).
     */
    public static boolean hook(LaunchClassLoader classLoader) {
        if (hookChecked) {
            return hooked;
        }
        hookChecked = true;

        try {
            // 1) [Critical step] Make optifine.* load from the parent loader (system classpath) instead.
            //    This is exactly what launchwrapper does for --tweakClass optifine.OptiFineTweaker:
            //    Launch.java:97 -> classLoader.addClassLoaderExclusion("optifine").
            //    Without this, optifine.* is defined by LaunchClassLoader itself, its CodeSource becomes a
            //    jar-entry URL and OptiFine immediately fails with "Can not find the OptiFine JAR".
            if (OptiFineProvisioner.isVisibleToSystemClassLoader()) {
                classLoader.addClassLoaderExclusion(EXCLUSION_OPTIFINE);
            } else {
                PhiLogger.warn("OptiFine is not visible to the parent class loader; "
                        + "it may fail to locate its own jar.");
            }

            Class<?> transformerClass = Class.forName(CLASS_TRANSFORMER, false, classLoader);

            // OptiFineClassTransformer writes its own instance into the static field `instance` on construction.
            // If another entry (e.g. an extra --tweakClass optifine.OptiFineTweaker) already registered it,
            // do not register again, or the same class would be patched twice.
            Field instanceField = transformerClass.getField("instance");
            if (instanceField.get(null) != null) {
                hooked = true;
                return hooked;
            }

            classLoader.registerTransformer(CLASS_TRANSFORMER);
            hooked = instanceField.get(null) != null;
            if (!hooked) {
                PhiLogger.warn("OptiFine transformer was registered but did not initialize "
                        + "(it could not locate its own jar).");
            }
        } catch (ClassNotFoundException ignored) {
            // No OptiFine on the classpath (the dev environment is exactly this case) - expected.
            hooked = false;
        } catch (Throwable throwable) {
            hooked = false;
            PhiLogger.error("Failed to hook OptiFine.", throwable);
        }

        return hooked;
    }

    /**
     * The cached result of {@link #detectOptiFine()}: whether OptiFine exists on the classpath.
     *
     * <p>Used to avoid double-loading: when the user already installed OptiFine with its installer, we
     * should not extract and mount our bundled copy.</p>
     *
     * @return the cached detection result.
     */
    public static boolean isOptiFineDetected() {
        return detected;
    }

    /**
     * Live re-check of whether OptiFine is on the classpath.
     *
     * <p>Do not trust only {@link #isOptiFineDetected()}'s cached result: under Forge, FML adds the mods'
     * jars to the class loader during {@code injectIntoClassLoader}, which is <em>later</em> than our
     * earlier {@code acceptOptions} check - that check would wrongly cache "no OptiFine".</p>
     *
     * @return true if OptiFine can currently be found.
     */
    public static boolean isOptiFineOnClasspath() {
        return findOptiFineResource() != null;
    }

    /**
     * @return true if OptiFine is actually usable.
     */
    public static boolean isLoaded() {
        return hooked || detected || findConfigClass() != null;
    }

    /**
     * @return the OptiFine version string, e.g. {@code OptiFine_1.8.9_HD_U_M5}; null if unavailable.
     */
    public static String getVersion() {
        Class<?> config = findConfigClass();
        if (config == null) {
            return null;
        }

        Object version = invokeStatic(config, "getVersion");
        return version == null ? null : String.valueOf(version);
    }

    /**
     * @return whether OptiFine's "fast render" (multi-threaded rendering, conflicts with most custom 2D rendering) is on.
     */
    public static boolean isFastRenderEnabled() {
        return readBooleanFlag("isFastRender");
    }

    /**
     * @return whether OptiFine shaders are currently active.
     */
    public static boolean isShadersEnabled() {
        return readBooleanFlag("isShaders");
    }

    /**
     * Enables / disables OptiFine's "fast render".
     *
     * <p>Custom rendering (e.g. NanoVG / self-drawn HUD) generally needs this off, otherwise the render
     * state is corrupted by the extra thread. Note OptiFine re-reads the config every time the video
     * settings screen is opened, so this may need to be re-applied at the right moment.</p>
     *
     * @param enabled the desired fast-render state.
     * @return true if the flag was set successfully.
     */
    public static boolean setFastRenderEnabled(boolean enabled) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.gameSettings == null) {
                return false;
            }

            Field field = findField(minecraft.gameSettings.getClass(), FIELD_FAST_RENDER);
            if (field == null || field.getType() != boolean.class) {
                return false;
            }

            field.setAccessible(true);
            field.setBoolean(minecraft.gameSettings, enabled);
            return true;
        } catch (Throwable throwable) {
            PhiLogger.error("Failed to set OptiFine fast render.", throwable);
            return false;
        }
    }

    /**
     * Reads a boolean flag by invoking a no-arg static method on the OptiFine config class.
     *
     * @param methodName the method name (e.g. {@code isFastRender}).
     * @return the boolean value, or false if OptiFine is unavailable / the call fails.
     */
    private static boolean readBooleanFlag(String methodName) {
        Class<?> config = findConfigClass();
        if (config == null) {
            return false;
        }

        Object value = invokeStatic(config, methodName);
        return value instanceof Boolean && (Boolean) value;
    }

    /**
     * Resolves the OptiFine config class (default package), or {@code null} if not present.
     */
    private static Class<?> findConfigClass() {
        try {
            return Class.forName(CLASS_CONFIG);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Invokes a no-arg static method and returns its result (or {@code null} on failure).
     */
    private static Object invokeStatic(Class<?> owner, String methodName) {
        try {
            Method method = owner.getMethod(methodName);
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Finds a field by name, walking up the superclass chain.
     */
    private static Field findField(Class<?> owner, String name) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
