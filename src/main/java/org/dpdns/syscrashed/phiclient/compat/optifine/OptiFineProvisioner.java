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
 * 把 PhiClient jar 里内置的 OptiFine 取出来，并以<strong>jar 文件</strong>形式挂到类加载器上，
 * 让用户无需自己安装 OptiFine。
 *
 * <p>为什么必须是"jar 文件 + 父加载器可见"，而不是"解压成目录"或"只挂给 LaunchClassLoader"：
 *
 * <p>OptiFine 用 {@code getProtectionDomain().getCodeSource().getLocation().toURI()} 定位自己的 jar。
 * 而 {@code LaunchClassLoader#findClass} 是用
 * {@code new CodeSource(findResource(name).getURL(), ...)} 造 CodeSource 的 —— 也就是
 * <strong>jar 条目 URL</strong>（{@code jar:file:/.../OptiFine.jar!/optifine/X.class}）。
 * 于是 {@code new File(uri)} 必然抛 {@code IllegalArgumentException: URI is not hierarchical}，
 * OptiFine 随后打印 "*** Can not find the OptiFine JAR in the classpath ***" 并彻底失效。
 * 这点已实测：标准 launchwrapper-1.11 与 OptiFine 自带的 launchwrapper-of-2.2 行为完全一致。
 *
 * <p>官方之所以能用，是因为 {@code Launch} 在加载 tweaker 时做了这件事
 * （launchwrapper 1.11 {@code Launch.java:97}）：
 * <pre>
 * // Ensure we allow the tweak class to load with the parent classloader
 * classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
 * </pre>
 * 传 {@code --tweakClass optifine.OptiFineTweaker} 时，被排除的前缀正是 {@code optifine}，
 * 于是所有 {@code optifine.*} 改由<strong>父加载器（系统 classpath）</strong>加载 ——
 * 而普通 {@code URLClassLoader} 造的 CodeSource 是 {@code file:/.../OptiFine.jar}，定位成立。
 * 本类负责"让 OptiFine 的 jar 出现在父加载器能看到的地方"，
 * {@link OptiFineCompat#hook} 负责补上那条 {@code addClassLoaderExclusion}。
 */
public final class OptiFineProvisioner {

    private static final String RESOURCE_DIR = "/assets/phiclient/optifine/";
    private static final String RESOURCE_NAME = "OptiFine_1.8.9_HD_U_M5.jar";
    private static final String CACHE_NAME = "OptiFine_1.8.9_HD_U_M5.jar";

    private OptiFineProvisioner() {
    }

    /**
     * 供给 OptiFine：把内置的 OptiFine jar 落到用户目录缓存，并挂到类加载器上。
     *
     * @return 可用的 OptiFine jar 文件；无需/无法供给时返回 null
     */
    public static File provision(LaunchClassLoader classLoader) {
        if (isDeobfuscatedEnvironment()) {
            // 开发工作区跑的是 SRG 命名的 MC，OptiFine 的补丁按混淆类名匹配，压根不会生效，直接跳过。
            return null;
        }

        if (isVisibleToSystemClassLoader()) {
            // 系统 classpath 上已经有了（我们自己的启动脚本把 OptiFine 放进 libraries/ 就是这个情况），
            // 父加载器能直接看到，无需再解内置的那份。
            return null;
        }

        InputStream stream = OptiFineProvisioner.class.getResourceAsStream(RESOURCE_DIR + RESOURCE_NAME);
        if (stream == null) {
            // 没有内置 OptiFine：交给外置 classpath 或用户环境处理，保持向后兼容。
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

            // 父加载器可见 → optifine.* 由它加载 → CodeSource 是 file: 形式的 jar 路径
            addToSystemClassPath(target);
            // LaunchClassLoader 也要有 → 否则 net.optifine.*（408 个类）找不到
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
     * 判断是否跑在反混淆（SRG）环境里，例如 ForgeGradle 的 {@code runClient}。
     *
     * 依据：混淆环境下 Minecraft 类的资源路径是 {@code /ave.class}，
     * 而 SRG 环境下是 {@code /net/minecraft/client/Minecraft.class}。
     */
    private static boolean isDeobfuscatedEnvironment() {
        try {
            URL self = net.minecraft.client.Minecraft.class.getResource("Minecraft.class");
            return self != null && self.toString().contains("net/minecraft/client/Minecraft.class");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** OptiFine 是否已经能被父加载器（系统 classpath）看到。 */
    static boolean isVisibleToSystemClassLoader() {
        try {
            return ClassLoader.getSystemClassLoader().getResource(OptiFineCompat.RESOURCE_TRANSFORMER) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Java 8 下系统类加载器是 {@code URLClassLoader}，可以反射追加 URL。 */
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
