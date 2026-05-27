package net.ankio.ai.demo

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class DemoTab(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Settings(R.string.tab_ai_settings, Icons.Default.Settings),
    Chat(R.string.tab_chat, Icons.AutoMirrored.Filled.Chat),
}

