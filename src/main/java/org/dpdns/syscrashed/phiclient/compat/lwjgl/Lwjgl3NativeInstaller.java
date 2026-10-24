package org.dpdns.syscrashed.phiclient.compat.lwjgl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * LWJGL 3 原生库安装器 —— 解决与 LWJGL 2 的「同名 lwjgl.dll」冲突。
 *
 * <p><b>问题</b>：LWJGL 3 的 {@code Library.loadSystem("lwjgl")} 会<b>优先</b>从
 * {@code java.library.path} 里找名为 {@code lwjgl.dll} 的文件；而 Minecraft 1.8.9 的 natives 目录里
 * 正好躺着 <b>LWJGL 2 的 32 位</b> {@code lwjgl.dll}（64 位 JVM 用的是 {@code lwjgl64.dll}，
 * 所以那个 32 位文件平时是死的）。于是 LWJGL 3 把 LWJGL 2 的 32 位 dll 加载进了 64 位 JVM，
 * 两个 native 库混用 → 堆损坏（Windows 上表现为退出码 {@code 0xC0000374 / STATUS_HEAP_CORRUPTION}）。
 *
 * <p><b>解法</b>：把 LWJGL 3 自己的原生库（{@code lwjgl.dll} + {@code lwjgl_nanovg.dll}，
 * 来自 {@code libs/lwjgl-soar-natives.jar}）解到临时目录，再把这个目录<b>前置</b>到
 * {@code java.library.path}：
 * <ul>
 *   <li>LWJGL 3 → 命中我们解出来的正确 dll ✓</li>
 *   <li>LWJGL 2 → 仍走 JVM 启动时固定的 native 搜索路径（它要的是 {@code lwjgl64.dll} / {@code OpenAL64.dll}），
 *       完全不受影响 ✓</li>
 * </ul>
 *
 * <p>之所以能在运行期改：LWJGL 3 的 {@code loadSystemFromLibraryPath} 是<b>调用时</b>才读
 * {@code java.library.path} 属性并自己遍历目录找文件，而不是用 JVM 启动时缓存的 native 搜索路径。
 */
public final class Lwjgl3NativeInstaller {

    private static boolean installed;

    private Lwjgl3NativeInstaller() {
    }

    /**
     * 安装 LWJGL 3 原生库。必须在任何 LWJGL 3 类（NanoVG 绑定）被初始化之前调用。
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
     * 当前平台需要的原生库（在 natives jar 内的路径）。
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
     * 从 classpath（natives jar）里取出原生库写到临时目录。
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
