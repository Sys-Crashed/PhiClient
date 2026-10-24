package org.dpdns.syscrashed.phiclient.gui.nanovg;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.FunctionProvider;

/**
 * 把 LWJGL 2 的 {@code GLContext#getFunctionAddress} 包装成 LWJGL 3 的 {@link FunctionProvider}，
 * 供 NanoVG 的原生代码填充它的 GL 函数指针表。
 *
 * <p>为什么需要它：NanoVG（LWJGL 3 的 {@code org.lwjgl.nanovg} 绑定）在 GL2 后端创建上下文时，
 * 会回调 {@code NanoVGGLConfig.configGL(long)} 来取 GL 函数指针。原版 LWJGL 3 在这里通过
 * {@code Class.forName("org.lwjgl.opengl.GL")} 拿自己的 GL 模块，但 Minecraft 1.8.9 跑的是 LWJGL 2，
 * 没法直接用。
 *
 * <p>我们用 {@link org.dpdns.syscrashed.phiclient.injection.transformer.LwjglTransformer} 在字节码层面
 * 把 {@code NanoVGGLConfig.configGL} 改成直接 {@code config(addr, new Lwjgl2FunctionProvider())}，
 * 从而完全绕过那个 {@code Class.forName("org.lwjgl.opengl.GL")} —— 这样本类可以放在任意包下，
 * 不会和 LWJGL 2 的 {@code org.lwjgl.opengl} 撞 security seal。
 *
 * <p>采用严格模式（构造/取地址失败直接抛异常）：GL2 级别的这几个函数在 LWJGL 2 上下文里一定存在，
 * 一旦拿不到说明环境异常，早点失败比拿到 NULL 指针让 NanoVG 莫名其妙崩更好定位。
 */
public final class Lwjgl2FunctionProvider implements FunctionProvider {

    private final Method m_getFunctionAddress;

    public Lwjgl2FunctionProvider() {
        try {
            m_getFunctionAddress = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
            m_getFunctionAddress.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        try {
            return (long) m_getFunctionAddress.invoke(null, functionName.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(ByteBuffer functionName) {
        throw new UnsupportedOperationException();
    }
}
