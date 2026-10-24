# SoarClient 技术参考文档

> 版本：`Soar` v7.1.2（源码中 `Soar.java` 硬编码 `version = "7.1.2"`）
> 目标游戏：Minecraft 1.8.9（映射 `stable_22`）
> 构建链：ForgeGradle 2.1 + MixinGradle 0.6 + Gradle 4.7 + **Java 8**
> 本质：**tweaker 注入式客户端 mod**（不是 Forge mod，不依赖 FML `@Mod`；通过自定义 `ITweaker` + SpongePowered Mixin 在启动时织入原版代码）
> 备份仓库（冻结，不再更新）：https://github.com/Sys-Crashed/Legacy-SoarClient

本文档用于在不重新阅读全部源码、甚至源码被清空后，仍能快速理解项目架构、构建约束与扩展方式。所有细节以备份仓库为准。

---

## 1. 项目本质与整体结构

SoarClient 是一个**客户端 PvP 修改版**，运行方式是在启动 Minecraft 1.8.9 时作为 **Tweaker** 被 LaunchWrapper 加载，再用 **Mixin** 在类加载期改写/扩展原版类（`net.minecraft.*`）。它**不是**标准 Forge mod（没有 `net.minecraftforge.fml.common.Mod` 注解，没有 `mcmod.info`），因此：

- 可以被任何支持 `--tweakClass` 的启动器加载（或开发时用 `gradlew runClient`）；
- 不能在服务端运行（`tweaker-client` 插件 + 大量 `@Mixin` 只针对客户端类）；
- 全部"功能"通过 Mixin 钩入原版生命周期 + 自研管理系统（模块、事件总线、GUI）实现。

顶层包：`me.eldodebug.soar`，核心入口文件：
- `Soar.java`：客户端单例（`getInstance()`），`start()` 是初始化主流程。
- `SoarAPI.java`：对外/内部 API，负责创建 `GuiModMenu`、`GuiSoarMainMenu` 等。
- `SoarHandler.java`：全局事件监听器（tick、加入服务器、皮肤/披风、包等）。
- `injection/mixin/SoarTweaker.java`：Tweaker 入口（见第 3 节）。

主要子包职责一览：

| 包 | 职责 |
|---|---|
| `management` | 业务"大脑"：账号、模块(Mod)、事件、配置、颜色、音乐、档案、截图、路点、通知等管理器 |
| `management.account[.microsoft/.skin]` | 离线 / Microsoft 登录、皮肤下载 |
| `management.mods[.impl/.settings]` | 模块框架 + 130+ 具体模块实现 + 设置项系统 |
| `management.event[.impl]` | 自研事件总线 + 大量事件类 |
| `management.nanovg` | NanoVG 2D 矢量渲染管理器、字体与纹理 |
| `management.profile` / `mainmenu` | 按服务器 IP 自动切换的配置档案 / 主菜单背景 |
| `gui[.mainmenu/.modmenu/...]` | 自定义 GUI（主菜单、模块菜单、HUD 编辑、浏览器、路点等） |
| `ui` | 可复用 UI 组件库 |
| `hooks` | 原版 GUI/渲染钩子 |
| `injection` | 注入基础设施：`mixin`（SoarTweaker、Mixin 类、interfaces/Accessor）、`transformer`（LWJGL 转换器） |
| `discord[.ipc.*]` | Discord 富媒体状态（RPC），含自研 IPC 管道 |
| `mcef` | JCEF（Chromium Embedded）浏览器封装 |
| `viaversion[.platform.*]` | ViaVersion / ViaBackwards / ViaRewind 跨版本协议兼容 |
| `utils` | 通用工具（渲染、动画、向量数学、网络、文件、颜色等） |
| `logger` | `SoarLogger`（Log4j 封装） |
| `mobends` / `mp3agic` | 第三方库重打包（动画 / MP3 元数据） |

---

## 2. 构建与运行环境（重要约束）

> ⚠️ 这套构建链对版本极其敏感，以下约束是"能跑起来"的前提。

### 2.1 硬性版本要求
- **必须使用 Java 8**（Amazon Corretto 1.8.0_472 实测可用，位于 `C:\Users\SysCr\.jdks\corretto-1.8.0_472`）。Java 9+ 会让 Gradle 4.7 在启动期直接崩（`ScriptPluginFactory`/`PluginResolutionStrategyInternal` 报错）——因为 Gradle 4.x 内部服务 API 在 Java 9+ 已被移除。
- **Gradle 4.7**（由 `gradle-wrapper.properties` 固定，`gradle-4.7-bin.zip`）。
- **ForgeGradle 2.1-SNAPSHOT** + **MixinGradle 0.6-SNAPSHOT**（见 `build.gradle` 的 `buildscript.dependencies`）。
- **不要升级** Gradle / ForgeGradle / Mixin 版本——老 ForgeGradle 2.1 必须配 Gradle 4.x + Java 8，强行升级会破坏 MCP 反混淆流程。

### 2.2 网络 / 代理
- 中国大陆环境访问 `libraries.minecraft.net`、`files.minecraftforge.net`、`github.com` 等可能不稳定或被挡。
- 本机 Windows 系统代理为 `127.0.0.1:55179`（`HKCU\...\Internet Settings` 的 `ProxyEnable=1`）。**Git 不会自动读取系统代理**，需显式配置：
  ```powershell
  git config --global http.proxy http://127.0.0.1:55179
  git config --global https.proxy http://127.0.0.1:55179
  ```
- `gh` CLI 装在 `C:\Program Files\GitHub CLI\gh.exe`（不在默认 PATH，需用完整路径或新开终端）。
- 构建时下载原生库（lwjgl/twitch）或资源索引（`1.8.json`）出现 `Connection reset` 属网络抖动，重跑即可，Gradle 会缓存已成功的下载。

### 2.3 常用命令（均需在 JDK 8 下）
```powershell
# 进入项目目录后，先确保 JAVA_HOME 指向 JDK 8
$env:JAVA_HOME="C:\Users\SysCr\.jdks\corretto-1.8.0_472"
$env:PATH="C:\Users\SysCr\.jdks\corretto-1.8.0_472\bin;"+$env:PATH

.\gradlew.bat setupdecompworkspace   # 首次：反混淆 MC、下载依赖（耗时，可能需重试）
.\gradlew.bat genIntellijRuns         # 为 IntelliJ 生成模块 classpath 与 runClient 运行配置
.\gradlew.bat build                   # 产出 build/libs/SoarClient-Release.jar
.\gradlew.bat runClient               # 启动客户端（开发调试；生成 run/ 目录）
```
IntelliJ 中：将项目 SDK 与 Gradle JVM 设为 JDK 8，然后右侧 Gradle 面板点 "Refresh all Gradle projects" 重新同步（否则 `Minecraft`/`JsonObject`/`lang3` 等会报红——它们是反混淆 MC jar 与 MC 自带库，构建能用但 IDE 未同步）。

---

## 3. 启动与注入机制（核心）

### 3.1 构建期装配（build.gradle 关键块）
```gradle
apply plugin: 'net.minecraftforge.gradle.tweaker-client'
apply plugin: 'org.spongepowered.mixin'
apply plugin: 'java'

minecraft {
    version = "1.8.9"
    tweakClass = "me.eldodebug.soar.injection.mixin.SoarTweaker"
    runDir = "run"
    mappings = "stable_22"
    clientJvmArgs = ["-XX:-DisableExplicitGC"]
    makeObfSourceJar = false
}
// ...
mixin {
    defaultObfuscationEnv = "notch"
    add sourceSets.main, "mixins.soar.refmap.json"
}
// jar 的 manifest：
manifest.attributes(
    "MixinConfigs": 'mixins.soar.json',
    "TweakClass": 'me.eldodebug.soar.injection.mixin.SoarTweaker',
    "TweakOrder": 0,
    "Manifest-Version": 1.0,
    'FMLAT': 'soar_at.cfg'
)
```
- 依赖通过 `configurations { embed; compile.extendsFrom(embed) }` 把 Mixin 与 `libs/*.jar` **shade（内嵌）** 进最终 jar（排除 `META-INF` 签名）。
- `version = "Release"`、`group = "me.eldodebug"`、`archivesBaseName = "SoarClient"`。

### 3.2 运行时加载：SoarTweaker（实现 `net.minecraft.launchwrapper.ITweaker`）
`injection/mixin/SoarTweaker.java` 的关键动作（`injectIntoClassLoader`）：
1. `classLoader.registerTransformer(LwjglTransformer.class.getName())` —— 注册自定义 ASM 字节码转换器（见 3.4）。
2. `MixinBootstrap.init()` 初始化 Mixin。
3. `Mixins.addConfiguration("mixins.soar.json")` 注册 Mixin 配置（与 manifest 的 `MixinConfigs` 双保险）。
4. 若 `ObfuscationContext == null` 设为 `"notch"`，`setSide(CLIENT)`。
5. `unlockLwjgl()`：反射从 `LaunchClassLoader.classLoaderExceptions` 中移除 `"org.lwjgl."`，使客户端可重新织入 LWJGL 类。
- `acceptOptions(...)`：探测 OptiFine（`Class.forName("optifine.Patcher")` → 静态 `hasOptifine`，供 `Soar.removeOptifineZoom()` 使用），收集启动参数。
- `getLaunchTarget()` 返回 `net.minecraft.client.main.Main`。

### 3.3 Mixin 配置：mixins.soar.json
位于 `src/main/resources/mixins.soar.json`：
```json
{
  "package": "me.eldodebug.soar.injection.mixin.mixins",
  "refmap": "mixins.soar.refmap.json",
  "compatibilityLevel": "JAVA_8",
  "mixins": [ "client.MixinMinecraft", "gui.MixinGuiNewChat", ... 约 100 个 ]
}
```
- **无 `plugin` 字段** → 没有自定义 Mixin 配置插件。
- `refmap` 文件**由 MixinGradle 在构建期生成**，`src/main/resources` 中不存在（仓库里没有）。开发环境用 searge 映射故本地 run 正常；发布 jar 必须构建生成它，否则线上混淆名无法回映射。
- 新增/删除 mixin 必须同步修改这里面的列表。

### 3.4 自定义 ASM 转换器：LwjglTransformer
`injection/transformer/LwjglTransformer.java` 实现 `IClassTransformer`，改写 `org.lwjgl.nanovg.NanoVGGLConfig.configGL`，把原 `FunctionProvider.CONFIG_GL` 调用替换为自研 `Lwjgl2FunctionProvider`（通过 LWJGL2 的 `GLContext.getFunctionAddress` 提供 GL 函数地址）。**目的**：让 NanoVG（默认面向 LWJGL3 FunctionProvider）能在 1.8.9 的 LWJGL2 上工作。

### 3.5 Accessor / Invoker 接口
位于 `injection/interfaces/`：`IMixinMinecraft`、`IMixinEntityPlayer`、`IMixinRenderPlayer`、`IMixinChatLine`、`IMixinWorld`、`IMixinShaderGroup` 等。Mixin 类 `implements` 这些接口，用 `@Accessor`/`@Invoker` 暴露原版私有字段/方法（如 `MixinMinecraft.setSession()`、`isRunning()`、`resizeWindow()`）。业务代码通过强转调用：`((IMixinMinecraft) mc).setSession(...)`。

---

## 4. 初始化流程（Soar.start）

由 `MixinMinecraft.startGame` 在 `ingameGUI` 字段初始化之后 `@Inject` 触发 `Soar.getInstance().start()`。顺序如下：

1. `OptifineUtils.disableFastRender()` + 移除 OptiFine 缩放键绑定（`removeOptifineZoom()`）。
2. `FileManager` → `DownloadManager` → `LanguageManager` → `AccountManager`（构造时加载 `Account.json`、异步刷新 Microsoft token）→ `EventManager`。
3. `ModManager` + `modManager.init()`（注册全部 130+ 模块）。
4. `CapeManager` → `ColorManager` → `ProfileManager`（加载 `profiles/Default.json`）→ `MusicManager`。
5. `SoarAPI` 构造 + `init()`（创建 `GuiModMenu` 与 `GuiSoarMainMenu`）。
6. `CommandManager` → `ScreenshotManager` → `NotificationManager` → `SecurityFeatureManager` → `QuickPlayManager` → `ChangelogManager` → `WaypointManager`。
7. `eventManager.register(new SoarHandler())`。
8. `setupLibraryPath()`：设系统属性 `jcef.path` 指向 `soar/external/cef`。
9. `GlobalSettingsMod.getInstance().setToggled(true)` 并 `mc.updateDisplay()`。

关闭：`MixinMinecraft.shutdownMinecraftApplet` HEAD → `Soar.stop()` 保存 profile 与 account。

---

## 5. Mixin 子系统

### 5.1 分类（按 `mixins.soar.json` 子包）
- **client**：`MixinMinecraft`（最核心，见 5.3）。
- **gui**：`GuiIngame`/`GuiNewChat`/`ChatLine`/`GuiScreen`/`GuiChat`/`GuiContainer`/`GuiMultiplayer`/`GuiOptions`/`GuiLanguage`/`GuiPlayerTabOverlay`/`GuiScreenBook`/`GuiScreenResourcePacks`/`GuiDownloadTerrain`/`GuiGameOver` 等。
- **render**：`EntityRenderer`/`RendererLivingEntity`/`ItemRenderer`/`Render`/`RenderItem`/`RenderGlobal`/`RenderManager`/`RenderEntityItem`/`BlockModelRenderer`/`WorldRenderer`/`RenderTNTPrimed`/`InventoryEffectRenderer`/`AmbientOcclusionFace` 等。
- **entity**：`Entity`/`EntityLivingBase`/`EntityPlayer`/`EntityPlayerSP`/`EntityXPOrb`/`AbstractClientPlayer`/`RenderPlayer`/`EntityList`/`EntityOtherPlayerMP`/`BaseAttribute`。
- **network**：`NetHandlerPlayClient`/`NetworkManager`(含 `NetworkManager5`)/`PacketThreadUtil`/`C08PacketPlayerBlockPlacement`/`C17PacketCustomPayload`/`S3FPacketCustomPayload`/`S14PacketEntity`/`LazyLoadBase`。
- **world / chunk**：`World`/`WorldClient`/`WorldInfo`/`WorldChunkManager`/`ChunkCache`/`NodeProcessor`/`Chunk`/`AnvilChunkLoader`/`ChunkRenderDispatcher`/`RenderChunk`/`VisGraph`/`ChunkRenderContainer`。
- **block**：`BlockPos`（**13 个 @Overwrite**）、`BlockGlass`/`BlockStainedGlass`/`BlockFluidRenderer`/`BlockRedstoneTorch`/`PropertyBool`/`PropertyInteger`/`PropertyHelper`。
- **item / layer / model / tileentity / scoreboard / server / settings / shader / texture / util / vertex / nbt / resources / multiplayer / optifine / lwjgl**：
  - `lwjgl`：`LinuxKeyboard`/`LinuxKeycodes`/`WindowsDisplay`（平台相关修复）。
  - `optifine`：`MixinLightMap`。

### 5.2 注解使用模式
- `@Mixin(目标类.class)`：指定被织入的原版类。
- `@Shadow`：直接读写原版私有字段/方法（无需反射，但要对应混淆名，配合 refmap）。
- `@Inject`：在方法指定位置（HEAD/RETURN/TAIL，或 `@At` 精确点）插入代码；是事件派发的主要手段。
- `@Redirect`：替换某次方法调用/字段访问（如拦截 `Mouse.next()`、`System.gc()`、Twitch 流代码、攻击挥砍改走 ViaVersion `AttackOrder`）。
- `@Overwrite`：整体替换原版方法（如 `MixinBlockPos` 的全部坐标方法、`getLimitFramerate` 等）。
- `@Accessor`/`@Invoker`：暴露私有成员给业务代码（配合 `injection/interfaces`）。

### 5.3 典型示例：MixinMinecraft（最核心）
- `@Shadow` 大量原版私有成员（`timer`/`session`/`thePlayer`/`gameSettings`/`clickMouse()` 等）。
- `@Inject`：
  - `startGame` 的 `ingameGUI` 字段之后 → 触发 `Soar.start()`；
  - `runTick` TAIL → `new EventTick().call()`（事件总线）；
  - `runTick` 键处理之后 → `EventKey`；
  - `shutdownMinecraftApplet` HEAD → `Soar.stop()`；
  - `drawSplashScreen` HEAD cancellable → 替换为 `GuiSplashScreen`；
  - `displayGuiScreen` RETURN → 若显示 `GuiMainMenu` 替换为 Soar 主菜单。
- `@Redirect`：拦截 `Mouse.next()` / `getEventDWheel()` / `System.gc()`（切世界优化）/ 攻击逻辑。
- `@Overwrite`：`getLimitFramerate()` / `isFramerateLimitBelowMax()` / `getDebugFPS()`。

---

## 6. 模块（Mod）系统

- `Mod`（基类）：持有名称/描述（用 `TranslateText` 多语言键）、开关状态、`SimpleAnimation`、所属 `ModCategory`（ALL/PLAYER/RENDER/HUD/WORLD/OTHER）。`onEnable/onDisable` 通过 `EventManager.register/unregister(this)` 接入事件总线。
- `HUDMod extends Mod`：可拖拽 HUD 模块基类（x/y/scale/draggable），封装 NanoVG 绘制助手（scissor、drawPlayerHead、drawRoundedImage 等）。
- `ModManager.init()`：**手动 `mods.add(new XxxMod())` 注册 130+ 模块**（AppleSkin、ArmorStatus、Keystrokes、Minimap、Zoom、Waypoint、ViaVersionMod、DiscordRPCMod、Mechvibes、MoBends、OldAnimations……）。
- 设置体系：`settings.Setting` 基类 + 实现 `BooleanSetting`/`NumberSetting`/`ColorSetting`/`ComboSetting`/`TextSetting`/`KeybindSetting`/`ImageSetting`/`SoundSetting`。构造时 `addSettings(...)` 集中管理，自动被 profile 持久化。

---

## 7. 主要功能子系统

- **账号系统**：`AccountManager` 用 Gson 读写 `soar/Account.json`；`microsoft.MicrosoftAuthentication` 实现完整 Xbox Live → XSTS → Minecraft Services OAuth2 流程（含拥有权校验、皮肤下载）；`microsoft.MicrosoftLoginWindow` 是基于 Swing + JavaFX `WebView` 的登录弹窗（⚠️ 用了 `sun.net.www.protocol.https.Handler`，见第 9 节）。
- **GUI / NanoVG**：`management.nanovg.NanoVGManager` 用 `NanoVGGL2.nvgCreate(...)` 创建矢量绘制上下文，提供圆角矩形/渐变/字体/贴图等 2D UI 能力；主菜单 `gui.mainmenu.GuiSoarMainMenu` 用场景切换系统（`MainScene`/`AccountScene`/`BackgroundScene`/`ShopScene`/`MicrosoftLoginScene` + `welcome.*` 首次引导）；模块菜单 `gui.modmenu.GuiModMenu`。
- **ViaVersion 跨版本**：基于 **ViaLoadingBase** 封装 `com.viaversion.viaversion`，原生协议 `NATIVE_VERSION = 47`（1.8.9）。`ViaSoar` builder 初始化并注册 `ViaVersionPlatformImpl`/`ViaBackwardsPlatformImpl`/`ViaRewindPlatformImpl`（平台实现在 `platform/`），通过 `inClassPath(...)` 探测 Backwards/Rewind 是否在 classpath。`netty` 包下 `VLBViaDecodeHandler`/`VLBViaEncodeHandler` 注入 Netty 管线；`fixes.AttackOrder`、`FixedSoundEngine` 修复跨版本问题。`AsyncVersionSlider` 供游戏内切换协议版本。
- **Discord RPC**：`DiscordRPC` 用 `IPCClient(1059341815205068901L)` 连接 Discord 设置 RichPresence；`discord.ipc.*` 是自研 IPC（命名管道，`WindowsPipe` 等）。
- **JCEF 浏览器**：`mcef` 封装 Java Chromium Embedded，无窗口渲染，缓存目录 `soar/cache/mcef`；`UnsafeUtils` 用 `sun.misc.Unsafe` 构造 `KeyEvent`（见第 9 节）。
- **事件总线**：`management.event` 自研事件系统，模块用 `@EventTarget` 方法监听，`EventManager` 管理注册与优先级（`Priority`）。
- **配置持久化**：根目录 `Minecraft.mcDataDir/soar/`（由 `FileManager` 创建）：
  - `Account.json`（账号 + refreshToken）
  - `profile/`（每 profile 一个 JSON，如 `Default.json`，含模块开关与设置；按服务器 IP 自动加载，`SoarHandler.onJoinServer`）
  - `cache/`（版本标记、皮肤、head、mcef）、`music/`、`external/`（含 `microsoft`、`cef`）、`screenshots/`
  - 语言：`assets/minecraft/soar/language/{en,cn,ja}.properties` + `TranslateText`。

---

## 8. 依赖（libs/，共 11 个 jar，构建期 shade 进 jar）

| 文件 | 用途 |
|---|---|
| `Java-WebSocket-1.5.4.jar` | WebSocket 客户端（实时数据/商店/通信） |
| `ViaVersion-4.7.0-1.20-pre2-SNAPSHOT.jar` | 协议转换核心（1.8.9 ↔ 新版服务端） |
| `ViaBackwards-4.7.0-1.20-pre1-SNAPSHOT.jar` | 连接更高版本服务端 |
| `ViaRewind-2.0.4-SNAPSHOT.jar` | 支持 1.7 及更早协议 |
| `jcef.jar` | Java Chromium Embedded（内置浏览器/登录） |
| `jfxrt.jar` | JavaFX 运行时（`MicrosoftLoginWindow` WebView 依赖，~17MB） |
| `jfxswt.jar` | JavaFX-SWT 桥接（JFXPanel 嵌入 Swing） |
| `lwjgl-soar.jar` + `lwjgl-soar-natives.jar` | 定制 LWJGL（含 nanovg 绑定与 natives）供 NanoVG 使用 |
| `slf4j-api-2.0.7.jar` | 日志门面（ViaVersion 等依赖） |
| `snakeyaml-2.0.jar` | YAML 解析（ViaVersion 配置） |

---

## 9. 已知坑 / 构建特征（排错用）

1. **`sun.*` 内部 API 依赖**（Java 8 才能编译/运行）：
   - `mcef/UnsafeUtils.java`：反射获取 `sun.misc.Unsafe.theUnsafe` 直接写 `KeyEvent` 私有字段。
   - `account/microsoft/MicrosoftLoginWindow.java`：用 `sun.net.www.protocol.https.Handler` 拦截 https 重定向拿 OAuth code。
   - 升级到 Java 11+ 这两处会编译失败（项目目前锁死 Java 8）。
2. **refmap 不在源码中**：`mixins.soar.refmap.json` 仅构建期生成；只拷 `.class` 而不经 gradle 构建会导致混淆名回映射失败。
3. **`@Overwrite` 缺 Javadoc**：全仓约 27 处 `@Overwrite`（MixinBlockPos 单文件 13 个）。Mixin 0.7.11 默认对其强制要求 Javadoc（否则 errorLevel=ERROR）。实务用 JVM 参数 `-Dmixin.overwriteErrorLevel=warn` 或在构建/加载时放宽；补全 Javadoc 亦可。
4. **`FMLAT: soar_at.cfg` 引用但文件缺失**：`build.gradle` manifest 声明了该 Access Transformer，但仓库中找不到对应文件——FML 加载时尝试读取会失败或 AT 不生效（对需要放宽访问级别的字段/方法是隐患）。
5. **LWJGL2 ↔ NanoVG 桥接**：依赖 `LwjglTransformer` 改写字节码 + `unlockLwjgl()` 移除 `classLoaderExceptions` 中的 `org.lwjgl.`，否则 NanoVG 无法在 LWJGL2 上解析 GL 函数。
6. **Twitch 流代码被剔除**：`MixinMinecraft` 用 `@Redirect` 把 `IStream` 相关方法置空（禁用已废弃的 Twitch 直播集成）。
7. **OptiFine 兼容**：`SoarHandler.onTick` 每 tick 重新 `disableFastRender()`，确保与 NanoVG 兼容；`hasOptifine` 静态标志用于条件逻辑。
8. **构建时的 65 个警告**：多为 Mixin 映射提示（`Cannot find method mapping for @At` 指向 lwjgl/java 库方法，正常）与 `@Overwrite` 缺 javadoc，**均不影响 BUILD SUCCESSFUL**，可忽略。

---

## 10. 扩展指南（从代码推断的模式）

### 新增一个 Mixin
1. 在 `src/main/java/me/eldodebug/soar/injection/mixin/mixins/<category>/` 新建 `MixinXxx.java`，加 `@Mixin(目标类.class)`。
2. 读写私有成员用 `@Shadow`；要暴露给业务代码则在 `injection/interfaces/` 定义接口并让 mixin `implements`，用 `@Accessor`/`@Invoker` 实现，业务侧 `(IMixinXxx) instance` 强转调用。
3. 钩入逻辑用 `@Inject`/`@Redirect`/`@ModifyArg`/`@Overwrite`，`target` 用 obf 名（`net/minecraft/...`）以保证 refmap 正确。
4. 把 `category.MixinXxx` 加进 `src/main/resources/mixins.soar.json` 的 `mixins` 列表。
5. 重新构建以生成/刷新 `mixins.soar.refmap.json`。

### 新增一个模块（Mod）
1. `management/mods/impl/` 新建类，继承 `Mod`（纯逻辑/设置）或 `HUDMod`（可拖拽界面）。
2. 构造器传 `new Mod(nameTranslate, descTranslate, ModCategory.X)`，`setup()` 初始化，`onEnable/onDisable` 已自动注册/注销事件。
3. 在 `management/mods/ModManager.init()` 末尾 `mods.add(new YourMod());`。
4. 设置项：`addSettings(new BooleanSetting(...), new NumberSetting(...), ...)`，自动随 profile 持久化。
5. 状态随 `ProfileManager` 写入 `soar/profile/*.json`，按服务器 IP 在 `SoarHandler.onJoinServer` 自动切换。

### 新增事件 / 集成
- 事件：新建 `management/event/impl/EventXxx extends Event`，在合适的 Mixin 里 `new EventXxx(...).call()`，模块用 `@EventTarget` 监听。
- 跨版本：在 `viaversion/platform/` 扩展平台实现，或在 `ViaSoar` builder 注册新 `Platform`（参考 `inClassPath` 探测模式）。

---

## 11. 参考与备注

- 完整源码（冻结备份）：https://github.com/Sys-Crashed/Legacy-SoarClient
- 本文档撰写时项目仍含全部 `me.eldodebug.soar` 源码；若后续按决定清空代码，细节请以备份仓库为准。
- 当前本地开发机关键路径：JDK 8 = `C:\Users\SysCr\.jdks\corretto-1.8.0_472`；`gh` = `C:\Program Files\GitHub CLI\gh.exe`；系统代理 = `127.0.0.1:55179`。
