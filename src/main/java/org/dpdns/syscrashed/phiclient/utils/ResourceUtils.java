package org.dpdns.syscrashed.phiclient.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * 资源读取工具：把 jar / classpath 里的资源读成直接内存 ByteBuffer。
 *
 * <p>刻意<b>不</b>走 Minecraft 的 ResourceManager：1.8.9 的 DefaultResourcePack 只认
 * {@code minecraft} 这一个 domain，而且 classpath 兜底用的是 {@code ClassLoader.getSystemResourceAsStream}
 * —— 在 dev 环境（ForgeGradle 用自定义类加载器跑）里系统 classpath 根本不含我们的资源，
 * 界面字体就会读不到。直接用自己的类去 getResourceAsStream 则 dev / 真机都成立。
 *
 * <p>必须是 <b>直接内存 + nativeOrder</b>：NanoVG 的 {@code nvgCreateFontMem} 会直接拿这个
 * 指针去解析 TTF，普通堆内 ByteBuffer 的地址是无效的。
 */
public final class ResourceUtils {

    private ResourceUtils() {
    }

    /**
     * 从 classpath 读取资源（路径以 "/" 开头，例如 {@code /assets/phiclient/fonts/x.ttf}）。
     */
    public static ByteBuffer classpathToByteBuffer(String path, Class<?> owner) {
        InputStream in = null;
        try {
            in = owner.getResourceAsStream(path);
            if (in == null) {
                // 退一步用系统加载器（真机时资源在系统 classpath 上）
                in = ClassLoader.getSystemResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            }
            if (in == null) {
                PhiLogger.error("Resource not found on classpath: " + path);
                return null;
            }
            return toDirectBuffer(readAll(in));
        } catch (Exception e) {
            PhiLogger.error("Failed to read classpath resource: " + path, e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    private static ByteBuffer toDirectBuffer(byte[] bytes) {
        ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        data.put(bytes);
        ((Buffer) data).flip();
        return data;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, in.available()));
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
