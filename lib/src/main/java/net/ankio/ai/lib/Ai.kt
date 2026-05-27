package net.ankio.ai.lib

import net.ankio.ai.lib.core.AiDataStore
import net.ankio.ai.lib.core.AiLogger
import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.core.runCatchingExceptCancel
import net.ankio.ai.lib.provider.AiCtx
import net.ankio.ai.lib.provider.AiProviders
import net.ankio.ai.lib.provider.ProviderBackend
import net.ankio.ai.lib.provider.ProviderDef
import net.ankio.ai.lib.test.AiTestDemo

/** 默认提供商 id，与 [AiProviders.DEFAULT_ID] 一致。 */
const val AI_DEFAULT_PROVIDER_ID: String = "deepseek"

/**
 * AI 模块唯一入口。
 */
class Ai(
    private val store: AiDataStore,
    private val logger: AiLogger,
    private val userAgent: String = "1",
) {
    val providers: List<ProviderDef> = AiProviders.all

    suspend fun activeProviderId(): String {
        val id = store.getActiveProviderId()
        return if (providers.any { it.id == id }) id else AiProviders.DEFAULT_ID
    }

    /** 只改当前提供商 id，不碰其它配置 */
    suspend fun switchProvider(providerId: String) {
        AiProviders.def(providerId)
        store.setActiveProviderId(providerId)
    }

    suspend fun settings(providerId: String): ProviderSettings {
        AiProviders.def(providerId)
        return store.getSettings(providerId) ?: ProviderSettings(providerId = providerId)
    }

    suspend fun saveSettings(settings: ProviderSettings) {
        AiProviders.def(settings.providerId)
        store.saveSettings(settings)
    }

    /** 用指定配置探测连接：未启用视觉时验证文本连通；启用视觉时用 demo 图验证可读性。不读写 DataStore。 */
    suspend fun testConnection(settings: ProviderSettings): Result<Unit> {
        AiProviders.def(settings.providerId)
        val ctx = AiCtx(
            AiProviders.def(settings.providerId),
            settings,
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

    /** 用当前表单配置拉取模型列表，不读写 DataStore。 */
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

    suspend fun models(providerId: String? = null): List<String> =
        backend(providerId).models(ctx(providerId))

    suspend fun request(
        system: String,
        user: String,
        image: String = "",
        providerId: String? = null,
    ): Result<String> {
        val ctx = ctx(providerId)
        checkVision(ctx, image)
        return backend(providerId).chat(ctx, system, user, image, onChunk = null)
    }

    suspend fun requestStream(
        system: String,
        user: String,
        image: String = "",
        providerId: String? = null,
        onChunk: (String) -> Unit,
    ) {
        val ctx = ctx(providerId)
        checkVision(ctx, image)
        backend(providerId).chat(ctx, system, user, image, onChunk)
    }

    private suspend fun ctx(providerId: String?): AiCtx {
        val id = providerId ?: activeProviderId()
        return AiCtx(AiProviders.def(id), settings(id), logger, userAgent)
    }

    private suspend fun backend(providerId: String?) =
        AiProviders.backend(providerId ?: activeProviderId())

    private fun checkVision(ctx: AiCtx, image: String) {
        if (image.isNotBlank() && !ctx.visionEnabled) {
            error("提供商「${ctx.def.displayName}」未启用视觉识别")
        }
    }
}
