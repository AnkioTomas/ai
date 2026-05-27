package net.ankio.ai.lib.ui

import net.ankio.ai.lib.ProviderDef
import net.ankio.ai.lib.ProviderSettings

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

    companion object {
        fun from(def: ProviderDef, settings: ProviderSettings) = AiSettingsState(
            providerId = settings.providerId,
            apiKey = settings.apiKey,
            apiUri = settings.apiUri.orEmpty(),
            model = settings.model.orEmpty(),
            visionEnabled = settings.visionEnabled,
        )
    }
}
