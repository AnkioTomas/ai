# Ankio AI — 随 AAR 合并到宿主 release 的 R8 规则（consumerProguardFiles）
# 公开 API 保留；序列化模型与自定义 Serializer 保留；实现类允许混淆。

# 注：SourceFile / LineNumberTable / renamesourcefileattribute 须在宿主 proguard 中配置，不可写在 consumer 文件。

# ── 对外公开 API（宿主直接引用）────────────────────────────────────────
-keep class net.ankio.ai.lib.Ai {
    public <methods>;
    public <fields>;
}

-keep interface net.ankio.ai.lib.core.AiDataStore {
    public <methods>;
}

-keep interface net.ankio.ai.lib.core.AiLogger {
    public <methods>;
}

-keep class net.ankio.ai.lib.core.ProviderSettings {
    public <methods>;
    public <fields>;
}

-keep class net.ankio.ai.lib.provider.ProviderDef {
    public <methods>;
    public <fields>;
}

-keep class net.ankio.ai.lib.test.AiTest {
    public <methods>;
}

-keep interface net.ankio.ai.lib.test.AiTestResult { *; }
-keep class net.ankio.ai.lib.test.AiTestResult$* { *; }

-keep class net.ankio.ai.lib.ui.settings.AiSettingsState {
    public <methods>;
    public <fields>;
}

-keep interface net.ankio.ai.lib.ui.settings.AiTestUiState { *; }
-keep class net.ankio.ai.lib.ui.settings.AiTestUiState$* { *; }

# 顶层扩展与 Composable 入口（Kotlin 文件级生成 *Kt）
-keep class net.ankio.ai.lib.core.AiDataStoreExtKt { public static <methods>; }
-keep class net.ankio.ai.lib.core.AiErrorsKt { public static <methods>; }
-keep class net.ankio.ai.lib.ui.settings.AiSettingsScreenKt { public static <methods>; }

# 宿主实现的 DataStore / Logger
-keep class * implements net.ankio.ai.lib.core.AiDataStore { *; }
-keep class * implements net.ankio.ai.lib.core.AiLogger { *; }

# ── kotlinx.serialization（OpenAI / Gemini JSON）──────────────────────
-keep @kotlinx.serialization.Serializable class net.ankio.ai.lib.model.** { *; }

-keep class net.ankio.ai.lib.model.openai.MessageContentSerializer { *; }
-keep class net.ankio.ai.lib.model.openai.MessageContent { *; }
-keep class net.ankio.ai.lib.model.openai.MessageContent$* { *; }

-keepclassmembers class net.ankio.ai.lib.model.** {
    *** Companion;
}

-keepclasseswithmembers class net.ankio.ai.lib.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# ── Kotlin / 协程 ────────────────────────────────────────────────────
-dontwarn kotlin.reflect.jvm.internal.**

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── OkHttp / Okio（依赖自带部分规则，此处补常见告警）────────────────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
