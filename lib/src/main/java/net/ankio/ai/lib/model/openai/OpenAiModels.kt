package net.ankio.ai.lib.model.openai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import net.ankio.ai.lib.core.AiJson
import net.ankio.ai.lib.core.ProviderSettings

// ── 请求 ─────────────────────────────────────────────────────────────

/** `POST /v1/chat/completions` 请求体。 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = ProviderSettings.DEFAULT_TEMPERATURE,
    /** 为 `true` 时服务端返回 SSE 流。 */
    val stream: Boolean? = null,
)

/** 单条对话消息。 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: MessageContent,
)

/**
 * 消息正文：纯文本或多模态 parts。
 *
 * 序列化时可能是 JSON 字符串或 part 数组，见 [MessageContentSerializer]。
 */
@Serializable(with = MessageContentSerializer::class)
sealed class MessageContent {
    /** 纯文本内容。 */
    data class Text(val value: String) : MessageContent()

    /** 文本 + 图片等多模态片段。 */
    data class Parts(val items: List<ContentPart>) : MessageContent()
}

/** 多模态片段（`text` 或 `image_url`）。 */
@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
)

/** OpenAI 视觉消息中的图片 URL（常为 base64 data URL）。 */
@Serializable
data class ImageUrl(val url: String)

/** [MessageContent] 的 JSON 多态序列化器。 */
object MessageContentSerializer : KSerializer<MessageContent> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MessageContent")

    override fun deserialize(decoder: Decoder): MessageContent {
        val input = decoder as JsonDecoder
        return when (val element = input.decodeJsonElement()) {
            is JsonPrimitive if element.isString ->
                MessageContent.Text(element.content)

            is JsonArray ->
                MessageContent.Parts(
                    element.map { input.json.decodeFromJsonElement(ContentPart.serializer(), it) }
                )

            else -> error("Unsupported message content: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: MessageContent) {
        val output = encoder as JsonEncoder
        val element: JsonElement = when (value) {
            is MessageContent.Text -> JsonPrimitive(value.value)
            is MessageContent.Parts ->
                AiJson.json.encodeToJsonElement(
                    ListSerializer(ContentPart.serializer()),
                    value.items,
                )
        }
        output.encodeJsonElement(element)
    }
}

// ── 响应 ─────────────────────────────────────────────────────────────

/** `GET /v1/models` 响应。 */
@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList(),
)

/** 模型列表项。 */
@Serializable
data class ModelInfo(val id: String)

/** 非流式 chat completion 响应。 */
@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

/** 非流式 choice。 */
@Serializable
data class ChatChoice(val message: ChatResponseMessage? = null)

/** 助手回复消息。 */
@Serializable
data class ChatResponseMessage(val content: String? = null)

/** SSE 流式单条 chunk。 */
@Serializable
data class ChatCompletionStreamChunk(
    val choices: List<StreamChoice> = emptyList(),
)

/** 流式 choice。 */
@Serializable
data class StreamChoice(val delta: StreamDelta? = null)

/** 流式增量内容。 */
@Serializable
data class StreamDelta(val content: String? = null)

/** OpenAI 风格错误响应（兼容 `error.message` 与顶层 `message`）。 */
@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null,
    val message: String? = null,
)

/** 嵌套错误详情。 */
@Serializable
data class ApiErrorDetail(val message: String? = null)

/** 从错误体提取可读信息，否则返回 [fallback]。 */
fun ApiErrorResponse.resolveMessage(fallback: String): String =
    error?.message?.takeIf { it.isNotBlank() }
        ?: message?.takeIf { it.isNotBlank() }
        ?: fallback

/** 取第一条 choice 的文本内容；无内容时抛错。 */
fun ChatCompletionResponse.firstContent(): String =
    choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
        ?: error("Empty choices content")

/** 取流式 chunk 的 delta 文本，可能为 `null`（心跳等）。 */
fun ChatCompletionStreamChunk.firstDeltaContent(): String? =
    choices.firstOrNull()?.delta?.content

// ── 构建辅助 ─────────────────────────────────────────────────────────

/** 构造 OpenAI 多轮 messages 列表（含可选 system 与 base64 图）。 */
object ChatMessageFactory {
    /**
     * @param system 系统提示；空则省略 system 消息。
     * @param user 用户文本。
     * @param image Base64 或 `data:image/...`；空则仅文本 user 消息。
     */
    fun build(system: String, user: String, image: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        if (system.isNotEmpty()) {
            messages += ChatMessage("system", MessageContent.Text(system))
        }
        val userContent = if (image.isBlank()) {
            MessageContent.Text(user)
        } else {
            val dataUrl = if (image.startsWith("data:image")) {
                image
            } else {
                "data:image/jpeg;base64,$image"
            }
            MessageContent.Parts(
                listOf(
                    ContentPart(type = "text", text = user),
                    ContentPart(type = "image_url", imageUrl = ImageUrl(dataUrl)),
                )
            )
        }
        messages += ChatMessage("user", userContent)
        return messages
    }
}
