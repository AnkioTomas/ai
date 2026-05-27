package net.ankio.ai.lib.provider.openai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.ai.lib.core.AiHttp
import net.ankio.ai.lib.core.AiJson
import net.ankio.ai.lib.core.decodeBody
import net.ankio.ai.lib.core.postJson
import net.ankio.ai.lib.core.removeThink
import net.ankio.ai.lib.core.runCatchingExceptCancel
import net.ankio.ai.lib.core.userAgent
import net.ankio.ai.lib.model.openai.ApiErrorResponse
import net.ankio.ai.lib.model.openai.ChatCompletionRequest
import net.ankio.ai.lib.model.openai.ChatCompletionResponse
import net.ankio.ai.lib.model.openai.ChatCompletionStreamChunk
import net.ankio.ai.lib.model.openai.ChatMessageFactory
import net.ankio.ai.lib.model.openai.ModelsListResponse
import net.ankio.ai.lib.model.openai.firstContent
import net.ankio.ai.lib.model.openai.firstDeltaContent
import net.ankio.ai.lib.model.openai.resolveMessage
import net.ankio.ai.lib.provider.AiCtx
import net.ankio.ai.lib.provider.ProviderBackend
import net.ankio.ai.lib.provider.ProviderDef
import okhttp3.Request

/**
 * OpenAI Chat Completions 兼容协议后端。
 *
 * 适用于 DeepSeek、OpenAI、国内多数兼容端点等 [ProviderDef] 中 `chatPath` / `modelsPath` 配置项。
 */
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
        return withContext(Dispatchers.IO) {
            runCatchingExceptCancel {
                AiHttp.client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: error("Empty body")
                    if (!response.isSuccessful) error("HTTP ${response.code}: $body")
                    json.decodeBody(body, ModelsListResponse.serializer()).data.map { it.id }
                }
            }.onSuccess { ctx.logD("models ok, count=${it.size}") }
                .getOrElse {
                    ctx.logE("models failed", it)
                    throw it
                }
        }
    }

    override suspend fun chatOnce(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
    ): Result<String> {
        val body = ChatCompletionRequest(
            model = ctx.model,
            messages = ChatMessageFactory.build(system, user, image),
            temperature = ctx.temperature,
            stream = null,
        )
        val request = Request.Builder()
            .url("${ctx.apiUri.trimEnd('/')}${def.chatPath}")
            .addHeader("Authorization", "Bearer ${ctx.apiKey}")
            .addHeader("Content-Type", "application/json")
            .userAgent(ctx)
            .postJson(json, ChatCompletionRequest.serializer(), body)
            .build()

        ctx.logD("POST ${def.chatPath} stream=false messages=${body.messages.size}")
        return withContext(Dispatchers.IO) {
            runCatchingExceptCancel {
                AiHttp.client.newCall(request).execute().use { response ->
                    val text = response.body?.string()?.removeThink() ?: error("Empty body")
                    if (!response.isSuccessful) {
                        error(
                            json.decodeBody(text, ApiErrorResponse.serializer())
                                .resolveMessage(text),
                        )
                    }
                    json.decodeBody(text, ChatCompletionResponse.serializer()).firstContent()
                }
            }
        }
    }

    override suspend fun chatStream(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
        onChunk: (String) -> Unit,
    ): Result<Unit> {
        val body = ChatCompletionRequest(
            model = ctx.model,
            messages = ChatMessageFactory.build(system, user, image),
            temperature = ctx.temperature,
            stream = true,
        )
        val request = Request.Builder()
            .url("${ctx.apiUri.trimEnd('/')}${def.chatPath}")
            .addHeader("Authorization", "Bearer ${ctx.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .userAgent(ctx)
            .postJson(json, ChatCompletionRequest.serializer(), body)
            .build()

        ctx.logD("POST ${def.chatPath} stream=true messages=${body.messages.size}")
        return withContext(Dispatchers.IO) {
            runCatchingExceptCancel {
                stream(ctx, request, onChunk)
            }
        }
    }

    /** 解析 SSE `data:` 行并回调增量文本（须在 IO 线程调用）。 */
    private suspend fun stream(ctx: AiCtx, request: Request, onChunk: (String) -> Unit) {
        AiHttp.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                error("HTTP ${response.code}: $errBody")
            }
            val source = response.body?.source() ?: error("Empty body")
            val ser = ChatCompletionStreamChunk.serializer()
            var chunks = 0
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.substring(6)
                if (data == "[DONE]") break
                runCatchingExceptCancel {
                    json.decodeBody(data, ser).firstDeltaContent()?.let {
                        chunks++
                        onChunk(it)
                    }
                }
            }
            ctx.logD("stream ok, chunks=$chunks")
        }
    }
}
