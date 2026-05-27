package net.ankio.ai.lib.ui

sealed interface AiTestUiState {
    data object Idle : AiTestUiState
    data object Running : AiTestUiState
    data object Success : AiTestUiState
    data class Failure(val message: String) : AiTestUiState
    data object Saved : AiTestUiState
}
