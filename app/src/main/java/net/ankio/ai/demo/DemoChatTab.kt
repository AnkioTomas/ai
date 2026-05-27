package net.ankio.ai.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.core.displayMessage
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
    val outputScroll = rememberScrollState()

    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            outputScroll.animateScrollTo(outputScroll.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeText(
            text = "Chat",
            style = AnkioTheme.textStyles.title3,
            color = AnkioTheme.colorScheme.onSurface,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(outputScroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                error != null -> ThemeText(
                    text = "Error: $error",
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.error,
                )

                output.isNotBlank() -> ThemeText(
                    text = output,
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.onSurface,
                )

                sending -> ThemeText(
                    text = "…",
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )

                else -> ThemeText(
                    text = "Send a message to start streaming.",
                    style = AnkioTheme.textStyles.body2,
                    color = AnkioTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { androidx.compose.material3.Text("Message") },
            minLines = 2,
            maxLines = 5,
            enabled = !sending,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            ThemePrimaryButton(
                onClick = {
                    val text = input.trim()
                    if (text.isBlank() || sending) return@ThemePrimaryButton
                    scope.launch {
                        sending = true
                        output = ""
                        error = null
                        val chunks = Channel<String>(Channel.UNLIMITED)
                        val consumer = launch {
                            for (chunk in chunks) {
                                output += chunk
                            }
                        }
                        ai.requestStream(
                            system = "",
                            user = text,
                            providerId = null,
                        ) { chunk ->
                            chunks.trySend(chunk)
                        }.onFailure { t ->
                            error = t.displayMessage()
                        }
                        chunks.close()
                        consumer.join()
                        sending = false
                    }
                },
                enabled = !sending,
                text = if (sending) "Sending…" else "Send",
            )
        }
    }
}
