package net.ankio.ai.lib.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.AiProviders
import net.ankio.ai.lib.InMemoryAiDataStore
import net.ankio.ai.lib.ui.AiSettingsScreen
import net.ankio.theme.PreviewAllScreen
import net.ankio.theme.PreviewAllThemes
import net.ankio.theme.ThemePreviewConfig
import net.ankio.theme.ThemePreviewParameterProvider

@PreviewAllScreen
@Composable
private fun AiSettingsScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) config: ThemePreviewConfig,
) {
    PreviewAllThemes(config) {
        AiSettingsScreen(
            ai = Ai(InMemoryAiDataStore()),
            providers = AiProviders.all,
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
