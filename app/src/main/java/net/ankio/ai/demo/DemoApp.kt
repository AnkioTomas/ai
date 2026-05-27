package net.ankio.ai.demo

import android.app.Application
import net.ankio.theme.ThemeSettings
import net.ankio.theme.toast.ThemeToast

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeSettings.init(this)
        ThemeToast.init(this)
    }
}

