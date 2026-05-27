package net.ankio.ai.lib.ui.settings

import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.provider.ProviderDef

data class AiSettingsState(
    val providerId: String,
    val apiKey: String = "",
    val apiUri: String = "",
    val model: String = "",
    val visionEnabled: Boolean = true,
    val testState: AiTestUiState = AiTestUiState.Idle,
) {
    val isTesting: Boolean get() = testState is AiTestUiState.Running

    fun toSettings() = ProviderSettings(
        providerId = providerId,
        apiKey = apiKey.trim(),
        apiUri = apiUri.trim().ifBlank { null },
        model = model.trim().ifBlank { null },
        visionEnabled = visionEnabled,
    )

    /** 表单提交 / 测试 / 拉模型时使用，空白项回落到提供商默认 API、默认模型。 */
    fun toEffectiveSettings(def: ProviderDef) = toSettings().withProviderDefaults(def)

    companion object {
        fun from(def: ProviderDef, settings: ProviderSettings): AiSettingsState {
            val resolved = settings.withProviderDefaults(def)
            return AiSettingsState(
                providerId = resolved.providerId,
                apiKey = resolved.apiKey,
                apiUri = resolved.apiUri.orEmpty(),
                model = resolved.model.orEmpty(),
                visionEnabled = resolved.visionEnabled,
            )
        }
    }
}
