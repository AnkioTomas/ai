package net.ankio.ai.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface AiTestResult {
    data object Success : AiTestResult
    data class Failure(val message: String) : AiTestResult
}

/** 使用表单中的配置测试连接（无需先保存）。 */
object AiTest {
    suspend fun run(ai: Ai, settings: ProviderSettings): AiTestResult = withContext(Dispatchers.IO) {
        if (settings.apiKey.isBlank()) {
            return@withContext AiTestResult.Failure("api key required")
        }
        ai.testConnection(settings).fold(
            onSuccess = { AiTestResult.Success },
            onFailure = { AiTestResult.Failure(it.message ?: it.javaClass.simpleName) },
        )
    }
}
