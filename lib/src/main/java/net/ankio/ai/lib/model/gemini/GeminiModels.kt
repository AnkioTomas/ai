package net.ankio.ai.lib.model.gemini

import kotlinx.serialization.Serializable

// ── 请求 ─────────────────────────────────────────────────────────────

@Serializable
data class GeminiGenerateRequest(val contents: List<GeminiContent>)

@Serializable
data class GeminiContent(val role: String, val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String,
)

// ── 响应 ─────────────────────────────────────────────────────────────

@Serializable
data class GeminiModelsResponse(val models: List<GeminiModelInfo> = emptyList())

@Serializable
data class GeminiModelInfo(val name: String)

@Serializable
data class GeminiGenerateResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
data class GeminiCandidate(val content: GeminiResponseContent? = null)

@Serializable
data class GeminiResponseContent(val parts: List<GeminiResponsePart> = emptyList())

@Serializable
data class GeminiResponsePart(val text: String? = null)

fun GeminiGenerateResponse.firstText(): String? =
    candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text

object GeminiRequestFactory {
    fun build(system: String, user: String, image: String): GeminiGenerateRequest {
        val userParts = mutableListOf(GeminiPart(text = user))
        if (image.isNotBlank()) {
            val base64 = if (image.startsWith("data:image")) {
                image.substringAfter("base64,").ifBlank { image }
            } else {
                image
            }
            userParts += GeminiPart(
                inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64)
            )
        }
        return GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = system))),
                GeminiContent(role = "user", parts = userParts),
            )
        )
    }
}
