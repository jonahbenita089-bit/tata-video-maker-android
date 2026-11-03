package com.tata.videomaker

import com.tata.videomaker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal data class AssistantTurn(val role: String, val text: String)

internal object AssistantApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(50, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun reply(history: List<AssistantTurn>): String = withContext(Dispatchers.IO) {
        require(history.isNotEmpty()) { "Enter a message first." }
        val messages = JSONArray()
        history.takeLast(20).forEach { turn ->
            val role = if (turn.role.equals("Assistant", ignoreCase = true) || turn.role.equals("model", ignoreCase = true)) "model" else "user"
            messages.put(JSONObject().put("role", role).put("text", turn.text.take(6000)))
        }
        val body = JSONObject().put("messages", messages)
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/assistant/chat")
            .post(body.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val json = try { JSONObject(responseText) } catch (_: Exception) { JSONObject() }
            if (!response.isSuccessful) {
                val detail = json.optString("detail").ifBlank { "Assistant request failed (HTTP ${response.code})." }
                throw IllegalStateException(detail)
            }
            json.optString("reply").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The assistant returned an empty reply.")
        }
    }
}
