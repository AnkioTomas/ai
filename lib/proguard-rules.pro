# lib 模块本地 release 构建（若开启 isMinifyEnabled）时与 consumer 共用策略。
# 发布到 JitPack 的 AAR 默认不混淆；宿主 release 合并 consumer-rules.pro。

-include consumer-rules.pro

# 允许混淆内部实现，仅保留对外契约（见 consumer-rules.pro）
-keep,allowobfuscation class net.ankio.ai.lib.provider.** { *; }
-keep,allowobfuscation class net.ankio.ai.lib.core.AiJson { *; }
-keep,allowobfuscation class net.ankio.ai.lib.core.AiHttp { *; }
