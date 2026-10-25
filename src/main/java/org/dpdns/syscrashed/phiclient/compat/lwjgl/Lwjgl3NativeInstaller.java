package org.dpdns.syscrashed.phiclient.compat.lwjgl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * LWJGL 3 native library installer - resolves the "same-name lwjgl.dll" conflict with LWJGL 2
 * (package {@code org.dpdns.syscrashed.phiclient.compat.lwjgl}).
 *
 * <p><b>Problem</b>: LWJGL 3's {@code Library.loadSystem("lwjgl")} looks for a file named
 * {@code lwjgl.dll} in {@code java.library.path} <b>first</b>; MC 1.8.9's natives directory happens to
 * contain LWJGL 2's <b>32-bit</b> {@code lwjgl.dll} (the 64-bit JVM uses {@code lwjgl64.dll}, so that
 * 32-bit file is normally dead). LWJGL 3 then loads LWJGL 2's 32-bit dll into the 64-bit JVM - two native
 * libraries mixed -&gt; heap corruption (Windows exit code {@code 0xC0000374 / STATUS_HEAP_CORRUPTION}).</p>
 *
 * <p><b>Fix</b>: extract LWJGL 3's own natives ({@code lwjgl.dll} + {@code lwjgl_nanovg.dll}, from
 * {@code libs/lwjgl-phi-natives.jar}) into a temp directory, then <b>prepend</b> that directory to
 * {@code java.library.path}:
 * <ul>
 *   <li>LWJGL 3 -&gt; hits our correct dll ✓</li>
 *   <li>LWJGL 2 -&gt; still walks the native search path fixed at JVM startup (it wants {@code lwjgl64.dll} /
 *       {@code OpenAL64.dll}), completely unaffected ✓</li>
 * </ul>
 *
 * <p>Why this can be changed at runtime: LWJGL 3's {@code loadSystemFromLibraryPath} reads the
 * {@code java.library.path} property and walks the directories <em>when called</em>, not from a JVM-startup
 * cached native search path.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #install()} - installs the LWJGL 3 natives; must be called before any LWJGL 3 class (NanoVG binding) is initialized.</li>
 * </ul>
 */
public final class Lwjgl3NativeInstaller {

    private static boolean installed;

    private Lwjgl3NativeInstaller() {
    }

    /**
     * Installs the LWJGL 3 native libraries. Must be called before any LWJGL 3 class (the NanoVG binding)
     * is initialized.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        try {
            String[] resources = nativeResources();
            if (resources == null) {
                PhiLogger.warn("No LWJGL 3 natives available for this platform.");
                return;
            }

            File directory = new File(System.getProperty("java.io.tmpdir"), "phiclient-lwjgl3-natives");
            if (!directory.exists() && !directory.mkdirs()) {
                PhiLogger.warn("Cannot create LWJGL 3 natives directory: " + directory);
                return;
            }

            boolean ok = true;
            for (String resource : resources) {
                ok &= extract(resource, directory);
            }

            if (!ok) {
                PhiLogger.warn("Failed to extract all LWJGL 3 natives.");
                return;
            }

            String path = System.getProperty("java.library.path", "");
            System.setProperty("java.library.path",
                    directory.getAbsolutePath() + (path.isEmpty() ? "" : File.pathSeparator + path));

            PhiLogger.info("LWJGL 3 natives installed at " + directory.getAbsolutePath());
        } catch (Throwable t) {
            PhiLogger.error("Failed to install LWJGL 3 natives", t);
        }
    }

    /**
     * Returns the native resources needed for the current platform (paths inside the natives jar).
     *
     * @return the list of native resource paths, or {@code null} if the platform is unsupported (e.g. 32-bit).
     */
    private static String[] nativeResources() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        boolean x64 = arch.contains("64");
        if (os.contains("win")) {
            if (!x64) {
                return null;
            }
            return new String[] {
                    "windows/x64/org/lwjgl/lwjgl.dll",
                    "windows/x64/org/lwjgl/nanovg/lwjgl_nanovg.dll"
            };
        }
        if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            if (!x64) {
                return null;
            }
            return new String[] {
                    "linux/x64/org/lwjgl/liblwjgl.so",
                    "linux/x64/org/lwjgl/nanovg/liblwjgl_nanovg.so"
            };
        }
        return null;
    }

    /**
     * Extracts a native library from the classpath (natives jar) into the temp directory.
     *
     * @param resource  the in-jar resource path of the native.
     * @param directory the target directory.
     * @return true if the native is present (extracted or already cached).
     */
    private static boolean extract(String resource, File directory) {
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        File target = new File(directory, fileName);

        if (target.exists() && target.length() > 0) {
            return true;
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = Lwjgl3NativeInstaller.class.getResourceAsStream("/" + resource);
            if (in == null) {
                in = ClassLoader.getSystemResourceAsStream(resource);
            }
            if (in == null) {
                PhiLogger.warn("LWJGL 3 native not found on classpath: " + resource);
                return false;
            }

            out = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (Exception e) {
            PhiLogger.error("Failed to extract LWJGL 3 native: " + resource, e);
            return false;
        } finally {
            close(in);
            close(out);
        }
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
