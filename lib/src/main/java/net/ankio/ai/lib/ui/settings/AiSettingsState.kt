package net.ankio.ai.lib.ui.settings

import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.provider.ProviderDef

/**
 * AI 设置页 UI 状态（与 [ProviderSettings] 一一对应，外加测试状态）。
 *
 * @param providerId 当前选中的提供商 id。
 * @param apiKey API Key 输入框内容。
 * @param apiUri API 地址输入框内容（可已由默认值填充）。
 * @param model 模型名输入框内容。
 * @param visionEnabled 是否启用视觉识别。
 * @param temperature 采样温度，范围建议 `0.0`～`2.0`。
 * @param proxy 全局 HTTP/SOCKS 代理；空字符串表示直连。
 * @param testState 连接测试结果展示状态。
 */
data class AiSettingsState(
    val providerId: String,
    val apiKey: String = "",
    val apiUri: String = "",
    val model: String = "",
    val visionEnabled: Boolean = true,
    val temperature: Double = ProviderSettings.DEFAULT_TEMPERATURE,
    val proxy: String = "",
    val testState: AiTestUiState = AiTestUiState.Idle,
) {
    /** 是否正在执行连接测试。 */
    val isTesting: Boolean
        get() = testState is AiTestUiState.Running || testState is AiTestUiState.RefreshingModels

    /**
     * 转为 [ProviderSettings]（空白 apiUri/model 存为 `null`）。
     *
     * 不做默认值补全；仅反映表单字面内容。
     */
    fun toSettings() = ProviderSettings(
        providerId = providerId,
        apiKey = apiKey,
        apiUri = apiUri.ifBlank { null },
        model = model.ifBlank { null },
        visionEnabled = visionEnabled,
        temperature = temperature,
    )

    /**
     * 转为用于保存 / 测试 / 拉取模型列表的有效配置。
     *
     * 空白 [apiUri]、[model] 会回落到 [ProviderDef] 默认值。
     */
    fun toEffectiveSettings(def: ProviderDef) = toSettings().withProviderDefaults(def)

    companion object {
        /**
         * 从持久化配置构建 UI 状态，并填充默认 API / 默认模型到输入框。
         *
         * @param def 当前提供商定义。
         * @param settings 自 [net.ankio.ai.lib.Ai.settings] 或存储读取的配置。
         */
        fun from(
            def: ProviderDef,
            settings: ProviderSettings,
            proxy: String = "",
        ): AiSettingsState {
            val resolved = settings.withProviderDefaults(def)
            return AiSettingsState(
                providerId = resolved.providerId,
                apiKey = resolved.apiKey,
                apiUri = resolved.apiUri.orEmpty(),
                model = resolved.model.orEmpty(),
                visionEnabled = resolved.visionEnabled,
                temperature = resolved.temperature,
                proxy = proxy,
            )
        }
    }
}
