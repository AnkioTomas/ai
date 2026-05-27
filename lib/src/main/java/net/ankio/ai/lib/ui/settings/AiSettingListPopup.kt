package net.ankio.ai.lib.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.ankio.ai.lib.R
import net.ankio.theme.AnkioTheme
import net.ankio.theme.compat.ThemeIcon
import net.ankio.theme.compat.ThemeIconButton
import net.ankio.theme.compat.ThemeListPopupColumn
import net.ankio.theme.compat.ThemeListPopupItem
import net.ankio.theme.compat.ThemeSuperListPopup

/**
 * 设置项输入框内的下拉按钮 + [ThemeSuperListPopup] 列表。
 */
@Composable
internal fun SettingFieldListPopup(
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyText: String = stringResource(R.string.ai_model_list_empty),
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        ThemeIconButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.onGloballyPositioned { anchorHeightPx = it.size.height },
        ) {
            ThemeIcon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = stringResource(R.string.ai_pick_model),
                tint = AnkioTheme.colorScheme.onSurfaceVariant,
            )
        }
        ThemeSuperListPopup(
            show = expanded,
            onDismissRequest = { expanded = false },
            offset = IntOffset(0, anchorHeightPx),
            maxHeight = 300.dp,
            minWidth = 200.dp,
            content = {
                ThemeListPopupColumn {
                    if (options.isEmpty()) {
                        ThemeListPopupItem(
                            text = emptyText,
                            onClick = { expanded = false },
                        )
                    } else {
                        options.forEach { option ->
                            ThemeListPopupItem(
                                text = option,
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}
