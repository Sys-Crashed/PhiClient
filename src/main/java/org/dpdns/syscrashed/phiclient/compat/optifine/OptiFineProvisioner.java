package org.dpdns.syscrashed.phiclient.compat.optifine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Extracts the OptiFine jar bundled inside the PhiClient jar and mounts it as a <strong>jar file</strong>
 * onto the class loaders, so the user does not have to install OptiFine manually
 * (package {@code org.dpdns.syscrashed.phiclient.compat.optifine}).
 *
 * <p>Why it must be "a jar file visible to the parent loader" rather than "extracted to a directory" or
 * "only mounted on LaunchClassLoader":</p>
 *
 * <p>OptiFine locates its own jar via {@code getProtectionDomain().getCodeSource().getLocation().toURI()}.
 * But {@code LaunchClassLoader#findClass} builds its CodeSource with
 * {@code new CodeSource(findResource(name).getURL(), ...)} - i.e. a <strong>jar-entry URL</strong>
 * ({@code jar:file:/.../OptiFine.jar!/optifine/X.class}). Then {@code new File(uri)} throws
 * {@code IllegalArgumentException: URI is not hierarchical}, after which OptiFine prints
 * "*** Can not find the OptiFine JAR in the classpath ***" and dies. This was verified: standard
 * launchwrapper-1.11 and OptiFine's own launchwrapper-of-2.2 behave identically.</p>
 *
 * <p>The official build works because {@code Launch} excludes the tweak's package from LaunchClassLoader
 * (launchwrapper 1.11 {@code Launch.java:97}):
 * <pre>
 * // Ensure we allow the tweak class to load with the parent classloader
 * classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
 * </pre>
 * With {@code --tweakClass optifine.OptiFineTweaker} the excluded prefix is {@code optifine}, so all
 * {@code optifine.*} load from the <strong>parent (system classpath)</strong> loader - and a normal
 * {@code URLClassLoader} yields a {@code file:/.../OptiFine.jar} CodeSource, which resolves correctly.
 * This class makes OptiFine's jar appear where the parent loader can see it; {@link OptiFineCompat#hook}
 * adds the {@code addClassLoaderExclusion} part.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #provision(LaunchClassLoader)} - extracts the bundled OptiFine jar and mounts it; returns the jar (or null).</li>
 *   <li>{@link #isVisibleToSystemClassLoader()} - whether OptiFine is already on the system classpath.</li>
 * </ul>
 */
public final class OptiFineProvisioner {

    private static final String RESOURCE_DIR = "/assets/phiclient/optifine/";
    private static final String RESOURCE_NAME = "OptiFine_1.8.9_HD_U_M5.jar";
    private static final String CACHE_NAME = "OptiFine_1.8.9_HD_U_M5.jar";

    private OptiFineProvisioner() {
    }

    /**
     * Provisions OptiFine: extracts the bundled OptiFine jar into the user's cache directory and mounts it.
     *
     * @param classLoader the LaunchClassLoader to also receive the jar (for {@code net.optifine.*}).
     * @return a usable OptiFine jar file, or {@code null} when no provisioning is needed / possible.
     */
    public static File provision(LaunchClassLoader classLoader) {
        if (isDeobfuscatedEnvironment()) {
            // The dev workspace runs SRG-named MC; OptiFine's patches match obfuscated names and never apply, so skip.
            return null;
        }

        if (isVisibleToSystemClassLoader()) {
            // Already on the system classpath (our launcher puts OptiFine into libraries/), so the parent
            // loader can see it directly - no need to extract our bundled copy.
            return null;
        }

        InputStream stream = OptiFineProvisioner.class.getResourceAsStream(RESOURCE_DIR + RESOURCE_NAME);
        if (stream == null) {
            // No bundled OptiFine: leave it to an external classpath or the user's environment (backward compatible).
            return null;
        }

        File target = new File(new File(System.getProperty("user.home"), ".phiclient/optifine"), CACHE_NAME);
        try {
            if (!target.isFile()) {
                File parent = target.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                copy(stream, target);
                PhiLogger.info("Extracted embedded OptiFine to " + target.getAbsolutePath());
            }

            // Parent loader visible -> optifine.* loads from it -> CodeSource is a file: jar path.
            addToSystemClassPath(target);
            // LaunchClassLoader also needs it, otherwise net.optifine.* (408 classes) cannot be found.
            addToClassLoader(classLoader, target);
            return target;
        } catch (Throwable throwable) {
            PhiLogger.error("Failed to provision OptiFine.", throwable);
        } finally {
            closeQuietly(stream);
        }

        return null;
    }

    /**
     * Detects whether we are running in a deobfuscated (SRG) environment, e.g. ForgeGradle's
     * {@code runClient}.
     *
     * <p>Basis: in an obfuscated environment the Minecraft class resource path is {@code /ave.class},
     * while in SRG it is {@code /net/minecraft/client/Minecraft.class}.</p>
     *
     * @return true if the environment is deobfuscated.
     */
    private static boolean isDeobfuscatedEnvironment() {
        try {
            URL self = net.minecraft.client.Minecraft.class.getResource("Minecraft.class");
            return self != null && self.toString().contains("net/minecraft/client/Minecraft.class");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * @return whether OptiFine is already visible to the parent (system classpath) loader.
     */
    static boolean isVisibleToSystemClassLoader() {
        try {
            return ClassLoader.getSystemClassLoader().getResource(OptiFineCompat.RESOURCE_TRANSFORMER) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** On Java 8 the system class loader is a {@code URLClassLoader}, so we can reflectively append a URL. */
    private static void addToSystemClassPath(File jar) throws Exception {
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (!(system instanceof URLClassLoader)) {
            return;
        }

        java.lang.reflect.Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(system, jar.toURI().toURL());
    }

    private static void addToClassLoader(LaunchClassLoader classLoader, File jar) throws Exception {
        URL url = jar.toURI().toURL();
        java.lang.reflect.Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(classLoader, url);
    }

    private static void copy(InputStream stream, File target) throws Exception {
        OutputStream out = new FileOutputStream(target);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (Throwable ignored) {
            // ignore
        }
    }
}
