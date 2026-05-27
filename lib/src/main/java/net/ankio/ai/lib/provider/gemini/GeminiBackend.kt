package net.ankio.ai.lib.provider.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.ai.lib.core.AiHttp
import net.ankio.ai.lib.core.AiJson
import net.ankio.ai.lib.core.decodeBody
import net.ankio.ai.lib.core.postJson
import net.ankio.ai.lib.core.removeThink
import net.ankio.ai.lib.core.runCatchingExceptCancel
import net.ankio.ai.lib.core.userAgent
import net.ankio.ai.lib.model.gemini.GeminiGenerateRequest
import net.ankio.ai.lib.model.gemini.GeminiGenerateResponse
import net.ankio.ai.lib.model.gemini.GeminiGenerationConfig
import net.ankio.ai.lib.model.gemini.GeminiModelsResponse
import net.ankio.ai.lib.model.gemini.GeminiRequestFactory
import net.ankio.ai.lib.model.gemini.firstText
import net.ankio.ai.lib.provider.AiCtx
import net.ankio.ai.lib.provider.ProviderBackend
import net.ankio.ai.lib.provider.ProviderDef
import okhttp3.Request

/**
 * Google Gemini `v1beta` 协议后端。
 *
 * 使用 `generateContent` / `streamGenerateContent` 及 API Key 查询参数鉴权。
 */
internal class GeminiBackend(override val def: ProviderDef) : ProviderBackend {

    private val json get() = AiJson.json

    /** 保证 base URL 以 `/v1beta` 结尾。 */
    private fun baseUri(ctx: AiCtx): String {
        val trimmed = ctx.apiUri.trimEnd('/')
        return if (trimmed.endsWith("/v1beta")) trimmed else "$trimmed/v1beta"
    }

    override suspend fun models(ctx: AiCtx): List<String> = withContext(Dispatchers.IO) {
        ctx.logD("fetch models")
        val request = Request.Builder()
            .url("${baseUri(ctx)}?key=${ctx.apiKey}")
            .userAgent(ctx)
            .get()
            .build()
        runCatchingExceptCancel {
            AiHttp.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: error("Empty body")
                if (!response.isSuccessful) error("HTTP ${response.code}: $body")
                json.decodeBody(body, GeminiModelsResponse.serializer()).models.map { it.name }
            }
        }.onSuccess { ctx.logD("models ok, count=${it.size}") }
            .getOrElse {
                ctx.logE("models failed", it)
                throw it
            }
    }

    override suspend fun chatOnce(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val path = "generateContent"
        val generationConfig = GeminiGenerationConfig(temperature = ctx.temperature)
        ctx.logD("POST :$path stream=false model=${ctx.model}")
        val request = Request.Builder()
            .url("${baseUri(ctx)}/${ctx.model}:$path")
            .userAgent(ctx)
            .postJson(
                json,
                GeminiGenerateRequest.serializer(),
                GeminiRequestFactory.build(system, user, image, generationConfig),
            )
            .addHeader("x-goog-api-key", ctx.apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        val respSer = GeminiGenerateResponse.serializer()

        runCatchingExceptCancel {
            AiHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.body?.string()}")
                val body = response.body?.string()?.removeThink() ?: error("Empty body")
                parse(body, respSer) ?: error("Empty AI response")
            }
        }
    }

    override suspend fun chatStream(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
        onChunk: (String) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "streamGenerateContent?alt=sse"
        val generationConfig = GeminiGenerationConfig(temperature = ctx.temperature)
        ctx.logD("POST :$path stream=true model=${ctx.model}")
        val request = Request.Builder()
            .url("${baseUri(ctx)}/${ctx.model}:$path")
            .userAgent(ctx)
            .postJson(
                json,
                GeminiGenerateRequest.serializer(),
                GeminiRequestFactory.build(system, user, image, generationConfig),
            )
            .addHeader("x-goog-api-key", ctx.apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        val respSer = GeminiGenerateResponse.serializer()

        runCatchingExceptCancel {
            AiHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.body?.string()}")
                var chunks = 0
                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.substring(6)
                        if (data.isNotBlank() && data != "[DONE]") {
                            parse(data, respSer)?.let {
                                chunks++
                                onChunk(it)
                            }
                        }
                    }
                }
                ctx.logD("stream ok, chunks=$chunks")
            }
        }
    }

    /** 从单条 SSE/JSON 片段解析文本。 */
    private fun parse(
        body: String,
        ser: kotlinx.serialization.KSerializer<GeminiGenerateResponse>,
    ): String? = runCatching { json.decodeBody(body, ser).firstText() }.getOrNull()
}
