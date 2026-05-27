package net.ankio.ai.lib.ui.settings

/**
 * 设置页「测试连接 / 保存」区域的展示状态。
 */
sealed interface AiTestUiState {
    /** 初始或未开始测试。 */
    data object Idle : AiTestUiState

    /** 正在测试连接。 */
    data object Running : AiTestUiState

    /** 测试成功。 */
    data object Success : AiTestUiState

    /**
     * 测试失败。
     *
     * @param message 展示给用户的错误信息。
     */
    data class Failure(val message: String) : AiTestUiState

    /** 仅保存成功（未跑测试或测试前保存）。 */
    data object Saved : AiTestUiState
}
