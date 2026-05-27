package net.ankio.ai.lib.core

suspend fun AiDataStore.loadSettings(providerId: String): ProviderSettings =
    ProviderSettings(
        providerId = providerId,
        apiKey = getApiKey(providerId),
        apiUri = getApiUri(providerId),
        model = getModel(providerId),
        visionEnabled = getVisionEnabled(providerId),
    )

suspend fun AiDataStore.saveSettings(settings: ProviderSettings) {
    val id = settings.providerId
    setApiKey(id, settings.apiKey)
    setApiUri(id, settings.apiUri)
    setModel(id, settings.model)
    setVisionEnabled(id, settings.visionEnabled)
}
