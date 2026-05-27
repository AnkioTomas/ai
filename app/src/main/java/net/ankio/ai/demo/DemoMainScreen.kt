package net.ankio.ai.demo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.ankio.ai.demo.store.AiSettingsStore
import net.ankio.ai.demo.store.LogcatAiLogger
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.ui.settings.AiSettingsScreen
import net.ankio.theme.compat.ThemeNavigationBar
import net.ankio.theme.compat.ThemeNavigationBarItem

@Composable
fun DemoMainScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableStateOf(DemoTab.Settings) }

    val store = remember { AiSettingsStore(context) }
    val ai = remember(store) { Ai(store, LogcatAiLogger) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DemoTab.Settings -> AiSettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    ai = ai,
                    providers = ai.providers,
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
