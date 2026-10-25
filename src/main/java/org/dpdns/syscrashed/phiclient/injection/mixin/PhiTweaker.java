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
 * PhiClient launch entry point (the first layer)
 * (package {@code org.dpdns.syscrashed.phiclient.injection.mixin}).
 *
 * <p>Loaded by LaunchWrapper via the jar manifest's {@code TweakClass} (or build.gradle's
 * {@code tweakClass}). Its only responsibility is "bootstrapping": initialize Mixin, register the Mixin
 * config, set the obfuscation context and side (CLIENT), then hand the main class to
 * {@code net.minecraft.client.main.Main}.</p>
 *
 * <p>Business initialization does <b>not</b> live here - the real starting point is
 * {@code MixinMinecraft#startGame}'s injection, which calls {@code PhiClient.getInstance().start()}.</p>
 *
 * <p>Provided ITweaker methods:</p>
 * <ul>
 *   <li>{@link #acceptOptions(List, File, File, String)} - collects launch arguments (and does the early OptiFine probe).</li>
 *   <li>{@link #injectIntoClassLoader(LaunchClassLoader)} - registers transformers, unlocks LWJGL, provisions / hooks OptiFine, inits Mixin.</li>
 *   <li>{@link #getLaunchTarget()} - the main class to launch.</li>
 *   <li>{@link #getLaunchArguments()} - the collected launch arguments.</li>
 * </ul>
 */
public class PhiTweaker implements ITweaker {

    private static final String LAUNCH_TARGET = "net.minecraft.client.main.Main";
    private static final String MIXIN_CONFIG = "mixins.phiclient.json";
    private static final String OBFUSCATION_CONTEXT = "notch";

    private final List<String> launchArguments = new ArrayList<String>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // Earliest opportunity to probe for OptiFine's existence.
        OptiFineCompat.detectOptiFine();

        // FML calls the tweaker's acceptOptions twice (once via FMLTweaker, once via LaunchWrapper).
        // Without clearing first, arguments would be appended twice and MC would throw
        // joptsimple.MultipleArgumentsForOptionException: Found multiple arguments for option ['width']
        // -> Unable to launch. (PCL passes --width 854 --height 480, which hits exactly this.)
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
        // 0. Register NanoVG's bytecode transformer: redirect org.lwjgl.nanovg.NanoVGGLConfig.configGL(long)
        //    to our LWJGL 2 function provider, so NanoVG (LWJGL 3 binding) works inside MC 1.8.9's LWJGL 2
        //    environment. Must be registered before NanoVGGLConfig is loaded (the tweaker init stage satisfies this).
        classLoader.registerTransformer(LwjglTransformer.class.getName());

        // 0a. Unlock the "org.lwjgl." prefix - this is the prerequisite for the transformer above to work.
        //     LaunchClassLoader lists "org.lwjgl." as a classLoaderException by default; a class matching the
        //     exclusion is delegated straight to the parent (system classpath) and **never passes through
        //     LaunchClassLoader's bytecode transform**. So NanoVGGLConfig would use LWJGL 3's stock configGL,
        //     which does Class.forName("org.lwjgl.opengl.GL") to load LWJGL 3's GL module - but our trimmed
        //     lwjgl-phi.jar has no such class (and should not: LWJGL 2's lwjgl.jar is jar-level sealed, so
        //     adding classes under org.lwjgl.* throws SecurityException). The result is that on real hardware
        //     NanoVG context creation fails and the UI never draws (the dev environment passes only because
        //     the class paths differ). This matches the standard LWJGL unlock approach.
        unlockLwjgl(classLoader);

        // 0b. Provision the bundled OptiFine first (extract the jar and mount it), so the user needs no manual
        //     install. The key is to make the [parent (system classpath) loader] see it: OptiFine locates
        //     itself via getCodeSource(), which only yields a file: jar path when loaded by the parent loader.
        //     The dev environment (SRG) skips this automatically to avoid a meaningless load.
        if (OptiFineProvisioner.provision(classLoader) != null) {
            PhiLogger.info("OptiFine provisioned.");
        }

        // 0c. Hook OptiFine's class transformer. Must come before Mixin: OptiFine's patch/*.xdelta delta-patch
        //     the *unmodified vanilla bytes* (with MD5 verification); if Mixin changed them first, they would
        //     mismatch. Correct order is OptiFine patches, then Mixin weaves.
        //     This also adds addClassLoaderExclusion("optifine") - launchwrapper only does this automatically
        //     when passed --tweakClass optifine.OptiFineTweaker, so since we take over with our own tweaker we
        //     must do it manually, otherwise OptiFine cannot find its own jar.
        //     In the dev environment (SRG names) OptiFine is not on the classpath, so this silently skips.
        if (OptiFineCompat.hook(classLoader)) {
            PhiLogger.info("OptiFine class transformer registered.");
        }

        boolean forge = isForgePresent();

        // 1. Initialize the Mixin subsystem. This must be done ourselves regardless of Forge, or
        //    Mixins.addConfiguration throws "Environment conflict, mismatched versions or you didn't call
        //    MixinBootstrap.init()".
        MixinBootstrap.init();

        // 2. Register the Mixin config (a second safety net alongside the manifest's MixinConfigs).
        Mixins.addConfiguration(MIXIN_CONFIG);

        // 3. Obfuscation context and side: the only divergence point between the two modes.
        //    - Standalone tweaker (no Forge): must set it ourselves, otherwise Mixin does not take effect.
        //    - Forge: must leave it to FML's Mixin integration. If we force "notch" here too early, Mixin
        //      would apply the obfuscated names from the refmap before FML's deobfuscation remapper is ready,
        //      so the target class is still MCP-named net.minecraft.client.Minecraft while the refmap says ave,
        //      yielding "@Inject ... specifies a target class 'ave', which is not supported".
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
     * Removes the "org.lwjgl." prefix exclusion from LaunchClassLoader's classLoaderExceptions.
     *
     * <p>Without this, {@code org.lwjgl.nanovg.*} would be delegated to the parent loader and
     * {@link LwjglTransformer} would never be applied - so on real hardware NanoVG fails to create a context
     * for lack of GL function pointers.</p>
     *
     * <p>Reflection is used to stay compatible with both standard launchwrapper 1.11 and OptiFine's bundled
     * launchwrapper-of.</p>
     *
     * @param classLoader the LaunchClassLoader to patch.
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
     * Detects whether we are running inside a Forge environment.
     *
     * <p>Forge auto-scans jars in the mods directory that carry the {@code TweakClass} manifest attribute and
     * loads them (the log line "Loading tweaker ... from PhiClient-Release.jar" is FML doing this), so dropping
     * PhiClient into the mods folder hands control to FML - in which case the launch flow must yield to FML
     * and must not initialize Mixin as if it were a standalone tweaker.</p>
     *
     * @return true if the FML tweaker class is present.
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
