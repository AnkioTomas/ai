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
import net.ankio.ai.demo.store.NoOpAiLogger
import net.ankio.ai.lib.AI_DEFAULT_PROVIDER_ID
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.core.ProviderSettings
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
    val ai = remember(store) { Ai(store, NoOpAiLogger) }

    var providerId by rememberSaveable { mutableStateOf(AI_DEFAULT_PROVIDER_ID) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var apiUri by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var visionEnabled by rememberSaveable { mutableStateOf(true) }
    var testState by remember { mutableStateOf<AiTestUiState>(AiTestUiState.Idle) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        providerId = ai.activeProviderId()
        loaded = true
    }

    LaunchedEffect(providerId, loaded) {
        if (!loaded) return@LaunchedEffect
        val saved = ai.settings(providerId)
        apiKey = saved.apiKey
        apiUri = saved.apiUri.orEmpty()
        model = saved.model.orEmpty()
        visionEnabled = saved.visionEnabled
        testState = AiTestUiState.Idle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DemoTab.Settings -> AiSettingsScreen(
                    ai = ai,
                    providers = ai.providers,
                    state = AiSettingsState(
                        providerId = providerId,
                        apiKey = apiKey,
                        apiUri = apiUri,
                        model = model,
                        visionEnabled = visionEnabled,
                        testState = testState,
                    ),
                    onProviderChange = { id ->
                        scope.launch {
                            ai.switchProvider(id)
                            providerId = id
                            val saved = ai.settings(id)
                            apiKey = saved.apiKey
                            apiUri = saved.apiUri.orEmpty()
                            model = saved.model.orEmpty()
                            visionEnabled = saved.visionEnabled
                            testState = AiTestUiState.Idle
                        }
                    },
                    onApiKeyChange = { apiKey = it },
                    onApiUriChange = { apiUri = it },
                    onModelChange = { model = it },
                    onVisionEnabledChange = { visionEnabled = it },
                    onSave = {
                        scope.launch {
                            val settings = ProviderSettings(
                                providerId = providerId,
                                apiKey = apiKey.trim(),
                                apiUri = apiUri.trim().ifBlank { null },
                                model = model.trim().ifBlank { null },
                                visionEnabled = visionEnabled,
                            )
                            ai.saveSettings(settings)
                        }
                    },
                    onTestStateChange = { testState = it },
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

