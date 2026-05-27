package net.ankio.ai.lib.provider

import net.ankio.ai.lib.core.AiLogger
import net.ankio.ai.lib.core.ProviderSettings
import net.ankio.ai.lib.core.formatLogError
import net.ankio.ai.lib.provider.AiProviders.DEFAULT_ID
import net.ankio.ai.lib.provider.gemini.GeminiBackend
import net.ankio.ai.lib.provider.openai.OpenAiBackend

/**
 * 内置 AI 提供商的静态元数据。
 *
 * 描述默认端点、模型、申请 Key 链接及 OpenAI 兼容路径；
 * 实际请求参数来自 [net.ankio.ai.lib.core.ProviderSettings]。
 */
data class ProviderDef(
    /** 唯一标识，如 `deepseek`、`gemini`。 */
    val id: String,
    /** 设置页展示名称。 */
    val displayName: String,
    /** 默认 API 根地址（无自定义 [ProviderSettings.apiUri] 时使用）。 */
    val defaultApiUri: String,
    /** 默认模型名（无自定义 [ProviderSettings.model] 时使用）。 */
    val defaultModel: String,
    /** 申请 API Key 的网页；空字符串表示不显示外链按钮。 */
    val createKeyUri: String,
    /** OpenAI 兼容：对话接口路径，拼在 [defaultApiUri] 后。 */
    val chatPath: String = "/v1/chat/completions",
    /** OpenAI 兼容：模型列表路径；空表示不请求列表接口。 */
    val modelsPath: String = "/v1/models",
    /** 非空时跳过 models 接口，直接返回此静态列表。 */
    val staticModels: List<String>? = null,
)

/**
 * 单次 API 调用的运行时上下文。
 *
 * 合并 [ProviderDef]、[ProviderSettings] 与日志、UA，并解析有效的 apiUri / model。
 */
internal data class AiCtx(
    val def: ProviderDef,
    val settings: ProviderSettings,
    val logger: AiLogger,
    val userAgent: String,
    /** 全局 HTTP/SOCKS 代理；空字符串表示直连。 */
    val proxy: String = "",
) {
    /** 当前请求的 API Key。 */
    val apiKey get() = settings.apiKey

    /** 生效的 API 根地址（自定义优先，否则 [ProviderDef.defaultApiUri]）。 */
    val apiUri
        get() =
            settings.apiUri?.takeIf { it.isNotBlank() } ?: def.defaultApiUri

    /** 生效的模型名（自定义优先，否则 [ProviderDef.defaultModel]）。 */
    val model get() = settings.model?.takeIf { it.isNotBlank() } ?: def.defaultModel

    /** 是否启用视觉。 */
    val visionEnabled get() = settings.visionEnabled

    /** 本次请求使用的采样温度（来自 [ProviderSettings]）。 */
    val temperature get() = settings.temperature

    /** 日志标签 `Ai/{id}`。 */
    val tag get() = "Ai/${def.id}"

    /** 输出 debug 日志。 */
    fun logD(msg: String) = logger.debug(tag, msg)

    /** 输出 error 日志（文案含 [Throwable.displayMessage]）。 */
    fun logE(msg: String, t: Throwable? = null) = logger.error(tag, formatLogError(msg, t), t)
}

/** 某一协议族（OpenAI 兼容 / Gemini）的后端实现。 */
internal interface ProviderBackend {
    /** 绑定的提供商定义。 */
    val def: ProviderDef

    /** 拉取可用模型 id 列表。 */
    suspend fun models(ctx: AiCtx): List<String>

    /**
     * 非流式对话：返回完整回复文本。
     */
    suspend fun chatOnce(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
    ): Result<String>

    /**
     * 流式对话：通过 [onChunk] 逐段接收增量文本。
     *
     * 返回值仅用于表达“请求/解析是否成功”；流式内容由 [onChunk] 推送。
     */
    suspend fun chatStream(
        ctx: AiCtx,
        system: String,
        user: String,
        image: String,
        onChunk: (String) -> Unit,
    ): Result<Unit>
}

/** 内置提供商注册表与 [ProviderBackend] 工厂。 */
internal object AiProviders {

    /** 默认提供商 id（DeepSeek）。 */
    const val DEFAULT_ID: String = "deepseek"

    private fun openAi(
        id: String,
        displayName: String,
        defaultApiUri: String,
        defaultModel: String,
        createKeyUri: String,
        chatPath: String = "/v1/chat/completions",
        modelsPath: String = "/v1/models",
        staticModels: List<String>? = null,
    ) = ProviderDef(
        id = id,
        displayName = displayName,
        defaultApiUri = defaultApiUri,
        defaultModel = defaultModel,
        createKeyUri = createKeyUri,
        chatPath = chatPath,
        modelsPath = modelsPath,
        staticModels = staticModels,
    )

    // ── 已有 ──────────────────────────────────────────────────────────
    val DEEPSEEK = openAi(
        "deepseek",
        "DeepSeek",
        "https://api.deepseek.com",
        "deepseek-chat",
        "https://platform.deepseek.com/api-keys"
    )
    val CHATGPT = openAi(
        "chatgpt",
        "ChatGPT",
        "https://api.openai.com",
        "gpt-4o-mini",
        "https://platform.openai.com/api-keys"
    )
    val KIMI = openAi(
        "kimi",
        "Kimi",
        "https://api.moonshot.cn",
        "moonshot-v1-8k",
        "https://platform.moonshot.cn/console/api-keys"
    )
    val BIGMODEL = openAi(
        "bigmodel", "智谱清言", "https://open.bigmodel.cn/api", "glm-4.5",
        "https://bigmodel.cn/usercenter/proj-mgmt/apikeys",
        chatPath = "/paas/v4/chat/completions",
        modelsPath = "/paas/v4/models",
        staticModels = listOf(
            "glm-4.5", "glm-4.5-air", "glm-4.5-x", "glm-4.5-airx", "glm-4.5-flash",
            "glm-4-plus", "glm-4-air-250414", "glm-4-airx", "glm-4-flashx",
            "glm-4-flashx-250414", "glm-z1-air", "glm-z1-airx", "glm-z1-flash", "glm-z1-flashx",
        ),
    )
    val OPENROUTER = openAi(
        "openrouter",
        "OpenRouter",
        "https://openrouter.ai/api",
        "openai/gpt-4o-mini",
        "https://openrouter.ai/keys"
    )
    val QWEN = openAi(
        "qwen",
        "通义千问",
        "https://dashscope.aliyuncs.com/compatible-mode",
        "qwen-plus",
        "https://bailian.console.aliyun.com/"
    )
    val SILICONFLOW = openAi(
        "siliconflow",
        "硅基流动",
        "https://api.siliconflow.cn",
        "deepseek-ai/DeepSeek-V3",
        "https://cloud.siliconflow.cn/account/ak"
    )
    val MIMO = openAi(
        "mimo",
        "小米 MiMo",
        "https://api.xiaomimimo.com",
        "mimo-v2-flash",
        "https://platform.xiaomimimo.com/"
    )

    // ── 新增：国内 ────────────────────────────────────────────────────
    val DOUBAO = openAi(
        "doubao",
        "豆包",
        "https://ark.cn-beijing.volces.com/api/v3",
        "doubao-1-5-pro-32k",
        "https://console.volcengine.com/ark",
        chatPath = "/chat/completions",
        modelsPath = "/models",
    )
    val MINIMAX = openAi(
        "minimax", "MiniMax", "https://api.minimaxi.com/v1", "MiniMax-Text-01",
        "https://platform.minimaxi.com/user-center/basic-information/interface-key",
    )
    val STEP = openAi(
        "step", "阶跃星辰", "https://api.stepfun.com/v1", "step-2-16k",
        "https://platform.stepfun.com/interface-key",
    )
    val YI = openAi(
        "yi", "零一万物", "https://api.lingyiwanwu.com/v1", "yi-lightning",
        "https://platform.lingyiwanwu.com/apikeys",
    )
    val BAICHUAN = openAi(
        "baichuan", "百川智能", "https://api.baichuan-ai.com/v1", "Baichuan4-Turbo",
        "https://platform.baichuan-ai.com/console/apikey",
    )

    // ── 新增：海外 / 聚合 ─────────────────────────────────────────────
    val MISTRAL = openAi(
        "mistral",
        "Mistral",
        "https://api.mistral.ai/v1",
        "mistral-small-latest",
        "https://console.mistral.ai/api-keys/"
    )
    val GROQ = openAi(
        "groq",
        "Groq",
        "https://api.groq.com/openai/v1",
        "llama-3.3-70b-versatile",
        "https://console.groq.com/keys"
    )
    val TOGETHER = openAi(
        "together",
        "Together AI",
        "https://api.together.xyz/v1",
        "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        "https://api.together.xyz/settings/api-keys"
    )
    val GITHUB = openAi(
        "github", "GitHub Models", "https://models.inference.ai.azure.com", "gpt-4o-mini",
        "https://github.com/settings/tokens",
    )
    val NVIDIA = openAi(
        "nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1", "meta/llama-3.1-8b-instruct",
        "https://build.nvidia.com/",
    )

    // ── 新增：本地 / 企业 ───────────────────────────────────────────────
    val OLLAMA = openAi(
        "ollama", "Ollama", "http://127.0.0.1:11434/v1", "llama3.2",
        "https://ollama.com/download",
    )
    val LMSTUDIO = openAi(
        "lmstudio", "LM Studio", "http://127.0.0.1:1234/v1", "local-model",
        "https://lmstudio.ai/",
    )

    /**
     * Azure：请在设置里把 apiUri 设为
     * `https://{资源名}.openai.azure.com/openai/deployments/{部署名}`
     * 并把 chatPath 设为 `/chat/completions?api-version=2024-08-01-preview`（通过自定义 ProviderSettings 无法改 path，
     * 因此默认写在 Def 里；若部署 URI 已含完整路径可只填 apiUri）。
     */
    val AZURE = openAi(
        "azure",
        "Azure OpenAI",
        "https://YOUR_RESOURCE.openai.azure.com/openai/deployments/YOUR_DEPLOYMENT",
        "gpt-4o",
        "https://portal.azure.com/",
        chatPath = "/chat/completions?api-version=2024-08-01-preview",
        modelsPath = "/models?api-version=2024-08-01-preview",
        staticModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "o3-mini"),
    )

    /** 任意 OpenAI 兼容端点：默认指向 Ollama，用户改 apiUri / model 即可 */
    val CUSTOM = openAi(
        "custom",
        "自定义",
        "http://127.0.0.1:11434/v1",
        "",
        "",
    )

    // ── 非 OpenAI 协议 ─────────────────────────────────────────────────
    val GEMINI = ProviderDef(
        id = "gemini",
        displayName = "Gemini",
        defaultApiUri = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "models/gemini-2.0-flash",
        createKeyUri = "https://aistudio.google.com/app/apikey",
        chatPath = "",
        modelsPath = "",
    )

    private val openAiDefs = listOf(
        DEEPSEEK, CHATGPT, KIMI, BIGMODEL, OPENROUTER, QWEN, SILICONFLOW, MIMO,
        DOUBAO, MINIMAX, STEP, YI, BAICHUAN,
        MISTRAL, GROQ, TOGETHER, GITHUB, NVIDIA,
        OLLAMA, LMSTUDIO, AZURE, CUSTOM,
    )

    /** 所有内置提供商（OpenAI 兼容 + Gemini）。 */
    val all: List<ProviderDef> = openAiDefs + GEMINI

    private val backends: Map<String, ProviderBackend> = buildMap {
        openAiDefs.forEach { put(it.id, OpenAiBackend(it)) }
        put(GEMINI.id, GeminiBackend(GEMINI))
    }

    /**
     * 按 id 查找 [ProviderDef]。
     *
     * @throws IllegalStateException 未知 id。
     */
    fun def(id: String): ProviderDef =
        all.firstOrNull { it.id == id } ?: error("Unknown provider: $id")

    /**
     * 按 id 获取 [ProviderBackend]；未知 id 时回落到 [DEFAULT_ID] 对应后端。
     */
    fun backend(id: String): ProviderBackend = backends[id] ?: backends[DEFAULT_ID]!!
}
