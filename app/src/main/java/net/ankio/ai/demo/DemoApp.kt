package net.ankio.ai.demo

import android.app.Application
import net.ankio.theme.ThemeSettings

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeSettings.init(this)
    }
}

