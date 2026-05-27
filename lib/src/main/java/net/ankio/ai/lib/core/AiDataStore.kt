package net.ankio.ai.lib.core

/**
 * 配置持久化接口，由宿主实现（SharedPreferences / DataStore / Room 等）。
 * lib 不提供默认实现，且**不得**在实现层对配置做 JSON/序列化整包存储；
 * 应按字段分别读写（见各 `get*` / `set*`）。
 * 切换提供商只改 [setActiveProviderId]，各 providerId 的配置独立保存。
 */
interface AiDataStore {
    suspend fun getActiveProviderId(): String
    suspend fun setActiveProviderId(providerId: String)

    suspend fun getApiKey(providerId: String): String
    suspend fun setApiKey(providerId: String, apiKey: String)

    suspend fun getApiUri(providerId: String): String?
    suspend fun setApiUri(providerId: String, apiUri: String?)

    suspend fun getModel(providerId: String): String?
    suspend fun setModel(providerId: String, model: String?)

    suspend fun getVisionEnabled(providerId: String): Boolean
    suspend fun setVisionEnabled(providerId: String, enabled: Boolean)
}
