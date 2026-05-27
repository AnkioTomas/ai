package net.ankio.ai.lib.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.ai.lib.Ai
import net.ankio.ai.lib.core.ProviderSettings

/** 设置页「测试连接」的结果。 */
sealed interface AiTestResult {
    /** 连接与鉴权成功。 */
    data object Success : AiTestResult

    /** 失败原因（已本地化或原始错误信息）。 */
    data class Failure(val message: String) : AiTestResult
}

/**
 * 使用当前表单配置测试 AI 连接（无需先 [Ai.saveSettings]）。
 */
object AiTest {
    /**
     * 执行连接测试。
     *
     * @param ai [Ai] 实例。
     * @param settings 表单中的有效配置（建议 [ProviderSettings.withProviderDefaults]）。
     * @return [AiTestResult.Success] 或 [AiTestResult.Failure]。
     */
    suspend fun run(ai: Ai, settings: ProviderSettings): AiTestResult =
        withContext(Dispatchers.IO) {
            if (settings.apiKey.isBlank()) {
                return@withContext AiTestResult.Failure("api key required")
            }
            ai.testConnection(settings).fold(
                onSuccess = { AiTestResult.Success },
                onFailure = { AiTestResult.Failure(it.message ?: it.javaClass.simpleName) },
            )
        }
}
