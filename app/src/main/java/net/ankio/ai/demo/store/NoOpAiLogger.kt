package net.ankio.ai.demo.store

import net.ankio.ai.lib.core.AiLogger

/** Demo：空实现 [AiLogger]，宿主可按需替换为真实日志。 */
object NoOpAiLogger : AiLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}
