package net.ankio.ai.lib.model.gemini

import kotlinx.serialization.Serializable

// ── 请求 ─────────────────────────────────────────────────────────────

/** Gemini 生成参数（温度等）。 */
@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
)

/** Gemini `generateContent` / 流式请求体。 */
@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
)

/** 单轮 content（role + parts）。 */
@Serializable
data class GeminiContent(val role: String, val parts: List<GeminiPart>)

/** 文本或内联图片 part。 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

/** Base64 内联图片数据。 */
@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String,
)

// ── 响应 ─────────────────────────────────────────────────────────────

/** 模型列表 API 响应。 */
@Serializable
data class GeminiModelsResponse(val models: List<GeminiModelInfo> = emptyList())

/** 模型元信息。 */
@Serializable
data class GeminiModelInfo(val name: String)

/** 生成内容响应（非流式或 SSE 单条）。 */
@Serializable
data class GeminiGenerateResponse(val candidates: List<GeminiCandidate> = emptyList())

/** 候选回复。 */
@Serializable
data class GeminiCandidate(val content: GeminiResponseContent? = null)

/** 回复 content 容器。 */
@Serializable
data class GeminiResponseContent(val parts: List<GeminiResponsePart> = emptyList())

/** 回复文本 part。 */
@Serializable
data class GeminiResponsePart(val text: String? = null)

/** 取第一条候选的首段文本。 */
fun GeminiGenerateResponse.firstText(): String? =
    candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text

/** 构造 Gemini 请求 contents（system 与 user 分两条 content）。 */
object GeminiRequestFactory {
    /**
     * @param system 系统提示（作为单独 content）。
     * @param user 用户文本。
     * @param image Base64 或 data URL；非空时追加 inlineData part。
     */
    fun build(
        system: String,
        user: String,
        image: String,
        generationConfig: GeminiGenerationConfig? = null,
    ): GeminiGenerateRequest {
        val userParts = mutableListOf(GeminiPart(text = user))
        if (image.isNotBlank()) {
            val base64 = if (image.startsWith("data:image")) {
                image.substringAfter("base64,").ifBlank { image }
            } else {
                image
            }
            userParts += GeminiPart(
                inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64),
            )
        }
        return GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = system))),
                GeminiContent(role = "user", parts = userParts),
            ),
            generationConfig = generationConfig,
        )
    }
}
