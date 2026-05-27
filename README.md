# Ankio AI

Android AI 客户端库：多提供商（OpenAI 兼容 + Gemini）、Compose 配置页、连接测试、模型列表刷新、全局代理。

[![JitPack](https://jitpack.io/v/AnkioTomas/ai.svg)](https://jitpack.io/#AnkioTomas/ai)

## 环境要求

| 项       | 值                                               |
|---------|-------------------------------------------------|
| minSdk  | 30                                              |
| Java    | 11（JitPack 构建 JDK 17，但 bytecode 目标仍为 11）        |
| Compose | BOM 2026.05+                                    |
| Theme   | `com.github.AnkioTomas:theme`（`lib` 以 `api` 传递） |

## 接入

### 1. 添加 JitPack 仓库

`settings.gradle.kts`：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

### 2. 添加依赖

```kotlin
dependencies {
    implementation("com.github.AnkioTomas:ai:1.0.0")
}
```

将 `1.0.0` 替换为 [JitPack](https://jitpack.io/#AnkioTomas/ai) 上对应 **Git Tag**。

## 使用说明（面向开发）

### 1. 依赖注入：Ai 不做持久化与日志

`net.ankio.ai.lib.Ai` 负责提供商切换、读取/保存配置、拉取模型、发起请求与连接测试。它不会自行实现配置存储与日志系统，因此宿主必须提供：

- `net.ankio.ai.lib.core.AiDataStore`
- `net.ankio.ai.lib.core.AiLogger`

`AiDataStore` 需要实现的方法（全部为 `suspend`）：

- provider 切换：`getActiveProviderId()` / `setActiveProviderId(providerId)`
- 每个 provider 的连接参数：
    - `getApiKey(providerId)` / `setApiKey(providerId, apiKey)`
    - `getApiUri(providerId)` / `setApiUri(providerId, apiUri)`
    - `getModel(providerId)` / `setModel(providerId, model)`
    - `getVisionEnabled(providerId)` / `setVisionEnabled(providerId, enabled)`
    - `getTemperature(providerId)` / `setTemperature(providerId, temperature)`
- 全局网络代理（对所有 provider 生效）：
    - `getProxy()` / `setProxy(proxy)`

`AiLogger` 需要实现：

- `debug(tag, message)`
- `error(tag, message, throwable?)`

### 2. 如需使用配置页 UI

#### Theme 初始化

复用 `net.ankio.ai.lib.ui.settings.AiSettingsScreen` 前，需初始化 Theme（与 demo 一致）：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        net.ankio.theme.ThemeSettings.init(this)
        net.ankio.theme.toast.ThemeToast.init(this)
    }
}
```

#### 嵌入 `AiSettingsScreen`

配置页**不**自行实现持久化，也**不**要求宿主维护表单状态。宿主只需构造已注入 `AiDataStore` 的 `Ai`
，再把页面放进 Compose 树即可；保存、切换提供商、拉模型、连接测试等均由页面内部通过 `ai` 读写存储。

对外参数：

| 参数                   | 说明                                       |
|----------------------|------------------------------------------|
| `ai`                 | 已注入 `AiDataStore` 与 `AiLogger` 的 `Ai` 实例 |
| `providers`          | 通常为 `ai.providers`                       |
| `modifier`           | 布局修饰；**滚动由宿主提供**（见下）                     |
| `onOpenCreateKeyUri` | 可选；提供商配置了申请 Key 外链时，点击按钮回调 URI，由宿主打开浏览器等 |

页面**不包含**内部 `verticalScroll`。内容超出时，在宿主侧为 `modifier` 加上滚动，例如：

```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.ui.settings.AiSettingsScreen

@Composable
fun SettingsTab(ai: Ai, onOpenUri: (String) -> Unit) {
    AiSettingsScreen(
        ai = ai,
        providers = ai.providers,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        onOpenCreateKeyUri = onOpenUri,
    )
}
```

若配置页嵌在**已有**可滚动容器（如 `LazyColumn`、`NestedScroll`）内，则不要再套一层 `verticalScroll`
，只传布局相关的 `modifier` 即可。

申请 Key 外链示例（与 demo 一致）：

```kotlin
onOpenCreateKeyUri = { uri ->
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
}
```

`AiDataStore` 参考实现见 demo：`app/src/main/java/net/ankio/ai/demo/store/AiSettingsStore.kt`。

### 3. 核心用法：读取配置、拉模型、发起请求

创建 `Ai`：

```kotlin
val store: net.ankio.ai.lib.core.AiDataStore = ...
val logger: net.ankio.ai.lib.core.AiLogger = ...
val ai = net.ankio.ai.lib.Ai(store, logger, userAgent = "1")
```

典型流程：

1. 切换提供商：`ai.switchProvider(providerId)`
2. 读取当前/指定提供商的有效配置：`val settings = ai.settings(providerId)`
3. 拉取模型列表：`val result = ai.listModels(settings)`（返回 `Result<List<String>>`）
4. 连接测试（不会写入存储）：`val ok = ai.testConnection(settings)`
5. 发起对话：
    - 非流式：`ai.request(system, user, image = "", providerId = null)`
    - 流式：`ai.requestStream(system, user, image = "", providerId = null) { delta -> ... }`

流式回调 `onChunk` 由 lib 的网络层发起，是否在 IO 线程运行由实现决定；更新 UI 时请自行切回主线程。

### 4. 最小请求示例（宿主侧）

下面以“非流式请求”为例。`AiDataStore` 与 `AiLogger` 的实现由你在宿主侧提供（demo 内已有一份参考实现）。

```kotlin
val providerId = ai.activeProviderId() // 或你自己选定 providerId
val settings = ai.settings(providerId)

ai.request(
    system = "你是一个助手",
    user = "你好，回答一句话即可",
    image = "",
    providerId = providerId,
).onSuccess { reply ->
    // reply: String
}.onFailure { t ->
    // t: Throwable（例如网络/鉴权/解析失败）
}
```

流式请求：

```kotlin
ai.requestStream(
    system = "你是一个助手",
    user = "给我一段逐步输出的回复",
    image = "",
    providerId = providerId,
) { delta ->
    // delta: 每个增量片段
}
```

## R8 混淆

### 库（`lib`）

- 通过 `consumerProguardFiles("consumer-rules.pro")` 将规则随 AAR 合并到宿主 **release** 构建。
- 保留对外 API（`Ai`、`AiDataStore`、`ProviderSettings`、`AiSettingsScreen` 等）、`kotlinx.serialization`
  模型与 `MessageContentSerializer`；内部 Backend 可在宿主 R8 中继续收缩/混淆。
- **不要**在 `consumer-rules.pro` 里写 `-renamesourcefileattribute` 等全局选项（AGP 会拒绝）；行号映射请在宿主
  `proguard-rules.pro` 配置。

### 宿主 App

release 建议开启：

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }
}
```

宿主 `proguard-rules.pro` 示例（堆栈可读 + 自有实现类）：

```proguard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class * implements net.ankio.ai.lib.core.AiDataStore { *; }
-keep class * implements net.ankio.ai.lib.core.AiLogger { *; }
```

Demo 已启用 R8，可参考 `app/proguard-rules.pro`。映射文件：`app/build/outputs/mapping/release/`。

验证：

```bash
./gradlew :app:assembleRelease
```

## 发布（JitPack）

本仓库通过 `jitpack.yml` 指定 JitPack 构建步骤：

```yaml
install:
  - ./gradlew :lib:publishToMavenLocal -x test -x lint --stacktrace
```

发布新版本时：

1. 更新 `gradle.properties` 中的 `VERSION_NAME`
2. 打同名 tag 并 push

```bash
git tag -a 1.0.1 -m "release 1.0.1"
git push origin 1.0.1
```

## License

Apache License 2.0

