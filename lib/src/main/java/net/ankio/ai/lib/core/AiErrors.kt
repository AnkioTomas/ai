package net.ankio.ai.lib.core

/** 用于日志与 UI 的可读错误文案（沿 cause 链取首个非空 [Throwable.message]）。 */
fun Throwable.displayMessage(): String {
    generateSequence(this) { it.cause }.forEach { throwable ->
        throwable.message?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return javaClass.simpleName
}

internal fun formatLogError(prefix: String, error: Throwable?): String =
    if (error != null) "$prefix: ${error.displayMessage()}" else prefix
