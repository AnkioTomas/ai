package net.ankio.ai.lib.core

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/** 解析后的 HTTP/SOCKS 代理配置。 */
internal data class ParsedProxy(
    val proxy: Proxy,
    val authenticator: Authenticator?,
)

/**
 * 解析代理地址字符串。
 *
 * 支持：
 * - `host:port`（默认 HTTP）
 * - `http://host:port`、`https://host:port`
 * - `socks5://host:port`、`socks://host:port`
 * - 含账号：`http://user:pass@host:port`
 *
 * 空白或无法解析时返回 `null`（直连）。
 */
internal fun parseProxySpec(spec: String): ParsedProxy? {
    val trimmed = spec.trim()
    if (trimmed.isEmpty()) return null

    return runCatching {
        if ("://" in trimmed) {
            parseUrlProxy(trimmed)
        } else {
            parseHostPortProxy(trimmed)
        }
    }.getOrNull()
}

private fun parseHostPortProxy(spec: String): ParsedProxy {
    val at = spec.lastIndexOf('@')
    val (auth, hostPort) = if (at > 0) {
        spec.substring(0, at) to spec.substring(at + 1)
    } else {
        null to spec
    }
    val colon = hostPort.lastIndexOf(':')
    require(colon > 0) { "invalid proxy host:port" }
    val host = hostPort.substring(0, colon)
    val port = hostPort.substring(colon + 1).toInt()
    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
    return ParsedProxy(proxy, auth?.let { credentialsAuthenticator(it) })
}

private fun parseUrlProxy(spec: String): ParsedProxy {
    val url = URL(spec)
    val type = when (url.protocol.lowercase()) {
        "http", "https" -> Proxy.Type.HTTP
        "socks", "socks5" -> Proxy.Type.SOCKS
        else -> error("unsupported proxy scheme: ${url.protocol}")
    }
    val port = when {
        url.port != -1 -> url.port
        type == Proxy.Type.SOCKS -> 1080
        url.protocol.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
    val proxy = Proxy(type, InetSocketAddress(url.host, port))
    val authenticator =
        url.userInfo?.takeIf { it.isNotBlank() }?.let { credentialsAuthenticator(it) }
    return ParsedProxy(proxy, authenticator)
}

private fun credentialsAuthenticator(userInfo: String): Authenticator {
    val parts = userInfo.split(":", limit = 2)
    val user = parts[0]
    val password = if (parts.size > 1) parts[1] else ""
    return proxyAuthenticator(user, password)
}

private fun proxyAuthenticator(username: String, password: String): Authenticator =
    Authenticator { _: Route?, response: Response ->
        if (response.request.header("Proxy-Authorization") != null) {
            return@Authenticator null
        }
        response.request.newBuilder()
            .header("Proxy-Authorization", Credentials.basic(username, password))
            .build()
    }
