package net.ankio.ai.lib.core

/**
 * 从 [AiDataStore] 各字段组装 [ProviderSettings]。
 *
 * 不做默认值补全；展示或请求前请配合 [ProviderSettings.withProviderDefaults]。
 *
 * @param providerId 目标提供商 id。
 */
suspend fun AiDataStore.loadSettings(providerId: String): ProviderSettings =
    ProviderSettings(
        providerId = providerId,
        apiKey = getApiKey(providerId),
        apiUri = getApiUri(providerId),
        model = getModel(providerId),
        visionEnabled = getVisionEnabled(providerId),
    )

/**
 * 将 [ProviderSettings] 按字段写回 [AiDataStore]。
 *
 * @param settings 待持久化的配置。
 */
suspend fun AiDataStore.saveSettings(settings: ProviderSettings) {
    val id = settings.providerId
    setApiKey(id, settings.apiKey)
    setApiUri(id, settings.apiUri)
    setModel(id, settings.model)
    setVisionEnabled(id, settings.visionEnabled)
}
