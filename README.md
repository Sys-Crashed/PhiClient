# PhiClient

Minecraft 1.8.9 客户端。采用 **tweaker 注入式**架构（自定义 `ITweaker` + SpongePowered Mixin），
不是标准 Forge mod，没有 `@Mod` 注解。

- 根包：`org.dpdns.syscrashed.phiclient`
- 启动入口：`org.dpdns.syscrashed.phiclient.injection.mixin.PhiTweaker`
- 业务初始化：`MixinMinecraft#startGame` → `PhiClient.getInstance().start()`
- Mixin 配置：`src/main/resources/mixins.phiclient.json`

## 构建环境（硬性要求）

- **Java 8**（必须）。Java 9+ 会让 Gradle 4.7 在启动期崩溃。
- Gradle 4.7 + ForgeGradle 2.1-SNAPSHOT + MixinGradle 0.6-SNAPSHOT。**不要升级。**

## How to setup project

### Eclipse

```
gradlew setupdecompworkspace
```

```
gradlew eclipse
```

### IntelliJ

```
gradlew setupdecompworkspace
```

```
gradlew genIntellijRuns
```

## OptiFine

PhiClient 不依赖 Forge，**也不需要用户手动安装 OptiFine** —— OptiFine 随 `PhiClient-Release.jar`
一起发布，启动时由 PhiClient 自己加载（`OptiFine_1.8.9_HD_U_M5.jar`）：

- `PhiTweaker#injectIntoClassLoader` 会**先于 Mixin** 注册 `optifine.OptiFineClassTransformer`。
  顺序是关键：OptiFine 的 `patch/*.xdelta` 是对"未修改过的原版字节"做的增量补丁（带 MD5 校验），
  先被 Mixin 改过就可能对不上。
- 所以只要把 OptiFine 的 jar 与 `PhiClient-Release.jar` 一起放进启动 classpath 即可，**不需要 Forge**，
  也不需要额外传 `--tweakClass optifine.OptiFineTweaker`（传了也没问题，会被检测为已注册并跳过）。
- 启动日志会打印 `OptiFine detected: OptiFine_1.8.9_HD_U_M5`，没适配到则打印 `OptiFine not detected.`。
- 运行时查询/开关走 `compat.optifine.OptiFineCompat`（纯反射，`isLoaded` / `getVersion` /
  `isFastRenderEnabled` / `isShadersEnabled` / `setFastRenderEnabled`）。

开发环境（`gradlew runClient`）里**一定检测不到 OptiFine**：dev 用的是 SRG 命名的 MC，
而 OptiFine 的补丁按混淆（notch）类名匹配。想验证必须拿 `gradlew build` 产出的**重混淆** jar
（`reobfJar` 任务会把 SRG 名映射回混淆名）在真实 1.8.9 客户端上跑。

### 根因：OptiFine 必须"看得见"自己的 jar（2026-10-11 定位并修复）

OptiFine 用 `getProtectionDomain().getCodeSource().getLocation().toURI()` 定位自己的 jar。
而 `LaunchClassLoader#findClass` 造 CodeSource 时用的是 `findResource()` 返回的 **jar 条目 URL**
（`jar:file:/…/OptiFine.jar!/optifine/OptiFineClassTransformer.class`），
`new File(uri)` 必抛 `IllegalArgumentException: URI is not hierarchical`，随即打印
`*** Can not find the OptiFine JAR in the classpath ***` 并彻底失效。
标准 `launchwrapper-1.11` 与 OptiFine 自带的 `launchwrapper-of-2.2.jar` 行为完全一致（已实测对比字节码）。

官方之所以能用，是因为 `Launch` 加载 tweaker 时做了这件事（launchwrapper 1.11 `Launch.java:97`）：

```java
// Ensure we allow the tweak class to load with the parent classloader
classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
```

传 `--tweakClass optifine.OptiFineTweaker` 时，被排除的前缀正是 `optifine`，
于是 `optifine.*` 改由**父加载器（系统 classpath）**加载 —— 普通 `URLClassLoader` 造的 CodeSource
是 `file:/…/OptiFine.jar`，定位成立。

所以 PhiClient 从自己的 tweaker 接管 OptiFine 时，必须同时满足两点：

1. OptiFine 的 jar 在**系统 classpath**（`-cp`）上，而不只是 `addURL` 给 LaunchClassLoader；
2. `OptiFineCompat#hook` 里补上 `classLoader.addClassLoaderExclusion("optifine")`。
   只排除 `optifine`；**不要**排除 `net.optifine` —— 那个包要留给以后的 `@Pseudo` mixin。

`OptiFineProvisioner` 负责第 1 点：classpath 上没有时，把内置的 jar 落到 `~/.phiclient/optifine/`
并反射 `URLClassLoader#addURL` 挂到**系统类加载器**（`net.optifine.*` 那 408 个类仍需
LaunchClassLoader 能找到，所以也给它挂一份）。开发环境（SRG）自动跳过。

### 为什么 `gradlew runClient` 里看不到 OptiFine 效果

dev 跑的是 SRG 命名的 MC，而 OptiFine 的 `patch/*.xdelta` 按混淆（notch）类名匹配，
对不上会静默跳过 —— 这是**命名**问题，不是加载问题（第 2 点已由上面的排除机制解决）。
要验证 OptiFine 必须跑真实混淆客户端：用下面的 `gradlew runRelease`，或双击发布包里的 `start.bat`。

另注：`libs/` 会被递归展开进 classpath，在里面放解压目录会变成 2746 条目 / 47 万字符 →
Windows `CreateProcess error=206`。别往 `libs/` 里塞目录。

若确实要把 OptiFine 直接打进发布 jar 里（属于二次分发，请先自行确认授权是否允许）：

```
gradlew build                                  # 默认：根目录有 OptiFine jar 就内置，用户无需安装
gradlew build -PbundleOptifine=false           # 不内置，退回"用户自备 OptiFine 放 classpath"模式
gradlew build -PoptifineJar=C:\path\to\OptiFine_1.8.9_HD_U_M5.jar
```

OptiFine 是以**嵌套 jar 资源**（`assets/phiclient/optifine/`）放进去的，所以 `reobfJar` 不会碰它
（`reobfJar` 只重映射 .class 条目）。运行时若系统 classpath 上没有 OptiFine，
`OptiFineProvisioner` 会把它取出来挂到系统类加载器（jar 文件形式，配合上面的排除机制才能定位自身）。
开发环境会自动跳过这一步（SRG 环境跑了也没用）。

## 自启动发布包（不依赖任何第三方启动器）

与上游 SoarClient 的发布包同构：把**真实混淆的 1.8.9 客户端** + 依赖库 + 原生库 + 资源 + OptiFine +
**自带 Java 8 运行时** + `start.bat` 组装成一个目录 / zip。解压双击 `start.bat` 即玩：
玩家不需要装 OptiFine，不需要装 Java，也不需要任何第三方启动器。

```
gradlew makeRelease                    # 产出 build/release/PhiClient/
gradlew packageRelease                 # 再打包成 build/release/PhiClient-Release.zip
gradlew runRelease                     # 直接用真实混淆客户端启动（自测用）
gradlew makeRelease -PskipAssets       # 跳过资源复制（约 110MB），加快迭代
gradlew makeRelease -PskipJava         # 不自带运行时（脚本会退回在本机找 Java 8）
```

产物结构（对齐 `SoarClient.zip`）：

```
PhiClient/
  start.bat                 双击即玩
  java/                     自带 Java 8 运行时（bin/javaw.exe）
  minecraft/1.8.9.jar       官方原版客户端（混淆）
  minecraft/PhiClient.jar   reobf 之后的 PhiClient
  libraries/                1.8.9 全部依赖 + OptiFine jar
  natives/                  已解压好的 Windows 原生库（lwjgl / jinput / OpenAL…）
  assets/                   资源索引与对象（取自 Gradle 缓存）
  game/                     游戏目录（首次启动自动创建）
```

`start.bat` 的几个要点（源文件在仓库 `launcher/start.bat`，Gradle 只负责复制）：

- **顶部 `set DEBUG=false`**：改成 `true` 会用 `java.exe` 带控制台窗口启动并在退出时 `pause`，
  方便看报错；默认 `false` 用 `javaw.exe` 无控制台启动并立即关掉黑框。
- **Java 8 是硬要求**：Java 9+ 的系统类加载器不再是 `URLClassLoader`，launchwrapper 直接抛
  `ClassCastException: …AppClassLoader cannot be cast to java.net.URLClassLoader`。
  优先用自带的 `java\bin\javaw.exe`；没有时才按 `JAVA_HOME` → PATH → 注册表 → 常见安装目录
  （含 `%USERPROFILE%\.jdks\*`）依次查找，并逐个校验版本是否为 1.8。
- **离线身份**：`--username "PhiClient User" --uuid 0 --accessToken 0`，不登录、不联网验证。
- `set MEMORY=2` 是 `-Xmx/-Xms` 的 GB 数，机器内存大可以自己调高。
- classpath 由 `libraries\*.jar` 拼出（`libraries/` 里含 OptiFine），再追加 `minecraft/` 下的两个 jar。
- 脚本一律写 **ASCII**，且必须是 **CRLF**（Gradle 复制时会强制转换）：cmd 对纯 LF 的批处理会解析错乱，
  中文注释在 GBK 控制台下还会把 `rem` 行变成可执行命令。

## How to build

```
gradlew build
```

## How to launch

```
gradlew runClient     # 开发环境（SRG 名）：调试用，看不到 OptiFine 效果
gradlew runRelease    # 真实混淆客户端 + OptiFine：验证发布效果的推荐方式
```
