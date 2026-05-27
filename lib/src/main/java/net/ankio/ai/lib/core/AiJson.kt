package net.ankio.ai.lib.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import net.ankio.ai.lib.provider.AiCtx
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** 模块内共享的 [Json] 实例，用于 OpenAI / Gemini 请求体编解码。 */
object AiJson {
    /** 宽松模式 JSON：忽略未知字段、允许 lenient 解析。 */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }
}

/** 模块内共享的 OkHttp 客户端（长超时，供各 Backend 使用）。 */
internal object AiHttp {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()
}

/**
 * 类似 [runCatching]，但协程 [CancellationException] 会原样抛出，不吞掉取消。
 */
internal suspend inline fun <R> runCatchingExceptCancel(block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/** 移除模型回复中的 `...` 思考块并规整空行。 */
internal fun String.removeThink(): String {
    val regex = Regex("(?si)<think\\b[^>]*?>.*?</think>")
    return replace(regex, "").replace(Regex("\\n{3,}"), "\n\n").trim()
}

/** 为请求添加 `User-Agent: AutoAccounting/{version}`。 */
internal fun Request.Builder.userAgent(ctx: AiCtx) = apply {
    addHeader("User-Agent", "AutoAccounting/${ctx.userAgent}")
}

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/** 将对象序列化为 JSON [okhttp3.RequestBody]。 */
fun <T> Json.encodeBody(value: T, serializer: KSerializer<T>) =
    encodeToString(serializer, value).toRequestBody(JSON_MEDIA)

/** 从 JSON 字符串反序列化对象。 */
fun <T> Json.decodeBody(body: String, serializer: KSerializer<T>): T =
    decodeFromString(serializer, body)

/** 以 JSON 请求体构造 POST [Request.Builder]。 */
fun <T> Request.Builder.postJson(
    json: Json,
    serializer: KSerializer<T>,
    value: T,
): Request.Builder = post(json.encodeBody(value, serializer))
