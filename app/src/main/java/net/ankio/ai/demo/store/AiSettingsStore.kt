package net.ankio.ai.demo.store

import android.content.Context
import androidx.core.content.edit
import net.ankio.ai.lib.AI_DEFAULT_PROVIDER_ID
import net.ankio.ai.lib.core.AiDataStore
import net.ankio.ai.lib.core.ProviderSettings

/** Demo：SharedPreferences 实现的 [AiDataStore]，按字段存储，不做 JSON 序列化。 */
class AiSettingsStore(context: Context) : AiDataStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun getActiveProviderId(): String =
        prefs.getString(KEY_ACTIVE, AI_DEFAULT_PROVIDER_ID) ?: AI_DEFAULT_PROVIDER_ID

    override suspend fun setActiveProviderId(providerId: String) {
        prefs.edit { putString(KEY_ACTIVE, providerId) }
    }

    override suspend fun getApiKey(providerId: String): String =
        prefs.getString(key(providerId, SUFFIX_API_KEY), "") ?: ""

    override suspend fun setApiKey(providerId: String, apiKey: String) {
        prefs.edit { putString(key(providerId, SUFFIX_API_KEY), apiKey) }
    }

    override suspend fun getApiUri(providerId: String): String? =
        prefs.getString(key(providerId, SUFFIX_API_URI), null)

    override suspend fun setApiUri(providerId: String, apiUri: String?) {
        prefs.edit {
            if (apiUri == null) remove(key(providerId, SUFFIX_API_URI))
            else putString(key(providerId, SUFFIX_API_URI), apiUri)
        }
    }

    override suspend fun getModel(providerId: String): String? =
        prefs.getString(key(providerId, SUFFIX_MODEL), null)

    override suspend fun setModel(providerId: String, model: String?) {
        prefs.edit {
            if (model == null) remove(key(providerId, SUFFIX_MODEL))
            else putString(key(providerId, SUFFIX_MODEL), model)
        }
    }

    override suspend fun getVisionEnabled(providerId: String): Boolean =
        prefs.getBoolean(key(providerId, SUFFIX_VISION), true)

    override suspend fun setVisionEnabled(providerId: String, enabled: Boolean) {
        prefs.edit { putBoolean(key(providerId, SUFFIX_VISION), enabled) }
    }

    override suspend fun getTemperature(providerId: String): Double {
        val key = key(providerId, SUFFIX_TEMPERATURE)
        return if (prefs.contains(key)) {
            prefs.getFloat(key, ProviderSettings.DEFAULT_TEMPERATURE.toFloat()).toDouble()
        } else {
            ProviderSettings.DEFAULT_TEMPERATURE
        }
    }

    override suspend fun setTemperature(providerId: String, temperature: Double) {
        prefs.edit { putFloat(key(providerId, SUFFIX_TEMPERATURE), temperature.toFloat()) }
    }

    private fun key(providerId: String, suffix: String) = "settings_${providerId}_$suffix"

    private companion object {
        const val PREFS = "net.ankio.ai.settings"
        const val KEY_ACTIVE = "active_provider"
        const val SUFFIX_API_KEY = "api_key"
        const val SUFFIX_API_URI = "api_uri"
        const val SUFFIX_MODEL = "model"
        const val SUFFIX_VISION = "vision"
        const val SUFFIX_TEMPERATURE = "temperature"
    }
}
