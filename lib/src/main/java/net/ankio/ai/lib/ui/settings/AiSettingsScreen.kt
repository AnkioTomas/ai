package net.ankio.ai.lib.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.R
import net.ankio.ai.lib.provider.ProviderDef
import net.ankio.ai.lib.test.AiTest
import net.ankio.ai.lib.test.AiTestResult
import net.ankio.theme.AnkioTheme
import net.ankio.theme.compat.ThemeCard
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeIconButton
import net.ankio.theme.compat.ThemeLinearProgressIndicator
import net.ankio.theme.compat.ThemePrimaryButton
import net.ankio.theme.compat.ThemeSecondaryButton
import net.ankio.theme.compat.ThemeText
import net.ankio.theme.settings.SettingCardPosition
import net.ankio.theme.settings.SettingInputMode
import net.ankio.theme.settings.ThemeSectionHeader
import net.ankio.theme.settings.ThemeSettingDropdown
import net.ankio.theme.settings.ThemeSettingSwitch
import net.ankio.theme.settings.ThemeSettingTextField

/**
 * AI 配置页：Provider 下拉；API / Key / Model 为 [ThemeSettingTextField]；
 * Key 支持明文切换；Model 通过 [SettingFieldListPopup] 选择。
 */
@Composable
fun AiSettingsScreen(
    ai: Ai,
    providers: List<ProviderDef>,
    state: AiSettingsState,
    onProviderChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiUriChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onVisionEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTestStateChange: (AiTestUiState) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCreateKeyUri: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val def = providers.firstOrNull { it.id == state.providerId } ?: providers.first()
    val providerNames = providers.map { it.displayName }
    val selectedIndex = providers.indexOfFirst { it.id == state.providerId }.coerceAtLeast(0)

    var modelItems by remember(state.providerId) { mutableStateOf<List<String>>(emptyList()) }
    var isRefreshingModels by remember { mutableStateOf(false) }
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }

    val invalidMessage = stringResource(R.string.ai_config_invalid)
    val testingMessage = stringResource(
        if (state.visionEnabled) R.string.ai_test_running_vision else R.string.ai_test_running,
    )
    val successMessage = stringResource(
        if (state.visionEnabled) R.string.ai_test_success_vision else R.string.ai_test_success,
    )
    val failedTemplate = stringResource(R.string.ai_test_failed)
    val savedMessage = stringResource(R.string.ai_saved)
    val refreshModelsMessage = stringResource(R.string.ai_refresh_models)
    val refreshModelsFailedTemplate = stringResource(R.string.ai_refresh_models_failed)
    val showKeyLabel = stringResource(R.string.ai_show_api_key)
    val hideKeyLabel = stringResource(R.string.ai_hide_api_key)

    val modelPopupOptions = remember(modelItems, state.model) {
        buildList {
            if (state.model.isNotBlank()) add(state.model)
            addAll(modelItems.filter { it.isNotBlank() && it != state.model })
        }
    }

    fun refreshModels() {
        val settings = state.toEffectiveSettings(def)
        if (settings.apiKey.isBlank()) {
            onTestStateChange(AiTestUiState.Failure(invalidMessage))
            return
        }
        scope.launch {
            isRefreshingModels = true
            ai.listModels(settings)
                .onSuccess { models ->
                    modelItems = models
                    if (models.isNotEmpty() && state.model !in models) {
                        onModelChange(
                            def.defaultModel.takeIf { it in models } ?: models.first(),
                        )
                    }
                }
                .onFailure { error ->
                    val detail = error.message.orEmpty().ifBlank { "unknown" }
                    onTestStateChange(
                        AiTestUiState.Failure(refreshModelsFailedTemplate.format(detail)),
                    )
                }
            isRefreshingModels = false
        }
    }

    LaunchedEffect(state.providerId) {
        modelItems = emptyList()
        apiKeyVisible = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeSectionHeader(stringResource(R.string.ai_section))

        ThemeSettingDropdown(
            items = providerNames,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { onProviderChange(providers[it].id) },
            title = stringResource(R.string.ai_provider),
            startAction = { SettingIcon(Icons.Filled.SmartToy) },
            position = SettingCardPosition.First,
        )
        ThemeSettingTextField(
            value = state.apiUri,
            onValueChange = onApiUriChange,
            title = stringResource(R.string.ai_api_uri),
            placeholder = def.defaultApiUri,
            startAction = { SettingIcon(Icons.Filled.Link) },
            position = SettingCardPosition.Middle,
        )
        ThemeSettingTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            title = stringResource(R.string.ai_api_key),
            inputMode = if (apiKeyVisible) SettingInputMode.Text else SettingInputMode.Password,
            startAction = { SettingIcon(Icons.Filled.Key) },
            position = SettingCardPosition.Middle,
            fieldEndAction = {
                ThemeIconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    ThemeIcon(
                        imageVector = if (apiKeyVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (apiKeyVisible) hideKeyLabel else showKeyLabel,
                        tint = AnkioTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            endAction = if (def.createKeyUri.isNotBlank()) {
                {
                    ThemeIconButton(
                        onClick = { onOpenCreateKeyUri(def.createKeyUri) },
                    ) {
                        ThemeIcon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.ai_create_key),
                            tint = AnkioTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                null
            },
        )
        ThemeSettingTextField(
            value = state.model,
            onValueChange = onModelChange,
            title = stringResource(R.string.ai_model),
            summary = stringResource(R.string.ai_model_hint, def.defaultModel),
            placeholder = def.defaultModel,
            startAction = { SettingIcon(Icons.Filled.SmartToy) },
            position = SettingCardPosition.Last,
            enabled = !isRefreshingModels,
            fieldEndAction = {
                SettingFieldListPopup(
                    options = modelPopupOptions,
                    onSelect = onModelChange,
                    enabled = !isRefreshingModels,
                )
            },
            endAction = {
                ThemeIconButton(
                    onClick = { refreshModels() },
                    enabled = !isRefreshingModels,
                ) {
                    ThemeIcon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = refreshModelsMessage,
                        tint = AnkioTheme.colorScheme.primary,
                    )
                }
            },
        )

        ThemeSettingSwitch(
            title = stringResource(R.string.ai_vision_enabled),
            checked = state.visionEnabled,
            onCheckedChange = onVisionEnabledChange,
            startAction = { SettingIcon(Icons.Filled.Visibility) },
            position = SettingCardPosition.Single,
        )

        ThemeCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ThemeSecondaryButton(
                        onClick = {
                            val settings = state.toEffectiveSettings(def)
                            if (settings.apiKey.isBlank()) {
                                onTestStateChange(AiTestUiState.Failure(invalidMessage))
                                return@ThemeSecondaryButton
                            }
                            onSave()
                            onTestStateChange(AiTestUiState.Saved)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isTesting,
                        text = stringResource(R.string.ai_save),
                    )
                    ThemePrimaryButton(
                        onClick = {
                            if (state.isTesting) return@ThemePrimaryButton
                            val settings = state.toEffectiveSettings(def)
                            if (settings.apiKey.isBlank()) {
                                onTestStateChange(AiTestUiState.Failure(invalidMessage))
                                return@ThemePrimaryButton
                            }
                            scope.launch {
                                onTestStateChange(AiTestUiState.Running)
                                when (val result = AiTest.run(ai, settings)) {
                                    AiTestResult.Success -> {
                                        onSave()
                                        onTestStateChange(AiTestUiState.Success)
                                    }

                                    is AiTestResult.Failure -> {
                                        val detail = result.message.ifBlank { "unknown" }
                                        onTestStateChange(
                                            AiTestUiState.Failure(failedTemplate.format(detail)),
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isTesting,
                        text = stringResource(R.string.ai_test),
                    )
                }

                AiTestResultContent(
                    testState = state.testState,
                    testingMessage = testingMessage,
                    successMessage = successMessage,
                    savedMessage = savedMessage,
                )
            }
        }
    }
}

@Composable
private fun SettingIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = AnkioTheme.colorScheme.primary,
    )
}

@Composable
internal fun AiTestResultContent(
    testState: AiTestUiState,
    testingMessage: String,
    successMessage: String,
    savedMessage: String,
) {
    val idleMessage = stringResource(R.string.ai_test_result_idle)
    val title = stringResource(R.string.ai_test_result_title)

    Column(modifier = Modifier.fillMaxWidth()) {
        ThemeText(
            text = title,
            style = AnkioTheme.textStyles.title4,
            color = AnkioTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        when (testState) {
            AiTestUiState.Idle -> ThemeText(
                text = idleMessage,
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.onSurfaceVariant,
            )

            AiTestUiState.Running -> {
                ThemeLinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ThemeText(
                    text = testingMessage,
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.primary,
                )
            }

            AiTestUiState.Success -> ThemeText(
                text = successMessage,
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.primary,
            )

            is AiTestUiState.Failure -> ThemeText(
                text = testState.message,
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.error,
            )

            AiTestUiState.Saved -> ThemeText(
                text = savedMessage,
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.primary,
            )
        }
    }
}
