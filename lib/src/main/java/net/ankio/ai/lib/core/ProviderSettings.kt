package net.ankio.ai.lib.core

import kotlinx.serialization.Serializable
import net.ankio.ai.lib.provider.ProviderDef

/**
 * 某一 AI 提供商的完整连接配置。
 *
 * 用于持久化、表单状态与 HTTP 请求；[apiUri]、[model] 可为 `null` 表示使用 [ProviderDef] 默认值。
 */
@Serializable
data class ProviderSettings(
    /** 提供商 id，对应 [ProviderDef.id]。 */
    val providerId: String,
    /** API Key / Token。 */
    val apiKey: String = "",
    /** 自定义 API 根地址；`null` 或空白时使用 [ProviderDef.defaultApiUri]。 */
    val apiUri: String? = null,
    /** 自定义模型名；`null` 或空白时使用 [ProviderDef.defaultModel]。 */
    val model: String? = null,
    /** 是否允许附带图片进行视觉识别。 */
    val visionEnabled: Boolean = true,
    /** 采样温度；越高越发散，常见范围 `0.0`～`2.0`。 */
    val temperature: Double = DEFAULT_TEMPERATURE,
) {
    /**
     * 用提供商元数据补全未保存的 [apiUri]、[model]。
     *
     * 用于 UI 展示、保存前校验及实际请求（与 [AiCtx] 内逻辑一致）。
     *
     * @param def 目标提供商定义。
     */
    fun withProviderDefaults(def: ProviderDef): ProviderSettings = copy(
        apiUri = apiUri?.takeIf { it.isNotBlank() } ?: def.defaultApiUri.takeIf { it.isNotBlank() },
        model = model?.takeIf { it.isNotBlank() } ?: def.defaultModel.takeIf { it.isNotBlank() },
    )

    companion object {
        /** 常规对话默认温度。 */
        const val DEFAULT_TEMPERATURE: Double = 0.7

        /** 连接测试等场景：尽量确定性输出。 */
        const val TEST_TEMPERATURE: Double = 0.0
    }
}
