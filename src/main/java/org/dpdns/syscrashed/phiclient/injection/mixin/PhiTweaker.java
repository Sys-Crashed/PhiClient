package org.dpdns.syscrashed.phiclient.injection.mixin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dpdns.syscrashed.phiclient.compat.optifine.OptiFineCompat;
import org.dpdns.syscrashed.phiclient.compat.optifine.OptiFineProvisioner;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import org.dpdns.syscrashed.phiclient.injection.transformer.LwjglTransformer;
import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * PhiClient 启动入口（第一层入口）。
 *
 * 由 LaunchWrapper 通过 jar manifest 的 {@code TweakClass}（或 build.gradle 的 {@code tweakClass}）加载，
 * 职责仅限「引导」：初始化 Mixin、注册 Mixin 配置、设置混淆环境与侧（CLIENT），
 * 然后把主类交给 {@code net.minecraft.client.main.Main}。
 *
 * 业务初始化不写在这里 —— 真正的起点是
 * {@code MixinMinecraft#startGame} 注入点调用的 {@code PhiClient.getInstance().start()}。
 */

public class PhiTweaker implements ITweaker {

    private static final String LAUNCH_TARGET = "net.minecraft.client.main.Main";
    private static final String MIXIN_CONFIG = "mixins.phiclient.json";
    private static final String OBFUSCATION_CONTEXT = "notch";

    private final List<String> launchArguments = new ArrayList<String>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // 最早的机会点做 OptiFine 存在性检测
        OptiFineCompat.detectOptiFine();

        // FML 会把 tweaker 的 acceptOptions 调用两次（FMLTweaker 一次、LaunchWrapper 一次）。
        // 若不清空就直接累加，启动参数会变成两份，MC 解析时抛
        // joptsimple.MultipleArgumentsForOptionException: Found multiple arguments for option ['width']
        // → Unable to launch。（PCL 传了 --width 854 --height 480，正好撞上）
        this.launchArguments.clear();

        if (args != null) {
            this.launchArguments.addAll(args);
        }

        if (profile != null) {
            launchArguments.add("--version");
            launchArguments.add(profile);
        }

        if (assetsDir != null) {
            launchArguments.add("--assetsDir");
            launchArguments.add(assetsDir.getAbsolutePath());
        }

        if (gameDir != null) {
            launchArguments.add("--gameDir");
            launchArguments.add(gameDir.getAbsolutePath());
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // 0. 注册 NanoVG 的字节码变换器：把 org.lwjgl.nanovg.NanoVGGLConfig.configGL(long) 重定向到
        //    我们的 LWJGL 2 函数提供者，使 NanoVG（LWJGL 3 绑定）能在 MC 1.8.9 的 LWJGL 2 环境里工作。
        //    必须在 NanoVGGLConfig 被加载之前注册（tweaker 初始化阶段正好满足）。
        classLoader.registerTransformer(LwjglTransformer.class.getName());

        // 0a. 解锁 "org.lwjgl." 前缀 —— 这一步是上面那个变换器能生效的前提。
        //     LaunchClassLoader 默认把 "org.lwjgl." 列为 classLoaderException，命中排除项的类会直接
        //     委派给父加载器（系统 classpath），**根本不会经过 LaunchClassLoader 的字节码变换**。
        //     于是 NanoVGGLConfig 用的是 LWJGL 3 原版 configGL —— 它会 Class.forName("org.lwjgl.opengl.GL")
        //     去加载 LWJGL 3 的 GL 模块，而我们裁剪过的 lwjgl-phi.jar 里根本没有这个类（也不该有：
        //     LWJGL 2 的 lwjgl.jar 是 jar 级 sealed 的，往 org.lwjgl.* 里塞类会直接抛 SecurityException）。
        //     结果就是真机上 NanoVG 上下文创建失败、界面画不出来（dev 环境因为类路径不同反而能过）。
        //     与标准 LWJGL 解锁做法一致。
        unlockLwjgl(classLoader);

        // 0a. 先把内置的 OptiFine 供给出来（取出 jar 并挂到类加载器），让用户无需手动安装。
        //     关键是让【父加载器（系统 classpath）】也能看到它：OptiFine 靠 getCodeSource() 定位自身，
        //     只有由父加载器加载时 location 才是 file: 形式的 jar 路径。
        //     开发环境（SRG）会自动跳过，避免无意义的加载。
        if (OptiFineProvisioner.provision(classLoader) != null) {
            PhiLogger.info("OptiFine provisioned.");
        }

        // 0b. 挂上 OptiFine 的类转换器。必须排在 Mixin 之前：
        //     OptiFine 用 patch/*.xdelta 对「原版未修改字节」做增量补丁（带 MD5 校验），
        //     先被 Mixin 改过就可能对不上；正确顺序是先 OptiFine 打补丁，再由 Mixin 织入。
        //     这里还会补上 addClassLoaderExclusion("optifine") —— launchwrapper 只有在
        //     传 --tweakClass optifine.OptiFineTweaker 时才会自动做，我们从自己的 tweaker 接管就必须手动补，
        //     否则 OptiFine 找不到自己的 jar。
        //     开发环境（SRG 命名）classpath 上没有 OptiFine，这里会安静跳过。
        if (OptiFineCompat.hook(classLoader)) {
            PhiLogger.info("OptiFine class transformer registered.");
        }

        boolean forge = isForgePresent();

        // 1. 初始化 Mixin 子系统。
        //    这一步无论有没有 Forge 都必须自己做 —— 否则下面 Mixins.addConfiguration 会抛
        //    "Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()"。
        MixinBootstrap.init();

        // 2. 注册 Mixin 配置（与 manifest 的 MixinConfigs 双保险）
        Mixins.addConfiguration(MIXIN_CONFIG);

        // 3. 混淆环境与侧：这里是两种模式唯一的分歧点。
        //    - 独立 tweaker（无 Forge）：必须自己设，否则 Mixin 不生效。
        //    - Forge：必须交给 FML 的 Mixin 集成来设。若我们在这里提前强制成 notch，
        //      Mixin 会在 FML 反混淆 remapper 就绪之前就套用 refmap 里的混淆名，于是
        //      目标类还是 MCP 名 net.minecraft.client.Minecraft、而 refmap 给的是 ave，
        //      报 "@Inject ... specifies a target class 'ave', which is not supported"。
        if (forge) {
            PhiLogger.info("Forge detected, letting FML manage the Mixin environment.");
        } else {
            MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
            if (environment.getObfuscationContext() == null) {
                environment.setObfuscationContext(OBFUSCATION_CONTEXT);
            }
            environment.setSide(MixinEnvironment.Side.CLIENT);
        }
    }

    /**
     * 从 LaunchClassLoader 的 classLoaderExceptions 里移除 "org.lwjgl." 前缀排除。
     *
     * <p>没有这一步，{@code org.lwjgl.nanovg.*} 会被直接委派给父加载器，
     * {@link LwjglTransformer} 就永远不会被应用 —— 真机上 NanoVG 会因为拿不到 GL 函数指针而创建失败。
     *
     * <p>用反射是为了同时兼容
     * 标准 launchwrapper 1.11 与 OptiFine 自带的 launchwrapper-of。
     */
    private static void unlockLwjgl(LaunchClassLoader classLoader) {
        try {
            Field field = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            field.setAccessible(true);
            Object exclusions = field.get(classLoader);
            if (exclusions instanceof Set) {
                ((Set<?>) exclusions).remove("org.lwjgl.");
                PhiLogger.info("Unlocked org.lwjgl class loading for NanoVG.");
            }
        } catch (Throwable t) {
            PhiLogger.warn("Failed to unlock org.lwjgl class loading (NanoVG may fail to init): " + t);
        }
    }

    /**
     * 是否运行在 Forge 环境中。
     *
     * <p>Forge 会自动扫描 mods 目录里带 {@code TweakClass} 清单属性的 jar 并加载它们
     * （日志里 "Loading tweaker ... from PhiClient-Release.jar" 就是 FML 干的），
     * 所以把 PhiClient 丢进 mods 文件夹时会被 FML 接管 —— 这时启动流程必须让给 FML，
     * 不能再按"独立 tweaker"那套来初始化 Mixin。
     */
    private boolean isForgePresent() {
        try {
            Class.forName("net.minecraftforge.fml.common.launcher.FMLTweaker", false, getClass().getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String getLaunchTarget() {
        return LAUNCH_TARGET;
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments.toArray(new String[launchArguments.size()]);
    }
}
