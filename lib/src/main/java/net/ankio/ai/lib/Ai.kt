package net.ankio.ai.lib

/** 默认提供商 id，与 [AiProviders.DEFAULT_ID] 一致。 */
const val AI_DEFAULT_PROVIDER_ID: String = "deepseek"

/**
 * AI 模块唯一入口。
 */
class Ai(
    private val store: AiDataStore,
    private val logger: AiLogger = NoOpAiLogger,
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

    /** 用指定配置探测连接（拉取模型列表），不读写 DataStore。 */
    suspend fun testConnection(settings: ProviderSettings): Result<Unit> =
        listModels(settings).map { }

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

/** 测试 / Demo 用内存存储 */
class InMemoryAiDataStore(
    initialId: String = AiProviders.DEFAULT_ID,
) : AiDataStore {
    private var active = initialId
    private val map = mutableMapOf<String, ProviderSettings>()

    override suspend fun getActiveProviderId() = active
    override suspend fun setActiveProviderId(providerId: String) { active = providerId }
    override suspend fun getSettings(providerId: String) = map[providerId]
    override suspend fun saveSettings(settings: ProviderSettings) { map[settings.providerId] = settings }
}
