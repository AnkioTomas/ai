package net.ankio.ai.lib.core

/**
 * 日志接口，由宿主实现；lib 不提供默认实现。
 */
interface AiLogger {
    /**
     * 输出调试日志。
     *
     * @param tag 日志标签，通常为 `Ai/{providerId}`。
     * @param message 日志正文。
     */
    fun debug(tag: String, message: String)

    /**
     * 输出错误日志。
     *
     * @param tag 日志标签。
     * @param message 错误描述。
     * @param throwable 关联异常，可为 `null`。
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
