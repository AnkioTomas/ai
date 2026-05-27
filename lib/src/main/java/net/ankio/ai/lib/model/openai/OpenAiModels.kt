package net.ankio.ai.lib.model.openai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import net.ankio.ai.lib.AiJson

// ── 请求 ─────────────────────────────────────────────────────────────

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean? = null,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: MessageContent,
)

@Serializable(with = MessageContentSerializer::class)
sealed class MessageContent {
    data class Text(val value: String) : MessageContent()
    data class Parts(val items: List<ContentPart>) : MessageContent()
}

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
)

@Serializable
data class ImageUrl(val url: String)

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

@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList(),
)

@Serializable
data class ModelInfo(val id: String)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(val message: ChatResponseMessage? = null)

@Serializable
data class ChatResponseMessage(val content: String? = null)

@Serializable
data class ChatCompletionStreamChunk(
    val choices: List<StreamChoice> = emptyList(),
)

@Serializable
data class StreamChoice(val delta: StreamDelta? = null)

@Serializable
data class StreamDelta(val content: String? = null)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorDetail(val message: String? = null)

fun ApiErrorResponse.resolveMessage(fallback: String): String =
    error?.message?.takeIf { it.isNotBlank() }
        ?: message?.takeIf { it.isNotBlank() }
        ?: fallback

fun ChatCompletionResponse.firstContent(): String =
    choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
        ?: error("Empty choices content")

fun ChatCompletionStreamChunk.firstDeltaContent(): String? =
    choices.firstOrNull()?.delta?.content

// ── 构建辅助 ─────────────────────────────────────────────────────────

object ChatMessageFactory {
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
