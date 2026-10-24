package org.dpdns.syscrashed.phiclient.compat.optifine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

/**
 * OptiFine（1.8.9 HD U M5）运行时兼容层。
 *
 * <p>为什么全部用反射：
 * <ul>
 *   <li>开发环境跑的是 SRG（MCP）命名的 Minecraft，而 OptiFine 的 {@code patch/*.xdelta}
 *       是对<strong>混淆（notch）类名 + 原始字节 MD5</strong>做的二进制增量补丁，
 *       所以 OptiFine 在开发环境下不可能生效，也不应该进编译期依赖；</li>
 *   <li>发布环境（玩家手上真实混淆过的 1.8.9）才有这些类和那些 patch。</li>
 * </ul>
 * 因此本类与 OptiFine 的一切交互都必须"按需反射 + 失败静默降级"。
 */
public final class OptiFineCompat {

    /** OptiFine 的类转换器；{@code optifine.OptiFineTweaker} 做的唯一一件事就是注册它。 */
    private static final String CLASS_TRANSFORMER = "optifine.OptiFineClassTransformer";
    /** OptiFine 的主配置类，位于默认包下（如 {@code Config.getVersion()} / {@code isFastRender()}）。 */
    private static final String CLASS_CONFIG = "Config";
    /** OptiFine 注入到 GameSettings 里的"快速渲染"开关字段名（混淆环境仍保留此名）。 */
    private static final String FIELD_FAST_RENDER = "ofFastRender";
    /** 转换器类的资源路径（探测用：只查资源不会触发类加载）。 */
    static final String RESOURCE_TRANSFORMER = "optifine/OptiFineClassTransformer.class";
    /**
     * 交给 {@code LaunchClassLoader#addClassLoaderExclusion} 的前缀。
     *
     * <p>与 launchwrapper 处理 {@code --tweakClass optifine.OptiFineTweaker} 时算出来的完全一致
     * （包名，不带末尾的点）。注意不要写成 {@code net.optifine}：那个包下的类仍需由
     * LaunchClassLoader 加载，否则以后加 {@code @Pseudo} mixin 会失效。
     */
    private static final String EXCLUSION_OPTIFINE = "optifine";

    private static boolean hookChecked;
    private static boolean hooked;
    private static boolean detected;

    private OptiFineCompat() {
    }

    /**
     * 早期存在性检测（对齐 SoarClient：它在 {@code SoarTweaker#acceptOptions} 里做同样的事）。
     *
     * <p>检测点是资源 {@code optifine/OptiFineClassTransformer.class}：
     * 它在 OptiFine 的 jar 里始终存在，且不依赖游戏是否已启动，因此适合放在 tweaker 最早的回调里判断。
     */
    public static boolean detectOptiFine() {
        detected = findOptiFineResource() != null;
        return detected;
    }

    /**
     * 只按资源探测，不 {@code Class.forName}。
     *
     * <p>原因：一旦在 {@code addClassLoaderExclusion} 之前用 LaunchClassLoader 把 OptiFine 的类
     * 加载进来，它就被缓存成"LaunchClassLoader 定义"的版本了，后面再排除也来不及，
     * 于是转换器照样拿不到 {@code file:} 形式的 CodeSource。
     */
    private static java.net.URL findOptiFineResource() {
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            if (system != null) {
                java.net.URL url = system.getResource(RESOURCE_TRANSFORMER);
                if (url != null) {
                    return url;
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }

        try {
            return OptiFineCompat.class.getClassLoader().getResource(RESOURCE_TRANSFORMER);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 尝试把 OptiFine 的类转换器挂到 LaunchWrapper 的转换链上。
     *
     * <p>必须由 {@code PhiTweaker#injectIntoClassLoader} 在 <strong>Mixin 初始化之前</strong>调用：
     * OptiFine 的增量补丁是针对"未被修改过的原版字节"做的（带 MD5 校验），
     * 若先被 Mixin 改过就可能对不上；正确顺序是 OptiFine 先打补丁，再由 Mixin 织入。
     *
     * @return true 表示 OptiFine 已可用（本次注册成功，或已被其它 tweaker 注册）
     */
    public static boolean hook(LaunchClassLoader classLoader) {
        if (hookChecked) {
            return hooked;
        }
        hookChecked = true;

        try {
            // 1) 【关键一步】让 optifine.* 改由父加载器（系统 classpath）加载。
            //    这正是 launchwrapper 为 --tweakClass optifine.OptiFineTweaker 做的事：
            //    Launch.java:97 → classLoader.addClassLoaderExclusion("optifine")。
            //    少了这一步，optifine.* 会由 LaunchClassLoader 自己 define，
            //    CodeSource 变成 jar 条目 URL，OptiFine 立刻 "Can not find the OptiFine JAR" 并失效。
            if (OptiFineProvisioner.isVisibleToSystemClassLoader()) {
                classLoader.addClassLoaderExclusion(EXCLUSION_OPTIFINE);
            } else {
                PhiLogger.warn("OptiFine is not visible to the parent class loader; "
                        + "it may fail to locate its own jar.");
            }

            Class<?> transformerClass = Class.forName(CLASS_TRANSFORMER, false, classLoader);

            /**
             *  OptiFineClassTransformer 在构造时把自己的实例写进静态字段 instance。
             *  若已经被别的入口（例如额外传了 --tweakClass optifine.OptiFineTweaker）注册过，
             *  这里就不要重复注册，否则同一个类会被打两次补丁。
             */

            Field instanceField = transformerClass.getField("instance");
            if (instanceField.get(null) != null) {
                hooked = true;
                return hooked;
            }

            classLoader.registerTransformer(CLASS_TRANSFORMER);
            hooked = instanceField.get(null) != null;
            if (!hooked) {
                PhiLogger.warn("OptiFine transformer was registered but did not initialize "
                        + "(it could not locate its own jar).");
            }
        } catch (ClassNotFoundException ignored) {
            // classpath 上没有 OptiFine（开发环境即如此），属于正常情况
            hooked = false;
        } catch (Throwable throwable) {
            hooked = false;
            PhiLogger.error("Failed to hook OptiFine.", throwable);
        }

        return hooked;
    }

    /**
     * {@link #detectOptiFine()} 的检测结果：classpath 上是否已存在 OptiFine。
     *
     * <p>用于避免重复加载 —— 用户已经用 OptiFine 安装器装过时，我们就不该再把内置的那份解压挂上去。
     */
    public static boolean isOptiFineDetected() {
        return detected;
    }

    /**
     * 实时检查 classpath 上是否已存在 OptiFine。
     *
     * <p>不能只信 {@link #isOptiFineDetected()} 的缓存结果：在 Forge 环境下 FML 是在
     * {@code injectIntoClassLoader} 阶段才把 mods 里的 jar 加进类加载器的，
     * 而我们更早的 {@code acceptOptions} 阶段去检测必然失败（会错误地缓存成"没有 OptiFine"）。
     */
    public static boolean isOptiFineOnClasspath() {
        return findOptiFineResource() != null;
    }

    /** OptiFine 是否真的可用。 */
    public static boolean isLoaded() {
        return hooked || detected || findConfigClass() != null;
    }

    /** 形如 {@code OptiFine_1.8.9_HD_U_M5}；不可用则返回 null。 */
    public static String getVersion() {
        Class<?> config = findConfigClass();
        if (config == null) {
            return null;
        }

        Object version = invokeStatic(config, "getVersion");
        return version == null ? null : String.valueOf(version);
    }

    /** 是否开启了 OptiFine 的"快速渲染"（多线程渲染，与多数自定义 2D 渲染冲突）。 */
    public static boolean isFastRenderEnabled() {
        return readBooleanFlag("isFastRender");
    }

    /** 是否正在使用光影着色器。 */
    public static boolean isShadersEnabled() {
        return readBooleanFlag("isShaders");
    }

    /**
     * 打开/关闭 OptiFine 的"快速渲染"。
     *
     * <p>自定义渲染（后续如 NanoVG / 自绘 HUD）一般需要关掉它，否则渲染状态会被线程破坏。
     * 注意 OptiFine 会在每次进入视频设置时重新读取配置，需要在合适时机反复调用。
     */
    public static boolean setFastRenderEnabled(boolean enabled) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.gameSettings == null) {
                return false;
            }

            Field field = findField(minecraft.gameSettings.getClass(), FIELD_FAST_RENDER);
            if (field == null || field.getType() != boolean.class) {
                return false;
            }

            field.setAccessible(true);
            field.setBoolean(minecraft.gameSettings, enabled);
            return true;
        } catch (Throwable throwable) {
            PhiLogger.error("Failed to set OptiFine fast render.", throwable);
            return false;
        }
    }

    private static boolean readBooleanFlag(String methodName) {
        Class<?> config = findConfigClass();
        if (config == null) {
            return false;
        }

        Object value = invokeStatic(config, methodName);
        return value instanceof Boolean && (Boolean) value;
    }

    private static Class<?> findConfigClass() {
        try {
            return Class.forName(CLASS_CONFIG);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> owner, String methodName) {
        try {
            Method method = owner.getMethod(methodName);
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> owner, String name) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
