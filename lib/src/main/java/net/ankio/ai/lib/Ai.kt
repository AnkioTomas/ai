package net.ankio.ai.lib

import net.ankio.ai.lib.core.AiDataStore
import net.ankio.ai.lib.core.AiLogger
import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.core.loadSettings
import net.ankio.ai.lib.core.runCatchingExceptCancel
import net.ankio.ai.lib.core.saveSettings
import net.ankio.ai.lib.provider.AiCtx
import net.ankio.ai.lib.provider.AiProviders
import net.ankio.ai.lib.provider.ProviderBackend
import net.ankio.ai.lib.provider.ProviderDef
import net.ankio.ai.lib.test.AiTestDemo

/** 默认提供商 id，与 [AiProviders.DEFAULT_ID] 一致。 */
const val AI_DEFAULT_PROVIDER_ID: String = "deepseek"

/**
 * AI 模块对外唯一入口。
 *
 * 负责提供商切换、配置读写、模型列表、对话请求与连接测试。
 * 持久化与日志由宿主通过 [AiDataStore]、[AiLogger] 注入。
 *
 * @param store 配置存储，由宿主实现。
 * @param logger 日志输出，由宿主实现。
 * @param userAgent 写入 HTTP `User-Agent` 的版本片段。
 */
class Ai(
    private val store: AiDataStore,
    private val logger: AiLogger,
    private val userAgent: String = "1",
) {
    /** 内置的全部提供商定义（只读）。 */
    val providers: List<ProviderDef> = AiProviders.all

    /**
     * 当前激活的提供商 id。
     *
     * 若存储中的 id 不在 [providers] 内，回落到 [AiProviders.DEFAULT_ID]。
     */
    suspend fun activeProviderId(): String {
        val id = store.getActiveProviderId()
        return if (providers.any { it.id == id }) id else AiProviders.DEFAULT_ID
    }

    /**
     * 切换当前激活提供商。
     *
     * 仅更新 active id，不修改其它 provider 的已存配置。
     *
     * @param providerId 目标提供商 id，须为 [providers] 中之一。
     */
    suspend fun switchProvider(providerId: String) {
        AiProviders.def(providerId)
        store.setActiveProviderId(providerId)
    }

    /**
     * 读取指定提供商的配置。
     *
     * 未单独保存的 [ProviderSettings.apiUri]、[ProviderSettings.model]
     * 会用该提供商的默认值补全（见 [ProviderSettings.withProviderDefaults]）。
     *
     * @param providerId 提供商 id。
     */
    suspend fun settings(providerId: String): ProviderSettings {
        val def = AiProviders.def(providerId)
        return store.loadSettings(providerId).withProviderDefaults(def)
    }

    /**
     * 持久化指定提供商的配置（按字段写入 [store]）。
     *
     * @param settings 待保存配置，[ProviderSettings.providerId] 决定写入目标。
     */
    suspend fun saveSettings(settings: ProviderSettings) {
        AiProviders.def(settings.providerId)
        store.saveSettings(settings)
    }

    /**
     * 使用给定配置探测连接，不读写 [store]。
     *
     * - 未启用视觉：发送短文本，校验回复含「连通」。
     * - 已启用视觉：附带 [AiTestDemo] 图片，校验模型能识别图中文字。
     *
     * @param settings 含 apiKey、apiUri、model 等；建议经 [ProviderSettings.withProviderDefaults] 补全默认值。
     * @return 成功为 [Result.success]，失败携带错误信息。
     */
    suspend fun testConnection(settings: ProviderSettings): Result<Unit> {
        AiProviders.def(settings.providerId)
        val testSettings = settings.copy(
            temperature = ProviderSettings.TEST_TEMPERATURE,
        )
        val ctx = AiCtx(
            AiProviders.def(settings.providerId),
            testSettings,
            logger,
            userAgent,
        )
        val backend = AiProviders.backend(settings.providerId)
        return if (settings.visionEnabled) {
            testVisionConnection(backend, ctx)
        } else {
            testTextConnection(backend, ctx)
        }
    }

    /** 文本连通性探测：要求模型回复包含「连通」。 */
    private suspend fun testTextConnection(backend: ProviderBackend, ctx: AiCtx): Result<Unit> =
        backend.chat(
            ctx,
            system = "",
            user = "请仅回复一个词：连通",
            image = "",
            onChunk = null,
        ).mapCatching { body ->
            val reply = body.trim()
            if (reply.isEmpty()) error("AI 返回为空")
            if (!reply.contains("连通")) error("未收到连通确认：$reply")
        }

    /** 视觉连通性探测：要求模型能读取测试图中的 AUTO TEST 文字。 */
    private suspend fun testVisionConnection(backend: ProviderBackend, ctx: AiCtx): Result<Unit> =
        backend.chat(
            ctx,
            system = "",
            user = "这是一张连接测试图片，图中文字为「AUTO TEST」。请判断你能否读取图片中的文字。" +
                "若能读取，请回复「可以读取」并简要说明你看到的文字。",
            image = AiTestDemo.IMAGE_BASE64,
            onChunk = null,
        ).mapCatching { body ->
            val reply = body.trim()
            if (reply.isEmpty()) error("AI 返回为空")
            val canRead = reply.contains("可以读取") ||
                listOf("AUTO", "TEST").all { reply.contains(it, ignoreCase = true) }
            if (!canRead) error("无法读取测试图片：$reply")
        }

    /**
     * 按给定配置拉取模型列表，不读写 [store]。
     *
     * @param settings 须含有效 [ProviderSettings.apiKey]。
     */
    suspend fun listModels(settings: ProviderSettings): Result<List<String>> {
        AiProviders.def(settings.providerId)
        val ctx = AiCtx(
            AiProviders.def(settings.providerId),
            settings,
            logger,
            userAgent,
        )
        return runCatchingExceptCancel {
            AiProviders.backend(settings.providerId).models(ctx)
        }
    }

    /**
     * 拉取模型列表（使用已保存配置）。
     *
     * @param providerId 为 `null` 时使用 [activeProviderId]。
     */
    suspend fun models(providerId: String? = null): List<String> =
        backend(providerId).models(ctx(providerId))

    /**
     * 非流式对话请求。
     *
     * 采样温度来自该提供商已保存的 [ProviderSettings]。
     *
     * @param system 系统提示词，可为空。
     * @param user 用户消息。
     * @param image Base64 或 `data:image/...` URL；非空时须已启用视觉。
     * @param providerId 为 `null` 时使用当前激活提供商。
     * @return 完整回复文本。
     */
    suspend fun request(
        system: String,
        user: String,
        image: String = "",
        providerId: String? = null,
    ): Result<String> {
        val ctx = ctx(providerId)
        return backend(providerId).chat(ctx, system, user, image, onChunk = null)
    }

    /**
     * 流式对话请求；通过 [onChunk] 逐段接收增量文本。
     *
     * @param onChunk 每收到一段 delta 文本时回调；流式模式下无整体返回值。
     */
    suspend fun requestStream(
        system: String,
        user: String,
        image: String = "",
        providerId: String? = null,
        onChunk: (String) -> Unit,
    ) {
        val ctx = ctx(providerId)
        backend(providerId).chat(ctx, system, user, image, onChunk)
    }

    /** 组装单次请求的上下文（含解析后的 apiUri、model）。 */
    private suspend fun ctx(providerId: String?): AiCtx {
        val id = providerId ?: activeProviderId()
        return AiCtx(AiProviders.def(id), settings(id), logger, userAgent)
    }

    /** 解析目标提供商的后端实现。 */
    private suspend fun backend(providerId: String?) =
        AiProviders.backend(providerId ?: activeProviderId())

}
