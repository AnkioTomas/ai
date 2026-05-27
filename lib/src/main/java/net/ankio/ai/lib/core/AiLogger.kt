package net.ankio.ai.lib.core

/** 日志接口，由宿主实现；lib 不提供默认实现。 */
interface AiLogger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
