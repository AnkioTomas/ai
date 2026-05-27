package net.ankio.ai.lib.ui.settings.preview

import net.ankio.ai.lib.ui.settings.AiSettingsState
import net.ankio.ai.lib.ui.settings.AiTestUiState

internal object AiPreviewSamples {
    val settingsState = AiSettingsState(
        providerId = "deepseek",
        apiKey = "sk-demo",
        model = "deepseek-chat",
        visionEnabled = true,
    )
    val settingsStateSuccess = settingsState.copy(testState = AiTestUiState.Success)
    val settingsStateFailure =
        settingsState.copy(testState = AiTestUiState.Failure("401 Unauthorized"))
}
