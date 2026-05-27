package net.ankio.ai.lib.core

/**
 * 配置持久化接口，由宿主实现（SharedPreferences / DataStore / Room 等）。
 *
 * lib 不提供默认实现，且**不得**在实现层对配置做 JSON/序列化整包存储；
 * 应按字段分别读写（见各 `get*` / `set*`）。
 * 切换提供商只改 [setActiveProviderId]，各 [providerId] 的配置独立保存。
 */
interface AiDataStore {
    /** 当前激活的提供商 id。 */
    suspend fun getActiveProviderId(): String

    /** 设置当前激活的提供商 id（不改动其它字段）。 */
    suspend fun setActiveProviderId(providerId: String)

    /** 读取指定提供商的 API Key；未配置时返回空字符串。 */
    suspend fun getApiKey(providerId: String): String

    /** 保存指定提供商的 API Key。 */
    suspend fun setApiKey(providerId: String, apiKey: String)

    /** 读取指定提供商的 API 地址；`null` 表示未单独配置（运行时用默认值）。 */
    suspend fun getApiUri(providerId: String): String?

    /** 保存指定提供商的 API 地址；传 `null` 表示清除自定义地址。 */
    suspend fun setApiUri(providerId: String, apiUri: String?)

    /** 读取指定提供商的模型名；`null` 表示未单独配置。 */
    suspend fun getModel(providerId: String): String?

    /** 保存指定提供商的模型名；传 `null` 表示清除自定义模型。 */
    suspend fun setModel(providerId: String, model: String?)

    /** 读取指定提供商是否启用视觉识别。 */
    suspend fun getVisionEnabled(providerId: String): Boolean

    /** 设置指定提供商是否启用视觉识别。 */
    suspend fun setVisionEnabled(providerId: String, enabled: Boolean)

    /** 读取采样温度；未配置时由实现返回 [ProviderSettings.DEFAULT_TEMPERATURE]。 */
    suspend fun getTemperature(providerId: String): Double

    /** 保存采样温度。 */
    suspend fun setTemperature(providerId: String, temperature: Double)
}
