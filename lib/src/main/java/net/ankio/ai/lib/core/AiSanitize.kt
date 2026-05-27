package net.ankio.ai.lib.core

/**
 * 清理凭证：仅保留 OkHttp Header 允许的可见 ASCII（`0x20`～`0x7e`）。
 *
 * 粘贴 Key 时常见尾部换行、误夹中文等，会导致 `IllegalArgumentException`。
 */
fun String.sanitizeCredential(): String =
    filter { it.code in 0x20..0x7e }.trim()

/** 清理 URL / 模型名等单行配置。 */
fun String.sanitizeSingleLine(): String = sanitizeCredential()
