package net.ankio.ai.lib

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AiJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }
}

internal object AiHttp {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()
}

internal suspend inline fun <R> runCatchingExceptCancel(block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

internal fun String.removeThink(): String {
    val regex = Regex("(?si)<think\\b[^>]*?>.*?</think>")
    return replace(regex, "").replace(Regex("\\n{3,}"), "\n\n").trim()
}

internal fun Request.Builder.userAgent(ctx: AiCtx) = apply {
    addHeader("User-Agent", "AutoAccounting/${ctx.userAgent}")
}

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

fun <T> Json.encodeBody(value: T, serializer: KSerializer<T>) =
    encodeToString(serializer, value).toRequestBody(JSON_MEDIA)

fun <T> Json.decodeBody(body: String, serializer: KSerializer<T>): T =
    decodeFromString(serializer, body)

fun <T> Request.Builder.postJson(json: Json, serializer: KSerializer<T>, value: T): Request.Builder =
    post(json.encodeBody(value, serializer))
