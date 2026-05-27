package net.ankio.ai.lib.store

import net.ankio.ai.lib.AI_DEFAULT_PROVIDER_ID
import net.ankio.ai.lib.core.AiDataStore
import net.ankio.ai.lib.core.ProviderSettings

/** Preview / 单元测试用内存 [AiDataStore]，不随 release AAR 发布。 */
internal class InMemoryAiDataStore(
    private var activeProviderId: String = AI_DEFAULT_PROVIDER_ID,
    private val settingsByProvider: MutableMap<String, ProviderSettings> = mutableMapOf(),
) : AiDataStore {

    override suspend fun getActiveProviderId(): String = activeProviderId

    override suspend fun setActiveProviderId(providerId: String) {
        activeProviderId = providerId
    }

    override suspend fun getApiKey(providerId: String): String =
        settingsByProvider[providerId]?.apiKey ?: ""

    override suspend fun setApiKey(providerId: String, apiKey: String) {
        update(providerId) { it.copy(apiKey = apiKey) }
    }

    override suspend fun getApiUri(providerId: String): String? =
        settingsByProvider[providerId]?.apiUri

    override suspend fun setApiUri(providerId: String, apiUri: String?) {
        update(providerId) { it.copy(apiUri = apiUri) }
    }

    override suspend fun getModel(providerId: String): String? =
        settingsByProvider[providerId]?.model

    override suspend fun setModel(providerId: String, model: String?) {
        update(providerId) { it.copy(model = model) }
    }

    override suspend fun getVisionEnabled(providerId: String): Boolean =
        settingsByProvider[providerId]?.visionEnabled ?: true

    override suspend fun setVisionEnabled(providerId: String, enabled: Boolean) {
        update(providerId) { it.copy(visionEnabled = enabled) }
    }

    private fun update(providerId: String, transform: (ProviderSettings) -> ProviderSettings) {
        val current = settingsByProvider[providerId] ?: ProviderSettings(providerId = providerId)
        settingsByProvider[providerId] = transform(current)
    }
}
