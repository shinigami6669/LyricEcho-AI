package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class GeminiLyricsResult(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val lines: List<LyricLine>,
    val rawOriginal: String,
    val rawTranslated: String,
    val providerUsed: String = "AI"
)

class GeminiLyricsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
) {

    private val lastRequestTimeMs = AtomicLong(0L)
    var onTerminalLog: ((String) -> Unit)? = null

    private fun logToTerminal(message: String) {
        onTerminalLog?.invoke(message)
    }

    suspend fun pingEndpoint(
        baseUrl: String,
        apiKey: String,
        isGemini: Boolean
    ): Result<EndpointPingResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val cleanBase = baseUrl.trim().removeSuffix("/")
            val url = if (isGemini) {
                if (cleanBase.contains("generativelanguage.googleapis.com")) {
                    "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
                } else {
                    "$cleanBase/v1beta/models?key=${apiKey.trim()}"
                }
            } else {
                "$cleanBase/models"
            }

            val reqBuilder = Request.Builder().url(url)
            if (!isGemini && apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            logToTerminal("> PING $url")
            val response = client.newCall(reqBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val code = response.code
            val body = response.body?.string() ?: ""

            logToTerminal("< PING Response: HTTP $code (${latency}ms)")

            if (response.isSuccessful) {
                var modelsCount = 0
                try {
                    val json = JSONObject(body)
                    if (json.has("data")) {
                        modelsCount = json.getJSONArray("data").length()
                    } else if (json.has("models")) {
                        modelsCount = json.getJSONArray("models").length()
                    }
                } catch (_: Exception) {}

                Result.success(
                    EndpointPingResult(
                        isSuccess = true,
                        latencyMs = latency,
                        httpCode = code,
                        message = "اتصال موفق (HTTP $code) - تاخیر: ${latency}ms",
                        detectedModelsCount = modelsCount
                    )
                )
            } else {
                logToTerminal("❌ Ping Failed: HTTP $code - ${body.take(100)}")
                Result.success(
                    EndpointPingResult(
                        isSuccess = false,
                        latencyMs = latency,
                        httpCode = code,
                        message = "خطا در سرور (HTTP $code): ${body.take(120)}"
                    )
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logToTerminal("❌ Ping Exception (${latency}ms): ${e.message}")
            Result.failure(Exception("خطا در پینگ سرور (${latency}ms): ${e.message}"))
        }
    }

    suspend fun detectAvailableModels(
        baseUrl: String,
        apiKey: String,
        isGemini: Boolean
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().removeSuffix("/")
            val url = if (isGemini) {
                if (cleanBase.contains("generativelanguage.googleapis.com")) {
                    "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
                } else {
                    "$cleanBase/v1beta/models?key=${apiKey.trim()}"
                }
            } else {
                "$cleanBase/models"
            }

            logToTerminal("> GET $url")
            com.example.diagnostics.AppLogger.i(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "درخواست شناسایی خودکار مدل‌ها از سرور: $url"
            )

            val reqBuilder = Request.Builder().url(url)
            if (!isGemini && apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            logToTerminal("< HTTP ${response.code} on models endpoint")

            if (!response.isSuccessful) {
                logToTerminal("❌ Failed to fetch models: HTTP ${response.code} - ${body.take(120)}")
                return@withContext Result.failure(
                    Exception("خطا در دریافت لیست مدل‌ها (HTTP ${response.code}): ${body.take(150)}")
                )
            }

            val modelsList = mutableListOf<String>()
            val root = JSONObject(body)

            if (root.has("data")) {
                // OpenAI format: { "data": [ { "id": "..." } ] }
                val dataArr = root.getJSONArray("data")
                for (i in 0 until dataArr.length()) {
                    val item = dataArr.getJSONObject(i)
                    val id = item.optString("id")
                    if (id.isNotBlank()) {
                        modelsList.add(id)
                    }
                }
            } else if (root.has("models")) {
                // Gemini format: { "models": [ { "name": "models/..." } ] }
                val modelsArr = root.getJSONArray("models")
                for (i in 0 until modelsArr.length()) {
                    val item = modelsArr.getJSONObject(i)
                    val name = item.optString("name").removePrefix("models/")
                    val methods = item.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = if (methods != null) {
                        var found = false
                        for (m in 0 until methods.length()) {
                            if (methods.getString(m) == "generateContent") found = true
                        }
                        found
                    } else true

                    if (supportsGenerate && name.isNotBlank()) {
                        modelsList.add(name)
                    }
                }
            }

            if (modelsList.isEmpty()) {
                Result.failure(Exception("هیچ مدلی در پاسخ سرور یافت نشد."))
            } else {
                com.example.diagnostics.AppLogger.s(
                    com.example.diagnostics.LogTag.GEMINI_AI,
                    "شناسایی خودکار مدل‌ها با موفقیت انجام شد: ${modelsList.size} مدل در دسترس است",
                    "Models: ${modelsList.take(4).joinToString(", ")}..."
                )
                Result.success(modelsList)
            }
        } catch (e: Exception) {
            com.example.diagnostics.AppLogger.e(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "خطا در شناسایی مدل‌های API: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun fetchLyricsAndTranslation(
        title: String,
        artist: String,
        targetLanguage: String,
        settings: ApiEngineSettings,
        userCustomKey: String? = null
    ): Result<GeminiLyricsResult> = withContext(Dispatchers.IO) {
        val effectiveKey = when {
            !userCustomKey.isNullOrBlank() -> userCustomKey.trim()
            settings.apiKey.isNotBlank() -> settings.apiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }

        if (effectiveKey.isBlank() && settings.providerType != ApiProviderType.OLLAMA_LOCAL) {
            return@withContext Result.failure(Exception("کلید API وارد نشده است. لطفاً از بخش تنظیمات، کلید API خود را وارد کنید."))
        }

        // Apply Rate Limiting (RPM)
        if (settings.requestsPerMinute > 0) {
            val minInterval = 60_000L / settings.requestsPerMinute
            val now = System.currentTimeMillis()
            val last = lastRequestTimeMs.get()
            val diff = now - last
            if (diff < minInterval) {
                val waitTime = minInterval - diff
                delay(waitTime)
            }
            lastRequestTimeMs.set(System.currentTimeMillis())
        }

        val prompt = buildLyricsPrompt(title, artist, targetLanguage)
        val maxAttempts = settings.maxRetries.coerceIn(1, 5)

        // Try with Primary Provider
        var lastError: Exception? = null
        for (attempt in 1..maxAttempts) {
            val result = if (settings.providerType.isGeminiNative) {
                executeGeminiNativeRequest(title, artist, targetLanguage, prompt, settings.baseUrl, effectiveKey, settings.selectedModel, settings.temperature)
            } else {
                executeOpenAiCompatibleRequest(title, artist, targetLanguage, prompt, settings.baseUrl, effectiveKey, settings.selectedModel, settings.temperature)
            }

            if (result.isSuccess) {
                return@withContext result
            }

            lastError = result.exceptionOrNull() as? Exception ?: Exception("Unknown API error")
            com.example.diagnostics.AppLogger.w(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "تلاش $attempt از $maxAttempts ناموفق بود: ${lastError.message}",
                if (attempt < maxAttempts) "در حال تلاش مجدد پس از ${attempt * 1000}ms..." else "اتمام تلاش‌های مجاز برای پرووایدر فعلی"
            )

            if (attempt < maxAttempts) {
                delay(attempt * 1000L)
            }
        }

        // Auto-fallback to Backup Google Gemini if enabled
        if (settings.autoFallbackToGemini && !settings.providerType.isGeminiNative) {
            val backupKey = settings.backupGeminiKey.ifBlank {
                com.example.data.preferences.SettingsDataStore.DEFAULT_BACKUP_GEMINI_KEY
            }
            com.example.diagnostics.AppLogger.w(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "پرووایدر اصلی (${settings.providerType.displayName}) در دسترس نیست. سوئیچ خودکار به سرور پشتیبان Google Gemini..."
            )
            val fallbackResult = executeGeminiNativeRequest(
                title = title,
                artist = artist,
                targetLanguage = targetLanguage,
                prompt = prompt,
                baseUrl = "https://generativelanguage.googleapis.com",
                apiKey = backupKey,
                model = "gemini-2.5-flash",
                temperature = 0.3f
            )
            if (fallbackResult.isSuccess) {
                com.example.diagnostics.AppLogger.s(
                    com.example.diagnostics.LogTag.GEMINI_AI,
                    "سوئیچ موفق به سرور پشتیبان Google Gemini: متن با موفقیت دریافت گردید."
                )
                return@withContext fallbackResult
            }
        }

        Result.failure(lastError ?: Exception("Failed to fetch lyrics after retries"))
    }

    private fun buildLyricsPrompt(title: String, artist: String, targetLanguage: String): String {
        return """
            You are an expert music lyricist and precision audio synchronizer. 
            Song Title: "$title"
            Artist: "$artist"
            Target Language: "$targetLanguage"

            Provide the complete authentic lyrics for this song, broken down into sequential lines with REALISTIC, ACCURATE studio audio timestamps:
            - CRITICAL FOR ACCURACY: Account for the real-world instrumental intro (do NOT start line 0 at 0ms unless singing literally starts immediately). Set timeMs to the actual millisecond when the vocal begins in the studio track.
            - Account for pauses between verses, musical bridges, and choruses.
            - Provide an emotionally resonant, accurate translation for every single line in $targetLanguage.
            
            Return STRICT JSON only matching this schema without markdown formatting or extra text:
            {
              "title": "$title",
              "artist": "$artist",
              "durationMs": 180000,
              "lines": [
                {
                  "id": 0,
                  "timeMs": 14000,
                  "original": "First line of original lyrics",
                  "translated": "First line translated into $targetLanguage"
                }
              ]
            }
        """.trimIndent()
    }

    private fun executeOpenAiCompatibleRequest(
        title: String,
        artist: String,
        targetLanguage: String,
        prompt: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        temperature: Float
    ): Result<GeminiLyricsResult> {
        val startTime = System.currentTimeMillis()
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val url = "$cleanBase/chat/completions"

        val maskedKey = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else "***"
        logToTerminal("> POST $url")
        logToTerminal("> Model: $model | Temp: $temperature | Key: $maskedKey")

        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "ارسال درخواست به پرووایدر OpenAI: \"$title\" - $artist",
            "URL: $url | Model: $model | Key: $maskedKey"
        )

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert audio lyrics extractor. Always reply with strict raw JSON according to user schema without markdown formatting.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", temperature.toDouble())
            try {
                put("response_format", JSONObject().put("type", "json_object"))
            } catch (_: Exception) {}
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            logToTerminal("< HTTP ${response.code} (Latency: ${latency}ms)")
            logToTerminal("< Raw Response: ${responseBody.take(1200)}")

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                logToTerminal("❌ API Error: $errorMsg")
                return Result.failure(Exception("OpenAI API Error (HTTP ${response.code}): $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                logToTerminal("❌ No choices returned in OpenAI response.")
                return Result.failure(Exception("No choices returned from AI model."))
            }

            val messageObj = choices.getJSONObject(0).optJSONObject("message")
            val contentStr = messageObj?.optString("content") ?: ""
            parseLyricsJson(contentStr, title, artist, targetLanguage, latency, "OpenAI ($model)")
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logToTerminal("❌ Network Exception (${latency}ms): ${e.message}")
            Log.e("GeminiLyricsService", "Failed OpenAI request in ${latency}ms", e)
            Result.failure(e)
        }
    }

    private fun executeGeminiNativeRequest(
        title: String,
        artist: String,
        targetLanguage: String,
        prompt: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        temperature: Float
    ): Result<GeminiLyricsResult> {
        val startTime = System.currentTimeMillis()
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val actualModel = if (model.startsWith("gemini")) model else "gemini-2.5-flash"
        val url = if (cleanBase.contains("generativelanguage.googleapis.com")) {
            "https://generativelanguage.googleapis.com/v1beta/models/$actualModel:generateContent?key=${apiKey.trim()}"
        } else {
            "$cleanBase/v1beta/models/$actualModel:generateContent?key=${apiKey.trim()}"
        }

        val maskedKey = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else "***"
        logToTerminal("> POST $url")
        logToTerminal("> Gemini Model: $actualModel | Key: $maskedKey")

        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "ارسال درخواست به Google Gemini: \"$title\" - $artist",
            "Model: $actualModel | Key: $maskedKey"
        )

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", temperature.toDouble())
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            logToTerminal("< HTTP ${response.code} (Latency: ${latency}ms)")
            logToTerminal("< Raw Response: ${responseBody.take(1200)}")

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                logToTerminal("❌ Gemini Error: $errorMsg")
                return Result.failure(Exception("Gemini API Error (HTTP ${response.code}): $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                logToTerminal("❌ No candidates returned from Gemini.")
                return Result.failure(Exception("No candidates returned from Gemini."))
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.getJSONObject(0)?.optString("text") ?: ""

            parseLyricsJson(rawText, title, artist, targetLanguage, latency, "Google Gemini ($actualModel)")
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logToTerminal("❌ Gemini Network Exception (${latency}ms): ${e.message}")
            Log.e("GeminiLyricsService", "Failed Gemini request in ${latency}ms", e)
            Result.failure(e)
        }
    }

    private fun parseLyricsJson(
        rawContent: String,
        fallbackTitle: String,
        fallbackArtist: String,
        targetLanguage: String,
        latencyMs: Long,
        providerTag: String
    ): Result<GeminiLyricsResult> {
        val clean = rawContent
            .replace(Regex("(?s)<think>.*?</think>"), "") // Remove reasoning tags from thinking models
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        val jsonString = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean.substring(firstBrace, lastBrace + 1)
        } else {
            clean
        }

        return try {
            val resultJson = JSONObject(jsonString)
            val resultTitle = resultJson.optString("title", fallbackTitle)
            val resultArtist = resultJson.optString("artist", fallbackArtist)
            val resultDuration = resultJson.optLong("durationMs", 180000L)

            val linesArray = resultJson.optJSONArray("lines") ?: JSONArray()
            val parsedLines = mutableListOf<LyricLine>()
            val origSb = StringBuilder()
            val transSb = StringBuilder()

            for (i in 0 until linesArray.length()) {
                val lineObj = linesArray.getJSONObject(i)
                val lineId = lineObj.optInt("id", i)
                val timeMs = lineObj.optLong("timeMs", (i * 4000L))
                val orig = lineObj.optString("original", "").trim()
                val trans = lineObj.optString("translated", "").trim()

                parsedLines.add(LyricLine(id = lineId, timeMs = timeMs, original = orig, translated = trans))
                if (orig.isNotBlank()) origSb.appendLine(orig)
                if (trans.isNotBlank()) transSb.appendLine(trans)
            }

            if (parsedLines.isEmpty()) {
                return Result.failure(Exception("پاسخ هوش مصنوعی حاوی خطوط متن معتبر نبود (خطوط خالی)."))
            }

            com.example.diagnostics.AppLogger.s(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "دریافت موفق لیریک از $providerTag: ${parsedLines.size} خط در ${latencyMs}ms",
                "Title: $resultTitle | Artist: $resultArtist | Duration: ${resultDuration}ms"
            )

            Result.success(
                GeminiLyricsResult(
                    title = resultTitle,
                    artist = resultArtist,
                    durationMs = resultDuration,
                    lines = parsedLines,
                    rawOriginal = origSb.toString().trim(),
                    rawTranslated = transSb.toString().trim(),
                    providerUsed = providerTag
                )
            )
        } catch (e: Exception) {
            com.example.diagnostics.AppLogger.e(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "خطا در تجزیه JSON پاسخ مدل: ${e.message}",
                "Raw: ${rawContent.take(200)}"
            )
            Result.failure(Exception("خطا در تجزیه متن ترانه از پاسخ هوش مصنوعی: ${e.message}"))
        }
    }

    suspend fun translateAuthenticLyrics(
        title: String,
        artist: String,
        lines: List<LyricLine>,
        targetLanguage: String,
        settings: ApiEngineSettings,
        userCustomKey: String? = null
    ): Result<List<LyricLine>> = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext Result.success(emptyList())

        val effectiveKey = when {
            !userCustomKey.isNullOrBlank() -> userCustomKey.trim()
            settings.apiKey.isNotBlank() -> settings.apiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }

        if (effectiveKey.isBlank() && settings.providerType != ApiProviderType.OLLAMA_LOCAL) {
            return@withContext Result.failure(Exception("کلید API وارد نشده است. لطفاً از بخش تنظیمات، کلید API خود را وارد کنید."))
        }

        if (settings.requestsPerMinute > 0) {
            val minInterval = 60_000L / settings.requestsPerMinute
            val now = System.currentTimeMillis()
            val last = lastRequestTimeMs.get()
            val diff = now - last
            if (diff < minInterval) {
                delay(minInterval - diff)
            }
            lastRequestTimeMs.set(System.currentTimeMillis())
        }

        val chunkSize = 20
        val chunks = lines.chunked(chunkSize)
        val allTranslatedStrings = mutableListOf<String>()

        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "آغاز ترجمه ${lines.size} خط با مدل انتخابی: ${settings.selectedModel} (${settings.providerType.displayName})",
            "Song: \"$title\" - $artist | Chunks: ${chunks.size} | Language: $targetLanguage"
        )

        for ((chunkIndex, chunk) in chunks.withIndex()) {
            val numberedLines = chunk.mapIndexed { idx, l -> "${idx + 1}. ${l.original}" }.joinToString("\n")
            val prompt = """
                You are an expert bilingual music and poetry translator.
                Song: "$title" by "$artist"
                Target Language: "$targetLanguage"

                Translate the following authentic lyrics into $targetLanguage line by line.
                Instructions:
                1. Keep the exact line order and line count (${chunk.size} lines).
                2. Provide natural, poetic, and emotional translations in $targetLanguage suited for reading along with the music.
                3. Output STRICT JSON: A JSON array of translated strings ONLY, without markdown code fences:
                [
                  "Translation of line 1",
                  "Translation of line 2"
                ]

                Lines to translate (${chunkIndex + 1}/${chunks.size}):
                $numberedLines
            """.trimIndent()

            val maxAttempts = settings.maxRetries.coerceIn(1, 3)
            var chunkSuccess = false

            for (attempt in 1..maxAttempts) {
                val res = if (settings.providerType.isGeminiNative) {
                    executeGeminiTranslationRequest(title, artist, prompt, settings.baseUrl, effectiveKey, settings.selectedModel, settings.temperature)
                } else {
                    executeOpenAiTranslationRequest(title, artist, prompt, settings.baseUrl, effectiveKey, settings.selectedModel, settings.temperature)
                }

                if (res.isSuccess) {
                    val strings = res.getOrNull() ?: emptyList()
                    if (strings.isNotEmpty()) {
                        allTranslatedStrings.addAll(strings)
                        chunkSuccess = true
                        break
                    }
                }
                if (attempt < maxAttempts) delay(attempt * 800L)
            }

            // Fallback to Google Gemini for this chunk if primary failed
            if (!chunkSuccess && settings.autoFallbackToGemini && !settings.providerType.isGeminiNative) {
                val backupKey = settings.backupGeminiKey.ifBlank {
                    com.example.data.preferences.SettingsDataStore.DEFAULT_BACKUP_GEMINI_KEY
                }
                logToTerminal("⚠️ Fallback to Gemini for chunk ${chunkIndex + 1}/${chunks.size}")
                val fbRes = executeGeminiTranslationRequest(
                    title = title,
                    artist = artist,
                    prompt = prompt,
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = backupKey,
                    model = "gemini-2.5-flash",
                    temperature = 0.2f
                )
                if (fbRes.isSuccess) {
                    val strings = fbRes.getOrNull() ?: emptyList()
                    if (strings.isNotEmpty()) {
                        allTranslatedStrings.addAll(strings)
                        chunkSuccess = true
                    }
                }
            }

            if (!chunkSuccess) {
                // If chunk translation completely failed, fill with empty strings to preserve index alignment
                allTranslatedStrings.addAll(chunk.map { "" })
            }
        }

        val updatedLines = lines.mapIndexed { idx, origLine ->
            val trans = allTranslatedStrings.getOrNull(idx)?.trim()
            origLine.copy(translated = if (!trans.isNullOrBlank()) trans else "")
        }

        val successCount = updatedLines.count { it.translated.isNotBlank() && it.translated != it.original }
        if (successCount > 0) {
            com.example.diagnostics.AppLogger.s(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "ترجمه موفق با مدل «${settings.selectedModel}»: $successCount از ${lines.size} خط ترجمه شد",
                "Model: ${settings.selectedModel} | Provider: ${settings.providerType.displayName}"
            )
            Result.success(updatedLines)
        } else {
            com.example.diagnostics.AppLogger.w(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "مدل «${settings.selectedModel}» ترجمه‌ای برای خطوط بازنگرداند.",
                "Primary and fallbacks returned empty translations"
            )
            Result.failure(Exception("Failed to translate lyrics with ${settings.selectedModel}"))
        }
    }

    private fun executeOpenAiTranslationRequest(
        title: String,
        artist: String,
        prompt: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        temperature: Float
    ): Result<List<String>> {
        val startTime = System.currentTimeMillis()
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val url = "$cleanBase/chat/completions"

        val maskedKey = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else "***"
        logToTerminal("> POST $url (Model: $model)")

        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "ارسال درخواست ترجمه به مدل OpenAI: $model",
            "URL: $url | Key: $maskedKey"
        )

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert bilingual lyricist. Always output a strict JSON array of translated strings without explanation.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", temperature.toDouble().coerceIn(0.1, 0.5))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            logToTerminal("< HTTP ${response.code} from $model in ${latency}ms")

            if (!response.isSuccessful) {
                logToTerminal("❌ Translation failed: HTTP ${response.code} - ${responseBody.take(150)}")
                com.example.diagnostics.AppLogger.w(
                    com.example.diagnostics.LogTag.GEMINI_AI,
                    "خطا در ترجمه با مدل $model (HTTP ${response.code})",
                    responseBody.take(200)
                )
                return Result.failure(Exception("OpenAI Translation HTTP ${response.code}: ${responseBody.take(120)}"))
            }

            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices") ?: return Result.failure(Exception("Empty choices"))
            val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""

            val parsed = parseTranslationArray(content)
            logToTerminal("✓ $model پاسخ ترجمه ${parsed.size} خط را در ${latency}ms تحویل داد.")
            com.example.diagnostics.AppLogger.s(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "دریافت ترجمه موفق از مدل $model (${parsed.size} خط)",
                "Latency: ${latency}ms | Sample: ${parsed.firstOrNull()?.take(40)}..."
            )
            Result.success(parsed)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logToTerminal("❌ Translation exception ($model, ${latency}ms): ${e.message}")
            com.example.diagnostics.AppLogger.e(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "خطا در ارتباط با مدل $model: ${e.message}"
            )
            Result.failure(e)
        }
    }

    private fun executeGeminiTranslationRequest(
        title: String,
        artist: String,
        prompt: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        temperature: Float
    ): Result<List<String>> {
        val startTime = System.currentTimeMillis()
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val actualModel = if (model.startsWith("gemini")) model else "gemini-2.5-flash"
        val url = if (cleanBase.contains("generativelanguage.googleapis.com")) {
            "https://generativelanguage.googleapis.com/v1beta/models/$actualModel:generateContent?key=${apiKey.trim()}"
        } else {
            "$cleanBase/v1beta/models/$actualModel:generateContent?key=${apiKey.trim()}"
        }

        logToTerminal("> POST $url (Gemini Translation)")
        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "ارسال درخواست ترجمه به سرور Google Gemini: $actualModel"
        )

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", temperature.toDouble().coerceIn(0.1, 0.4))
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            logToTerminal("< HTTP ${response.code} from Gemini in ${latency}ms")

            if (!response.isSuccessful) {
                logToTerminal("❌ Gemini translation failed: HTTP ${response.code} - ${responseBody.take(120)}")
                return Result.failure(Exception("Gemini Translation HTTP ${response.code}"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return Result.failure(Exception("Empty candidates"))
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.getJSONObject(0)?.optString("text") ?: ""

            val parsed = parseTranslationArray(rawText)
            logToTerminal("✓ Gemini ترجمه ${parsed.size} خط را در ${latency}ms تحویل داد.")
            Result.success(parsed)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logToTerminal("❌ Gemini translation exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseTranslationArray(raw: String): List<String> {
        var clean = raw
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (clean.contains("<think>")) {
            clean = clean.substringAfter("</think>", clean).trim()
        }

        val firstBracket = clean.indexOf('[')
        val lastBracket = clean.lastIndexOf(']')
        val jsonStr = if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            clean.substring(firstBracket, lastBracket + 1)
        } else {
            clean
        }

        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.get(i)
                if (item is JSONObject) {
                    val text = item.optString("translated", item.optString("text", item.optString("translation", "")))
                    list.add(text.trim())
                } else {
                    list.add(arr.optString(i, "").trim())
                }
            }
            list
        } catch (_: Exception) {
            clean.lines()
                .map { line ->
                    line.replace(Regex("^\\s*\\d+[.):\\]\\-]\\s*"), "")
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .removeSuffix(",")
                        .trim()
                }
                .filter { it.isNotBlank() && !it.startsWith("[") && !it.startsWith("]") }
        }
    }
}
