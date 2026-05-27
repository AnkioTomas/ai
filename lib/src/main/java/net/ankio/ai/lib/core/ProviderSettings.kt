package net.ankio.ai.lib.core

import kotlinx.serialization.Serializable
import net.ankio.ai.lib.provider.ProviderDef

@Serializable
data class ProviderSettings(
    val providerId: String,
    val apiKey: String = "",
    val apiUri: String? = null,
    val model: String? = null,
    /** 用户是否启用视觉识别 */
    val visionEnabled: Boolean = true,
) {
    /** 未保存的 apiUri / model 用提供商默认值补全（用于展示与请求）。 */
    fun withProviderDefaults(def: ProviderDef): ProviderSettings = copy(
        apiUri = apiUri?.takeIf { it.isNotBlank() } ?: def.defaultApiUri.takeIf { it.isNotBlank() },
        model = model?.takeIf { it.isNotBlank() } ?: def.defaultModel.takeIf { it.isNotBlank() },
    )
}
