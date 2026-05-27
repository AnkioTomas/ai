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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
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
import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.core.displayMessage
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
import net.ankio.theme.settings.ThemeSettingSlider
import net.ankio.theme.settings.ThemeSettingSwitch
import net.ankio.theme.settings.ThemeSettingTextField
import net.ankio.theme.toast.ThemeToast
import kotlin.math.round

/**
 * AI 提供商配置 Compose 页面。
 *
 * 内部维护表单状态与模型列表缓存，通过 [ai] 读写 [net.ankio.ai.lib.core.AiDataStore]。
 * 滚动由宿主在外层 [Modifier] 上提供（例如 `verticalScroll`）。
 *
 * @param ai [Ai] 实例（已注入 DataStore）。
 * @param providers 可选提供商列表，通常为 [Ai.providers]。
 * @param onOpenCreateKeyUri 打开申请 Key 外链（非空 [ProviderDef.createKeyUri] 时显示按钮）。
 */
@Composable
fun AiSettingsScreen(
    ai: Ai,
    providers: List<ProviderDef>,
    modifier: Modifier = Modifier,
    onOpenCreateKeyUri: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var settingsState by remember(providers) {
        mutableStateOf(AiSettingsState(providerId = providers.first().id))
    }
    var modelsCache by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val def = providers.firstOrNull { it.id == settingsState.providerId } ?: providers.first()
    val modelItems = modelsCache[settingsState.providerId].orEmpty()
    val providerNames = providers.map { it.displayName }
    val selectedIndex =
        providers.indexOfFirst { it.id == settingsState.providerId }.coerceAtLeast(0)

    val isRefreshingModels = settingsState.testState is AiTestUiState.RefreshingModels
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }

    val invalidMessage = stringResource(R.string.ai_config_invalid)
    val testingMessage = stringResource(
        if (settingsState.visionEnabled) R.string.ai_test_running_vision else R.string.ai_test_running,
    )
    val successMessage = stringResource(
        if (settingsState.visionEnabled) R.string.ai_test_success_vision else R.string.ai_test_success,
    )
    val failedTemplate = stringResource(R.string.ai_test_failed)
    val savedMessage = stringResource(R.string.ai_saved)
    val refreshModelsMessage = stringResource(R.string.ai_refresh_models)
    val refreshModelsFailedTemplate = stringResource(R.string.ai_refresh_models_failed)
    val refreshModelsRunningMessage = stringResource(R.string.ai_refresh_models_running)
    val refreshModelsSuccessTemplate = stringResource(R.string.ai_refresh_models_success)
    val refreshModelsEmptyMessage = stringResource(R.string.ai_refresh_models_empty)
    val showKeyLabel = stringResource(R.string.ai_show_api_key)
    val hideKeyLabel = stringResource(R.string.ai_hide_api_key)
    val proxyPlaceholder = stringResource(R.string.ai_proxy_placeholder)

    suspend fun loadProviderSettings(providerId: String) {
        val providerDef = providers.firstOrNull { it.id == providerId } ?: providers.first()
        settingsState = AiSettingsState.from(providerDef, ai.settings(providerDef.id), ai.proxy())
            .copy(testState = AiTestUiState.Idle)
    }

    suspend fun persistSettings() {
        val providerDef =
            providers.firstOrNull { it.id == settingsState.providerId } ?: providers.first()
        ai.saveSettings(settingsState.toEffectiveSettings(providerDef))
        ai.saveProxy(settingsState.proxy)
    }

    val modelPopupOptions = remember(modelItems, settingsState.model) {
        buildList {
            if (settingsState.model.isNotBlank()) add(settingsState.model)
            addAll(modelItems.filter { it.isNotBlank() && it != settingsState.model })
        }
    }

    fun refreshModels() {
        val settings = settingsState.toEffectiveSettings(def)
        if (settings.apiKey.isBlank()) {
            settingsState = settingsState.copy(testState = AiTestUiState.Failure(invalidMessage))
            ThemeToast.show(invalidMessage, ThemeToast.Style.Warning)
            return
        }
        scope.launch {
            settingsState = settingsState.copy(testState = AiTestUiState.RefreshingModels)
            ai.saveProxy(settingsState.proxy)
            ai.listModels(settings)
                .onSuccess { models ->
                    modelsCache = modelsCache + (settingsState.providerId to models)
                    when {
                        models.isEmpty() -> {
                            settingsState = settingsState.copy(
                                testState = AiTestUiState.Failure(refreshModelsEmptyMessage),
                            )
                            ThemeToast.show(refreshModelsEmptyMessage, ThemeToast.Style.Warning)
                        }

                        else -> {
                            if (settingsState.model !in models) {
                                settingsState = settingsState.copy(
                                    model = def.defaultModel.takeIf { it in models }
                                        ?: models.first(),
                                )
                            }
                            settingsState = settingsState.copy(
                                testState = AiTestUiState.ModelsRefreshed(models.size),
                            )
                            ThemeToast.show(
                                refreshModelsSuccessTemplate.format(models.size),
                                ThemeToast.Style.Success,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    val message = refreshModelsFailedTemplate.format(error.displayMessage())
                    settingsState = settingsState.copy(testState = AiTestUiState.Failure(message))
                    ThemeToast.show(message, ThemeToast.Style.Error)
                }
        }
    }

    LaunchedEffect(ai, providers) {
        loadProviderSettings(ai.activeProviderId())
    }

    LaunchedEffect(settingsState.providerId) {
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
            onSelectedIndexChange = { index ->
                val id = providers[index].id
                scope.launch {
                    ai.switchProvider(id)
                    loadProviderSettings(id)
                }
            },
            title = stringResource(R.string.ai_provider),
            startAction = { SettingIcon(Icons.Filled.SmartToy) },
            position = SettingCardPosition.First,
        )
        ThemeSettingTextField(
            value = settingsState.apiUri,
            onValueChange = { settingsState = settingsState.copy(apiUri = it) },
            title = stringResource(R.string.ai_api_uri),
            placeholder = def.defaultApiUri,
            startAction = { SettingIcon(Icons.Filled.Link) },
            position = SettingCardPosition.Middle,
        )
        ThemeSettingTextField(
            value = settingsState.apiKey,
            onValueChange = { settingsState = settingsState.copy(apiKey = it) },
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
            value = settingsState.model,
            onValueChange = { settingsState = settingsState.copy(model = it) },
            title = stringResource(R.string.ai_model),
            placeholder = def.defaultModel,
            startAction = { SettingIcon(Icons.Filled.SmartToy) },
            position = SettingCardPosition.Middle,
            enabled = !isRefreshingModels,
            fieldEndAction = {
                SettingFieldListPopup(
                    options = modelPopupOptions,
                    onSelect = { settingsState = settingsState.copy(model = it) },
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

        val temperatureValue = snapAiTemperature(settingsState.temperature.toFloat())
        ThemeSettingSlider(
            title = stringResource(R.string.ai_temperature),
            summary = stringResource(
                R.string.ai_temperature_summary,
                ProviderSettings.DEFAULT_TEMPERATURE,
            ),
            value = temperatureValue,
            onValueChange = {
                settingsState = settingsState.copy(temperature = snapAiTemperature(it).toDouble())
            },
            valueRange = AI_TEMPERATURE_MIN..AI_TEMPERATURE_MAX,
            steps = AI_TEMPERATURE_STEPS,
            valueLabel = stringResource(R.string.ai_temperature_value, temperatureValue),
            startAction = { SettingIcon(Icons.Filled.Tune) },
            position = SettingCardPosition.Middle,
        )

        ThemeSettingSwitch(
            title = stringResource(R.string.ai_vision_enabled),
            checked = settingsState.visionEnabled,
            onCheckedChange = { settingsState = settingsState.copy(visionEnabled = it) },
            startAction = { SettingIcon(Icons.Filled.Visibility) },
            position = SettingCardPosition.Middle,
        )
        ThemeSettingTextField(
            value = settingsState.proxy,
            onValueChange = { settingsState = settingsState.copy(proxy = it) },
            title = stringResource(R.string.ai_proxy),
            summary = stringResource(R.string.ai_proxy_summary),
            placeholder = proxyPlaceholder,
            startAction = { SettingIcon(Icons.Filled.VpnLock) },
            position = SettingCardPosition.Last,
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
                            val settings = settingsState.toEffectiveSettings(def)
                            if (settings.apiKey.isBlank()) {
                                settingsState = settingsState.copy(
                                    testState = AiTestUiState.Failure(invalidMessage),
                                )
                                return@ThemeSecondaryButton
                            }
                            scope.launch { persistSettings() }
                            settingsState = settingsState.copy(testState = AiTestUiState.Saved)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !settingsState.isTesting,
                        text = stringResource(R.string.ai_save),
                    )
                    ThemePrimaryButton(
                        onClick = {
                            if (settingsState.isTesting) return@ThemePrimaryButton
                            val settings = settingsState.toEffectiveSettings(def)
                            if (settings.apiKey.isBlank()) {
                                settingsState = settingsState.copy(
                                    testState = AiTestUiState.Failure(invalidMessage),
                                )
                                return@ThemePrimaryButton
                            }
                            scope.launch {
                                settingsState =
                                    settingsState.copy(testState = AiTestUiState.Running)
                                ai.saveProxy(settingsState.proxy)
                                when (val result = AiTest.run(ai, settings)) {
                                    AiTestResult.Success -> {
                                        persistSettings()
                                        settingsState =
                                            settingsState.copy(testState = AiTestUiState.Success)
                                    }

                                    is AiTestResult.Failure -> {
                                        settingsState = settingsState.copy(
                                            testState = AiTestUiState.Failure(
                                                failedTemplate.format(result.message),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !settingsState.isTesting,
                        text = stringResource(R.string.ai_test),
                    )
                }

                AiTestResultContent(
                    testState = settingsState.testState,
                    testingMessage = testingMessage,
                    successMessage = successMessage,
                    savedMessage = savedMessage,
                    refreshModelsRunningMessage = refreshModelsRunningMessage,
                    refreshModelsSuccessTemplate = refreshModelsSuccessTemplate,
                )
            }
        }
    }
}

/** 设置项左侧主题色图标。 */
@Composable
private fun SettingIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = AnkioTheme.colorScheme.primary,
    )
}

/**
 * 测试结果卡片内容区。
 *
 * @param testState 当前测试/保存状态。
 * @param testingMessage 测试中提示文案。
 * @param successMessage 测试成功文案。
 * @param savedMessage 仅保存成功文案。
 */
@Composable
internal fun AiTestResultContent(
    testState: AiTestUiState,
    testingMessage: String,
    successMessage: String,
    savedMessage: String,
    refreshModelsRunningMessage: String,
    refreshModelsSuccessTemplate: String,
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

            AiTestUiState.Running,
            AiTestUiState.RefreshingModels,
                -> {
                ThemeLinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ThemeText(
                    text = if (testState is AiTestUiState.RefreshingModels) {
                        refreshModelsRunningMessage
                    } else {
                        testingMessage
                    },
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.primary,
                )
            }

            is AiTestUiState.ModelsRefreshed -> ThemeText(
                text = refreshModelsSuccessTemplate.format(testState.count),
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.primary,
            )

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

private const val AI_TEMPERATURE_MIN = 0f
private const val AI_TEMPERATURE_MAX = 2f
private const val AI_TEMPERATURE_STEP = 0.1f
private const val AI_TEMPERATURE_STEPS =
    ((AI_TEMPERATURE_MAX - AI_TEMPERATURE_MIN) / AI_TEMPERATURE_STEP).toInt() - 1

private fun snapAiTemperature(value: Float): Float =
    (round(value / AI_TEMPERATURE_STEP) * AI_TEMPERATURE_STEP)
        .coerceIn(AI_TEMPERATURE_MIN, AI_TEMPERATURE_MAX)
