package net.ankio.ai.demo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.ankio.ai.demo.store.AiSettingsStore
import net.ankio.ai.demo.store.LogcatAiLogger
import net.ankio.ai.lib.AI_DEFAULT_PROVIDER_ID
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.ui.settings.AiSettingsScreen
import net.ankio.ai.lib.ui.settings.AiSettingsState
import net.ankio.ai.lib.ui.settings.AiTestUiState
import net.ankio.theme.compat.ThemeNavigationBar
import net.ankio.theme.compat.ThemeNavigationBarItem

@Composable
fun DemoMainScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(DemoTab.Settings) }

    val store = remember { AiSettingsStore(context) }
    val ai = remember(store) { Ai(store, LogcatAiLogger) }

    var settingsState by remember {
        mutableStateOf(
            AiSettingsState(providerId = AI_DEFAULT_PROVIDER_ID),
        )
    }

    suspend fun loadProviderSettings(providerId: String) {
        val def = ai.providers.first { it.id == providerId }
        settingsState = AiSettingsState.from(def, ai.settings(providerId), ai.proxy())
            .copy(testState = AiTestUiState.Idle)
    }

    LaunchedEffect(Unit) {
        loadProviderSettings(ai.activeProviderId())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DemoTab.Settings -> AiSettingsScreen(
                    modifier = Modifier.fillMaxSize(),
                    ai = ai,
                    providers = ai.providers,
                    state = settingsState,
                    onProviderChange = { id ->
                        scope.launch {
                            ai.switchProvider(id)
                            loadProviderSettings(id)
                        }
                    },
                    onApiKeyChange = { settingsState = settingsState.copy(apiKey = it) },
                    onApiUriChange = { settingsState = settingsState.copy(apiUri = it) },
                    onModelChange = { settingsState = settingsState.copy(model = it) },
                    onVisionEnabledChange = {
                        settingsState = settingsState.copy(visionEnabled = it)
                    },
                    onTemperatureChange = {
                        settingsState = settingsState.copy(temperature = it)
                    },
                    onProxyChange = { settingsState = settingsState.copy(proxy = it) },
                    onSave = {
                        scope.launch {
                            val def = ai.providers.first { it.id == settingsState.providerId }
                            ai.saveSettings(settingsState.toEffectiveSettings(def))
                            ai.saveProxy(settingsState.proxy)
                        }
                    },
                    onTestStateChange = { settingsState = settingsState.copy(testState = it) },
                    onOpenCreateKeyUri = { uri ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    },
                )

                DemoTab.Chat -> DemoChatTab(ai = ai)
            }
        }

        ThemeNavigationBar {
            DemoTab.entries.forEach { tab ->
                ThemeNavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = tab.icon,
                    label = stringResource(tab.titleRes),
                )
            }
        }
    }
}
