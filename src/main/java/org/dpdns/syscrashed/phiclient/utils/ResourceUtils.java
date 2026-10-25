package org.dpdns.syscrashed.phiclient.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * Resource loading helpers for PhiClient (package {@code org.dpdns.syscrashed.phiclient.utils}).
 *
 * <p>Reads a resource from the classpath / jar into an off-heap {@link ByteBuffer}.
 * This deliberately does <b>not</b> go through Minecraft's {@code ResourceManager}: the 1.8.9
 * {@code DefaultResourcePack} only knows the {@code minecraft} domain, and its classpath fallback
 * uses {@code ClassLoader.getSystemResourceAsStream} - in a dev environment (ForgeGradle runs the
 * game with a custom class loader) the system classpath does not contain our resources, so the UI
 * fonts would fail to load. Reading via our own class {@code getResourceAsStream} works in both dev
 * and release builds.</p>
 *
 * <p>The buffer must be <b>direct + native-order</b>: NanoVG's {@code nvgCreateFontMem} takes this
 * pointer directly to parse the TTF, so a normal heap {@link ByteBuffer}'s address is invalid.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #classpathToByteBuffer(String, Class)} - reads a classpath resource into a direct buffer.</li>
 * </ul>
 */
public final class ResourceUtils {

    private ResourceUtils() {
    }

    /**
     * Reads a classpath resource into a direct, native-ordered {@link ByteBuffer}.
     *
     * <p>The path must start with {@code /} (e.g. {@code /assets/phiclient/fonts/x.ttf}). If the
     * owning class cannot find the resource, the system class loader is tried as a fallback (in a
     * release build the resources live on the system classpath).</p>
     *
     * @param path  the resource path, starting with {@code /}.
     * @param owner the class used to resolve the resource (usually the calling class).
     * @return the resource content as a direct buffer, or {@code null} if it could not be read.
     */
    public static ByteBuffer classpathToByteBuffer(String path, Class<?> owner) {
        InputStream in = null;
        try {
            in = owner.getResourceAsStream(path);
            if (in == null) {
                // Fall back to the system loader (in release builds the resources sit on the system classpath).
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

    /**
     * Wraps raw bytes into a direct, native-ordered buffer and flips it for reading.
     *
     * @param bytes the raw resource bytes.
     * @return a direct buffer ready to be read from the start.
     */
    private static ByteBuffer toDirectBuffer(byte[] bytes) {
        ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        data.put(bytes);
        ((Buffer) data).flip();
        return data;
    }

    /**
     * Fully drains an {@link InputStream} into a byte array.
     *
     * @param in the stream to read.
     * @return the complete content of the stream.
     * @throws Exception if reading fails.
     */
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
