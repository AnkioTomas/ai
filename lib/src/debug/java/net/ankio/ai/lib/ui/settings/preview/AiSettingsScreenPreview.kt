package net.ankio.ai.lib.ui.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.core.AiLogger
import net.ankio.ai.lib.store.InMemoryAiDataStore
import net.ankio.ai.lib.ui.settings.AiSettingsScreen
import net.ankio.theme.PreviewAllScreen
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

/** [AiSettingsScreen] 多主题 Preview。 */
@PreviewAllScreen
@Composable
private fun AiSettingsScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        val ai = Ai(InMemoryAiDataStore(), PreviewAiLogger)
        AiSettingsScreen(
            ai = ai,
            providers = ai.providers,
            state = AiPreviewSamples.settingsState,
            onProviderChange = {},
            onApiKeyChange = {},
            onApiUriChange = {},
            onModelChange = {},
            onVisionEnabledChange = {},
            onSave = {},
            onTestStateChange = {},
        )
    }
}

/** Preview 用空日志实现。 */
private object PreviewAiLogger : AiLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}
