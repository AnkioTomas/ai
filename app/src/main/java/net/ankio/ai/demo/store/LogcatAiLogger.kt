package net.ankio.ai.demo.store

import android.util.Log
import net.ankio.ai.lib.core.AiLogger

/** Demo：将 [AiLogger] 输出到 Android Logcat。 */
object LogcatAiLogger : AiLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
