package net.ankio.ai.lib

/**
 * 配置持久化，宿主实现（DataStore / Room 等）。
 * 切换提供商只改 [setActiveProviderId]，其它 id 的配置通过 [saveSettings] 独立保存。
 */
interface AiDataStore {
    suspend fun getActiveProviderId(): String
    suspend fun setActiveProviderId(providerId: String)
    suspend fun getSettings(providerId: String): ProviderSettings?
    suspend fun saveSettings(settings: ProviderSettings)
}
