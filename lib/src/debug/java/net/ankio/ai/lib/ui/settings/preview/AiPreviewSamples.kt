package net.ankio.ai.lib.ui.settings.preview

import net.ankio.ai.lib.ui.settings.AiSettingsState
import net.ankio.ai.lib.ui.settings.AiTestUiState

/** Compose Preview 用的静态 [AiSettingsState] 样本。 */
internal object AiPreviewSamples {
    /** 默认 DeepSeek 配置样本。 */
    val settingsState = AiSettingsState(
        providerId = "deepseek",
        apiKey = "sk-demo",
        model = "deepseek-chat",
        visionEnabled = true,
    )

    /** 测试成功状态样本。 */
    val settingsStateSuccess = settingsState.copy(testState = AiTestUiState.Success)

    /** 测试失败状态样本。 */
    val settingsStateFailure =
        settingsState.copy(testState = AiTestUiState.Failure("401 Unauthorized"))
}
