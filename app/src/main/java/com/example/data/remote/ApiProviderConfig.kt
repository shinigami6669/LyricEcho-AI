package com.example.data.remote

enum class ApiProviderType(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val isGeminiNative: Boolean
) {
    GOOGLE_GEMINI(
        id = "google_gemini",
        displayName = "Google Gemini (Official)",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        defaultModel = "gemini-flash-lite-latest",
        isGeminiNative = true
    ),
    OPENAI_OFFICIAL(
        id = "openai_official",
        displayName = "OpenAI Official",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        isGeminiNative = false
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        isGeminiNative = false
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek Official",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        isGeminiNative = false
    ),
    GROQ(
        id = "groq",
        displayName = "Groq High-Speed",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        isGeminiNative = false
    ),
    CUSTOM_OPENAI(
        id = "custom_openai",
        displayName = "Custom Endpoint (OpenAI API)",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        isGeminiNative = false
    ),
    OLLAMA_LOCAL(
        id = "ollama_local",
        displayName = "Ollama / Local LLM",
        defaultBaseUrl = "http://localhost:11434/v1",
        defaultModel = "llama3:latest",
        isGeminiNative = false
    );

    companion object {
        fun fromId(id: String): ApiProviderType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE_GEMINI
        }
    }
}

data class ApiEngineSettings(
    val providerType: ApiProviderType = ApiProviderType.GOOGLE_GEMINI,
    val baseUrl: String = ApiProviderType.GOOGLE_GEMINI.defaultBaseUrl,
    val apiKey: String = "",
    val selectedModel: String = ApiProviderType.GOOGLE_GEMINI.defaultModel,
    val discoveredModels: List<String> = listOf(
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gpt-4o-mini",
        "deepseek-chat",
        "llama-3.3-70b-versatile"
    ),
    val maxRetries: Int = 3,
    val timeoutSeconds: Int = 45,
    val requestsPerMinute: Int = 30, // 0 means unlimited
    val autoFallbackToGemini: Boolean = false,
    val backupGeminiKey: String = "",
    val temperature: Float = 0.3f
)

data class EndpointPingResult(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val httpCode: Int,
    val message: String,
    val detectedModelsCount: Int = 0
)

data class ApiProfile(
    val id: String,
    val name: String,
    val providerTypeId: String = ApiProviderType.GOOGLE_GEMINI.id,
    val baseUrl: String = ApiProviderType.GOOGLE_GEMINI.defaultBaseUrl,
    val apiKey: String = "",
    val selectedModel: String = ApiProviderType.GOOGLE_GEMINI.defaultModel,
    val timeoutSeconds: Int = 30,
    val maxRetries: Int = 3,
    val rpmLimit: Int = 30,
    val isSystemPreset: Boolean = false
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject().apply {
        put("id", id)
        put("name", name)
        put("providerTypeId", providerTypeId)
        put("baseUrl", baseUrl)
        put("apiKey", apiKey)
        put("selectedModel", selectedModel)
        put("timeoutSeconds", timeoutSeconds)
        put("maxRetries", maxRetries)
        put("rpmLimit", rpmLimit)
        put("isSystemPreset", isSystemPreset)
    }

    companion object {
        fun fromJson(json: org.json.JSONObject): ApiProfile {
            return ApiProfile(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Custom Profile"),
                providerTypeId = json.optString("providerTypeId", ApiProviderType.GOOGLE_GEMINI.id),
                baseUrl = json.optString("baseUrl", ApiProviderType.GOOGLE_GEMINI.defaultBaseUrl),
                apiKey = json.optString("apiKey", ""),
                selectedModel = json.optString("selectedModel", ApiProviderType.GOOGLE_GEMINI.defaultModel),
                timeoutSeconds = json.optInt("timeoutSeconds", 30),
                maxRetries = json.optInt("maxRetries", 3),
                rpmLimit = json.optInt("rpmLimit", 30),
                isSystemPreset = json.optBoolean("isSystemPreset", false)
            )
        }

        fun getDefaultProfiles(): List<ApiProfile> = listOf(
            ApiProfile(
                id = "preset_gemini_official",
                name = "Google Gemini (Official)",
                providerTypeId = ApiProviderType.GOOGLE_GEMINI.id,
                baseUrl = "https://generativelanguage.googleapis.com",
                apiKey = "",
                selectedModel = "gemini-flash-lite-latest",
                timeoutSeconds = 30,
                isSystemPreset = true
            ),
            ApiProfile(
                id = "preset_openai_official",
                name = "OpenAI Official",
                providerTypeId = ApiProviderType.OPENAI_OFFICIAL.id,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                selectedModel = "gpt-4o-mini",
                timeoutSeconds = 30,
                isSystemPreset = true
            ),
            ApiProfile(
                id = "preset_openrouter",
                name = "OpenRouter AI",
                providerTypeId = ApiProviderType.OPENROUTER.id,
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "",
                selectedModel = "meta-llama/llama-3.3-70b-instruct:free",
                timeoutSeconds = 35,
                isSystemPreset = true
            ),
            ApiProfile(
                id = "preset_deepseek",
                name = "DeepSeek Official",
                providerTypeId = ApiProviderType.DEEPSEEK.id,
                baseUrl = "https://api.deepseek.com/v1",
                apiKey = "",
                selectedModel = "deepseek-chat",
                timeoutSeconds = 30,
                isSystemPreset = true
            ),
            ApiProfile(
                id = "preset_groq",
                name = "Groq Cloud",
                providerTypeId = ApiProviderType.GROQ.id,
                baseUrl = "https://api.groq.com/openai/v1",
                apiKey = "",
                selectedModel = "llama-3.3-70b-versatile",
                timeoutSeconds = 20,
                isSystemPreset = true
            ),
            ApiProfile(
                id = "preset_custom_openai",
                name = "Custom OpenAI Endpoint",
                providerTypeId = ApiProviderType.CUSTOM_OPENAI.id,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                selectedModel = "gpt-4o-mini",
                timeoutSeconds = 30,
                isSystemPreset = true
            )
        )
    }
}
