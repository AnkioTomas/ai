package net.ankio.ai.lib

interface AiLogger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

object NoOpAiLogger : AiLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}
