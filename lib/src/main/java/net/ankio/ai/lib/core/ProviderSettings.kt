package net.ankio.ai.lib.core

import kotlinx.serialization.Serializable

@Serializable
data class ProviderSettings(
    val providerId: String,
    val apiKey: String = "",
    val apiUri: String? = null,
    val model: String? = null,
    /** 用户是否启用视觉识别 */
    val visionEnabled: Boolean = true,
)
