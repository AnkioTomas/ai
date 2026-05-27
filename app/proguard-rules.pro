# Demo App — release R8 规则（lib 的 consumer-rules.pro 会自动合并）

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Application / Activity / 宿主 DataStore 实现
-keep class net.ankio.ai.demo.DemoApp { *; }
-keep class net.ankio.ai.demo.MainActivity { *; }
-keep class net.ankio.ai.demo.store.** { *; }

# Demo UI（Compose 入口与 Tab）
-keep class net.ankio.ai.demo.** { *; }
