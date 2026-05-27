package net.ankio.ai.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ankio.ai.lib.Ai
import net.ankio.theme.AnkioTheme
import net.ankio.theme.compat.ThemePrimaryButton
import net.ankio.theme.compat.ThemeText

@Composable
fun DemoChatTab(
    ai: Ai,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeText(
            text = "Chat",
            style = AnkioTheme.textStyles.title3,
            color = AnkioTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { androidx.compose.material3.Text("输入") },
            minLines = 3,
            enabled = !sending,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            ThemePrimaryButton(
                onClick = {
                    val text = input.trim()
                    if (text.isBlank() || sending) return@ThemePrimaryButton
                    sending = true
                    output = ""
                    error = null
                    scope.launch {
                        ai.request(
                            system = "",
                            user = text,
                            providerId = null,
                        )
                            .onSuccess { reply -> output = reply }
                            .onFailure { t -> error = t.message ?: "unknown" }
                        sending = false
                    }
                },
                enabled = !sending,
                text = if (sending) "发送中…" else "发送",
            )
        }

        Spacer(Modifier.height(4.dp))

        if (error != null) {
            ThemeText(
                text = "错误：$error",
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.error,
            )
        }

        if (output.isNotBlank()) {
            ThemeText(
                text = output,
                style = AnkioTheme.textStyles.body2,
                color = AnkioTheme.colorScheme.onSurface,
            )
        }
    }
}

