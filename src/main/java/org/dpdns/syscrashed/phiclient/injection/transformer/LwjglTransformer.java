package org.dpdns.syscrashed.phiclient.injection.transformer;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 把 NanoVG（LWJGL 3 绑定）原本依赖 LWJGL 3 OpenGL 模块的逻辑，重定向到我们的 LWJGL 2 函数提供者。
 *
 * <p>具体做法：在 {@code org.lwjgl.nanovg.NanoVGGLConfig#configGL(long)} 加载时，把它整个方法体替换成
 * <pre>
 *     static void configGL(long addr) {
 *         config(addr, new Lwjgl2FunctionProvider());
 *     }
 * </pre>
 * 这样 NanoVG 在创建上下文时不会再尝试 {@code Class.forName("org.lwjgl.opengl.GL")}（那会去加载
 * LWJGL 3 的 GL 模块、与 MC 1.8.9 的 LWJGL 2 冲突，且其所在的 {@code org.lwjgl.opengl} 路径还被
 * LWJGL 2 的 jar 做了 security seal，会直接破坏 GL 状态导致矩阵栈下溢）。
 *
 * <p>本类在 {@code PhiTweaker#injectIntoClassLoader} 里通过
 * {@code classLoader.registerTransformer(...)} 注册，必须在 {@code NanoVGGLConfig} 被加载之前生效
 * （tweaker 初始化阶段注册即可，远早于游戏运行）。
 */
public final class LwjglTransformer implements IClassTransformer {

    private static final String NANOVG_CONFIG = "org.lwjgl.nanovg.NanoVGGLConfig";
    private static final String FUNCTION_PROVIDER = "org/dpdns/syscrashed/phiclient/gui/nanovg/Lwjgl2FunctionProvider";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!NANOVG_CONFIG.equals(name)) {
            return basicClass;
        }

        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        for (MethodNode method : node.methods) {
            if (!"configGL".equals(method.name)) {
                continue;
            }

            InsnList list = new InsnList();
            // long addr 作为第 0 个 slot（long 占两个 slot，0 和 1）
            list.add(new VarInsnNode(Opcodes.LLOAD, 0));
            list.add(new TypeInsnNode(Opcodes.NEW, FUNCTION_PROVIDER));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    FUNCTION_PROVIDER,
                    "<init>",
                    "()V",
                    false));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "org/lwjgl/nanovg/NanoVGGLConfig",
                    "config",
                    "(JLorg/lwjgl/system/FunctionProvider;)V",
                    false));
            list.add(new InsnNode(Opcodes.RETURN));

            method.instructions.clear();
            method.instructions.insert(list);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(cw);
        return cw.toByteArray();
    }
}
