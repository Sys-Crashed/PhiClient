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
 * Redirects NanoVG's (LWJGL 3 binding) GL dependency to our LWJGL 2 function provider
 * (package {@code org.dpdns.syscrashed.phiclient.injection.transformer}).
 *
 * <p>Concretely, when {@code org.lwjgl.nanovg.NanoVGGLConfig#configGL(long)} is loaded, its whole
 * body is replaced with:</p>
 * <pre>
 *     static void configGL(long addr) {
 *         config(addr, new Lwjgl2FunctionProvider());
 *     }
 * </pre>
 * <p>so NanoVG no longer tries {@code Class.forName("org.lwjgl.opengl.GL")} when creating a context
 * (that would pull in LWJGL 3's GL module, conflicting with MC 1.8.9's LWJGL 2; and the
 * {@code org.lwjgl.opengl} path is security-sealed by LWJGL 2's jar, which would corrupt GL state
 * and underflow the matrix stack).</p>
 *
 * <p>This transformer is registered via {@code classLoader.registerTransformer(...)} inside
 * {@code PhiTweaker#injectIntoClassLoader} and must take effect before {@code NanoVGGLConfig} is
 * loaded (registering during tweaker init satisfies that, long before the game runs).</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #transform(String, String, byte[])} - the IClassTransformer entry point; rewrites {@code configGL} for the NanoVG config class.</li>
 * </ul>
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
            // long addr occupies slots 0 and 1 (a long takes two slots).
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
