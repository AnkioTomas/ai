package net.ankio.ai.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.ai.lib.model.openai.ApiErrorResponse
import net.ankio.ai.lib.model.openai.ChatCompletionRequest
import net.ankio.ai.lib.model.openai.ChatCompletionResponse
import net.ankio.ai.lib.model.openai.ChatCompletionStreamChunk
import net.ankio.ai.lib.model.openai.ChatMessageFactory
import net.ankio.ai.lib.model.openai.ModelsListResponse
import net.ankio.ai.lib.model.openai.firstContent
import net.ankio.ai.lib.model.openai.firstDeltaContent
import net.ankio.ai.lib.model.openai.resolveMessage
import okhttp3.Request

internal class OpenAiBackend(
    override val def: ProviderDef,
) : ProviderBackend {

    private val json get() = AiJson.json

    override suspend fun models(ctx: AiCtx): List<String> {
        def.staticModels?.let { return it }
        if (def.modelsPath.isBlank()) return emptyList()
        ctx.logD("获取模型列表")
        val request = Request.Builder()
            .url("${ctx.apiUri.trimEnd('/')}${def.modelsPath}")
            .addHeader("Authorization", "Bearer ${ctx.apiKey}")
            .userAgent(ctx)
            .build()
        return runCatchingExceptCancel {
            AiHttp.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: error("Empty body")
                json.decodeBody(body, ModelsListResponse.serializer()).data.map { it.id }
            }
        }.getOrElse {
            ctx.logE("获取模型失败", it)
            emptyList()
        }
    }

    override suspend fun chat(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
        onChunk: ((String) -> Unit)?,
    ): Result<String> {
        val body = ChatCompletionRequest(
            model = ctx.model,
            messages = ChatMessageFactory.build(system, user, image),
            temperature = 0.7,
            stream = if (onChunk != null) true else null,
        )
        val request = Request.Builder()
            .url("${ctx.apiUri.trimEnd('/')}${def.chatPath}")
            .addHeader("Authorization", "Bearer ${ctx.apiKey}")
            .addHeader("Content-Type", "application/json")
            .apply { if (onChunk != null) addHeader("Accept", "text/event-stream") }
            .userAgent(ctx)
            .postJson(json, ChatCompletionRequest.serializer(), body)
            .build()

        return runCatchingExceptCancel {
            if (onChunk != null) {
                stream(request, onChunk)
                ""
            } else {
                AiHttp.client.newCall(request).execute().use { response ->
                    val text = response.body?.string()?.removeThink() ?: error("Empty body")
                    if (!response.isSuccessful) {
                        error(json.decodeBody(text, ApiErrorResponse.serializer()).resolveMessage(text))
                    }
                    json.decodeBody(text, ChatCompletionResponse.serializer()).firstContent()
                }
            }
        }
    }

    private suspend fun stream(request: Request, onChunk: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            AiHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                val source = response.body?.source() ?: return@withContext
                val ser = ChatCompletionStreamChunk.serializer()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.substring(6)
                    if (data == "[DONE]") break
                    runCatchingExceptCancel {
                        json.decodeBody(data, ser).firstDeltaContent()?.let(onChunk)
                    }
                }
            }
        }
    }
}
