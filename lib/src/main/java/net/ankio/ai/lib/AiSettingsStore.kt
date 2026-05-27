package net.ankio.ai.lib

import android.content.Context
import androidx.core.content.edit

/**
 * 基于 SharedPreferences 的 [AiDataStore] 实现，供 Demo 或简单场景使用。
 */
class AiSettingsStore(context: Context) : AiDataStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = AiJson.json

    override suspend fun getActiveProviderId(): String =
        prefs.getString(KEY_ACTIVE, AiProviders.DEFAULT_ID) ?: AiProviders.DEFAULT_ID

    override suspend fun setActiveProviderId(providerId: String) {
        prefs.edit { putString(KEY_ACTIVE, providerId) }
    }

    override suspend fun getSettings(providerId: String): ProviderSettings? {
        val raw = prefs.getString(keySettings(providerId), null) ?: return null
        return json.decodeFromString(ProviderSettings.serializer(), raw)
    }

    override suspend fun saveSettings(settings: ProviderSettings) {
        prefs.edit {
            putString(keySettings(settings.providerId), json.encodeToString(ProviderSettings.serializer(), settings))
        }
    }

    private fun keySettings(providerId: String) = "settings_$providerId"

    private companion object {
        const val PREFS = "net.ankio.ai.settings"
        const val KEY_ACTIVE = "active_provider"
    }
}
