package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.ApiEngineSettings
import com.example.data.remote.ApiProfile
import com.example.data.remote.ApiProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lyricecho_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_BACKUP_GEMINI_KEY = ""

        private fun sanitizeLegacyTestValue(value: String?): String {
            if (value.isNullOrBlank()) return ""
            return value
        }
    }

    private val KEY_TARGET_LANGUAGE = stringPreferencesKey("target_language")
    private val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_gemini_api_key")
    private val KEY_AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
    private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
    private val KEY_AUTO_POPUP = booleanPreferencesKey("auto_popup_on_play")
    private val KEY_SPOTIFY_DETECT = booleanPreferencesKey("spotify_auto_detect")
    private val KEY_APP_MASTER_ENABLED = booleanPreferencesKey("app_master_power_enabled")
    private val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    private val KEY_POPUP_SCALE = stringPreferencesKey("popup_scale_size")
    private val KEY_POPUP_TAP_TO_COLLAPSE = booleanPreferencesKey("popup_tap_to_collapse")

    // API Engine & Provider settings
    private val KEY_PROVIDER_TYPE = stringPreferencesKey("api_provider_type")
    private val KEY_BASE_URL = stringPreferencesKey("api_base_url")
    private val KEY_SELECTED_MODEL = stringPreferencesKey("api_selected_model")
    private val KEY_DISCOVERED_MODELS = stringPreferencesKey("api_discovered_models")
    private val KEY_MAX_RETRIES = intPreferencesKey("api_max_retries")
    private val KEY_TIMEOUT_SECONDS = intPreferencesKey("api_timeout_seconds")
    private val KEY_RPM_LIMIT = intPreferencesKey("api_rpm_limit")
    private val KEY_AUTO_FALLBACK = booleanPreferencesKey("api_auto_fallback")
    private val KEY_BACKUP_KEY = stringPreferencesKey("api_backup_key")
    private val KEY_TEMPERATURE = floatPreferencesKey("api_temperature")
    private val KEY_SAVED_API_PROFILES = stringPreferencesKey("saved_api_profiles_json")
    private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_api_profile_id")

    // Telemetry & Request Counters
    private val KEY_TOTAL_REQUESTS = intPreferencesKey("stats_total_requests")
    private val KEY_SUCCESS_REQUESTS = intPreferencesKey("stats_success_requests")
    private val KEY_FAILED_REQUESTS = intPreferencesKey("stats_failed_requests")

    val targetLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TARGET_LANGUAGE] ?: "Persian"
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        sanitizeLegacyTestValue(prefs[KEY_CUSTOM_API_KEY])
    }

    val isAutoScrollEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SCROLL] ?: true
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_SPEED] ?: 1.0f
    }

    val fontSize: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: "Medium"
    }

    val isAutoPopupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_POPUP] ?: true
    }

    val isSpotifyDetectEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPOTIFY_DETECT] ?: true
    }

    val isAppMasterEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_MASTER_ENABLED] ?: true
    }

    val appThemeMode: Flow<com.example.ui.theme.AppThemeMode> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_APP_THEME_MODE] ?: com.example.ui.theme.AppThemeMode.NEON_OBSIDIAN.id
        com.example.ui.theme.AppThemeMode.fromId(id)
    }

    val popupScale: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_POPUP_SCALE] ?: "Medium"
    }

    val popupTapToCollapseEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_POPUP_TAP_TO_COLLAPSE] ?: true
    }

    val apiEngineSettings: Flow<ApiEngineSettings> = context.dataStore.data.map { prefs ->
        val provId = prefs[KEY_PROVIDER_TYPE] ?: ApiProviderType.GOOGLE_GEMINI.id
        val prov = ApiProviderType.fromId(provId)
        val rawBase = sanitizeLegacyTestValue(prefs[KEY_BASE_URL])
        val base = rawBase.ifBlank { prov.defaultBaseUrl }
        val key = sanitizeLegacyTestValue(prefs[KEY_CUSTOM_API_KEY])
        val rawModel = prefs[KEY_SELECTED_MODEL] ?: ""
        val model = if (rawModel.isBlank() || rawModel.startsWith("oc/") || rawModel.startsWith("nara/")) {
            prov.defaultModel
        } else {
            rawModel
        }
        val rawModels = prefs[KEY_DISCOVERED_MODELS] ?: ""
        val modelsList = if (rawModels.isNotBlank() && !rawModels.contains("nara/step-5")) {
            rawModels.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gpt-4o-mini",
                "deepseek-chat",
                "llama-3.3-70b-versatile"
            )
        }
        val retries = prefs[KEY_MAX_RETRIES] ?: 3
        val timeout = prefs[KEY_TIMEOUT_SECONDS] ?: 45
        val rpm = prefs[KEY_RPM_LIMIT] ?: 30
        val fallback = prefs[KEY_AUTO_FALLBACK] ?: false
        val backup = sanitizeLegacyTestValue(prefs[KEY_BACKUP_KEY])
        val temp = prefs[KEY_TEMPERATURE] ?: 0.3f

        ApiEngineSettings(
            providerType = prov,
            baseUrl = base,
            apiKey = key,
            selectedModel = model,
            discoveredModels = modelsList,
            maxRetries = retries,
            timeoutSeconds = timeout,
            requestsPerMinute = rpm,
            autoFallbackToGemini = fallback,
            backupGeminiKey = backup,
            temperature = temp
        )
    }

    val totalRequests: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOTAL_REQUESTS] ?: 0
    }

    val successRequests: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUCCESS_REQUESTS] ?: 0
    }

    val failedRequests: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAILED_REQUESTS] ?: 0
    }

    suspend fun setTargetLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TARGET_LANGUAGE] = language
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_API_KEY] = key
        }
    }

    suspend fun setAutoScrollEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SCROLL] = enabled
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = size
        }
    }

    suspend fun setAutoPopupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_POPUP] = enabled
        }
    }

    suspend fun setSpotifyDetectEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPOTIFY_DETECT] = enabled
        }
    }

    suspend fun setAppMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_MASTER_ENABLED] = enabled
        }
    }

    suspend fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_THEME_MODE] = mode.id
        }
    }

    suspend fun setPopupScale(scale: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POPUP_SCALE] = scale
        }
    }

    suspend fun setPopupTapToCollapseEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POPUP_TAP_TO_COLLAPSE] = enabled
        }
    }

    suspend fun setApiProviderType(type: ApiProviderType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER_TYPE] = type.id
            if (prefs[KEY_BASE_URL].isNullOrBlank() || prefs[KEY_BASE_URL] == DEFAULT_BASE_URL) {
                prefs[KEY_BASE_URL] = type.defaultBaseUrl
            }
            if (prefs[KEY_SELECTED_MODEL].isNullOrBlank() || prefs[KEY_SELECTED_MODEL] == DEFAULT_MODEL) {
                prefs[KEY_SELECTED_MODEL] = type.defaultModel
            }
        }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = url.trim()
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = model.trim()
        }
    }

    suspend fun setDiscoveredModels(models: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISCOVERED_MODELS] = models.joinToString(",")
        }
    }

    suspend fun setMaxRetries(retries: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_RETRIES] = retries.coerceIn(1, 10)
        }
    }

    suspend fun setTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TIMEOUT_SECONDS] = seconds.coerceIn(10, 120)
        }
    }

    suspend fun setRpmLimit(rpm: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RPM_LIMIT] = rpm
        }
    }

    suspend fun setAutoFallbackToGemini(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_FALLBACK] = enabled
        }
    }

    suspend fun setBackupGeminiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKUP_KEY] = key.trim()
        }
    }

    suspend fun setTemperature(temp: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEMPERATURE] = temp.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun recordRequestResult(isSuccess: Boolean) {
        context.dataStore.edit { prefs ->
            val total = prefs[KEY_TOTAL_REQUESTS] ?: 0
            prefs[KEY_TOTAL_REQUESTS] = total + 1
            if (isSuccess) {
                val succ = prefs[KEY_SUCCESS_REQUESTS] ?: 0
                prefs[KEY_SUCCESS_REQUESTS] = succ + 1
            } else {
                val fail = prefs[KEY_FAILED_REQUESTS] ?: 0
                prefs[KEY_FAILED_REQUESTS] = fail + 1
            }
        }
    }

    suspend fun resetRequestStats() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOTAL_REQUESTS] = 0
            prefs[KEY_SUCCESS_REQUESTS] = 0
            prefs[KEY_FAILED_REQUESTS] = 0
        }
    }

    val savedApiProfiles: Flow<List<ApiProfile>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_API_PROFILES]
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<ApiProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val p = ApiProfile.fromJson(obj)
                    if (p.id != "preset_opencode") {
                        list.add(p.copy(apiKey = sanitizeLegacyTestValue(p.apiKey)))
                    }
                }
                if (list.isEmpty()) ApiProfile.getDefaultProfiles() else list
            } catch (_: Exception) {
                ApiProfile.getDefaultProfiles()
            }
        } else {
            ApiProfile.getDefaultProfiles()
        }
    }

    val activeProfileId: Flow<String> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ACTIVE_PROFILE_ID] ?: "preset_gemini_official"
        if (id == "preset_opencode") "preset_gemini_official" else id
    }

    suspend fun saveApiProfile(profile: ApiProfile) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_SAVED_API_PROFILES]
            val list = if (!raw.isNullOrBlank()) {
                try {
                    val array = JSONArray(raw)
                    val result = mutableListOf<ApiProfile>()
                    for (i in 0 until array.length()) {
                        result.add(ApiProfile.fromJson(array.getJSONObject(i)))
                    }
                    result
                } catch (_: Exception) {
                    ApiProfile.getDefaultProfiles().toMutableList()
                }
            } else {
                ApiProfile.getDefaultProfiles().toMutableList()
            }

            val existingIndex = list.indexOfFirst { it.id == profile.id }
            if (existingIndex >= 0) {
                list[existingIndex] = profile
            } else {
                list.add(profile)
            }

            val jsonArray = JSONArray()
            list.forEach { jsonArray.put(it.toJson()) }
            prefs[KEY_SAVED_API_PROFILES] = jsonArray.toString()
        }
    }

    suspend fun deleteApiProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_SAVED_API_PROFILES]
            if (!raw.isNullOrBlank()) {
                try {
                    val array = JSONArray(raw)
                    val result = mutableListOf<ApiProfile>()
                    for (i in 0 until array.length()) {
                        val p = ApiProfile.fromJson(array.getJSONObject(i))
                        if (p.id != profileId) {
                            result.add(p)
                        }
                    }
                    val jsonArray = JSONArray()
                    result.forEach { jsonArray.put(it.toJson()) }
                    prefs[KEY_SAVED_API_PROFILES] = jsonArray.toString()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun activateApiProfile(profile: ApiProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = profile.id
            prefs[KEY_PROVIDER_TYPE] = profile.providerTypeId
            prefs[KEY_BASE_URL] = profile.baseUrl
            prefs[KEY_CUSTOM_API_KEY] = profile.apiKey
            prefs[KEY_SELECTED_MODEL] = profile.selectedModel
            prefs[KEY_TIMEOUT_SECONDS] = profile.timeoutSeconds
            prefs[KEY_MAX_RETRIES] = profile.maxRetries
            prefs[KEY_RPM_LIMIT] = profile.rpmLimit
        }
    }

    private val KEY_RELEASE_CLEAN_V1 = booleanPreferencesKey("release_clean_v1_done")

    suspend fun purgeLegacyTestCredentialsIfNeeded(): Boolean {
        var didClean = false
        context.dataStore.edit { prefs ->
            if (prefs[KEY_RELEASE_CLEAN_V1] != true) {
                prefs[KEY_CUSTOM_API_KEY] = ""
                prefs[KEY_BACKUP_KEY] = ""
                prefs[KEY_PROVIDER_TYPE] = ApiProviderType.GOOGLE_GEMINI.id
                prefs[KEY_BASE_URL] = ApiProviderType.GOOGLE_GEMINI.defaultBaseUrl
                prefs[KEY_SELECTED_MODEL] = ApiProviderType.GOOGLE_GEMINI.defaultModel
                prefs.remove(KEY_DISCOVERED_MODELS)
                prefs.remove(KEY_SAVED_API_PROFILES)
                prefs[KEY_ACTIVE_PROFILE_ID] = "preset_gemini_official"
                prefs[KEY_TOTAL_REQUESTS] = 0
                prefs[KEY_SUCCESS_REQUESTS] = 0
                prefs[KEY_FAILED_REQUESTS] = 0
                prefs[KEY_RELEASE_CLEAN_V1] = true
                didClean = true
            }
        }
        return didClean
    }
}
