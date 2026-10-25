package org.dpdns.syscrashed.phiclient.gui.nanovg;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.FunctionProvider;

/**
 * Adapts LWJGL 2's {@code GLContext#getFunctionAddress} to LWJGL 3's {@link FunctionProvider},
 * so NanoVG's native code can fill its GL function-pointer table
 * (package {@code org.dpdns.syscrashed.phiclient.gui.nanovg}).
 *
 * <p>Why this is needed: NanoVG (the LWJGL 3 {@code org.lwjgl.nanovg} binding), when creating a GL2
 * context, calls back {@code NanoVGGLConfig.configGL(long)} to obtain GL function pointers. The stock
 * LWJGL 3 implementation resolves its own GL module via {@code Class.forName("org.lwjgl.opengl.GL")},
 * but Minecraft 1.8.9 runs on LWJGL 2, so that fails.</p>
 *
 * <p>We instead use {@link org.dpdns.syscrashed.phiclient.injection.transformer.LwjglTransformer} to
 * rewrite {@code NanoVGGLConfig.configGL} at the bytecode level to call
 * {@code config(addr, new Lwjgl2FunctionProvider())}, fully bypassing the
 * {@code Class.forName("org.lwjgl.opengl.GL")} lookup. This class can therefore live in any package
 * without colliding with LWJGL 2's {@code org.lwjgl.opengl} security seal.</p>
 *
 * <p>Strict mode is used (constructing / resolving a missing address throws): these GL2-level
 * functions are guaranteed to exist in an LWJGL 2 context, so a missing pointer means the
 * environment is broken and failing early is easier to diagnose than a NULL dereference inside NanoVG.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getFunctionAddress(CharSequence)} - resolves a GL function by name (used by NanoVG).</li>
 *   <li>{@link #getFunctionAddress(ByteBuffer)} - unsupported overload (throws).</li>
 * </ul>
 */
public final class Lwjgl2FunctionProvider implements FunctionProvider {

    private final Method functionAddressMethod;

    public Lwjgl2FunctionProvider() {
        try {
            functionAddressMethod = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
            functionAddressMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        try {
            return (long) functionAddressMethod.invoke(null, functionName.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(ByteBuffer functionName) {
        throw new UnsupportedOperationException();
    }
}
