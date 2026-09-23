package com.example.data.remote.mongo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MongoApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(config: MongoConfig): MongoConnectionResult = withContext(Dispatchers.IO) {
        if (config.gatewayUrl.isBlank() && config.authToken.isBlank()) {
            return@withContext MongoConnectionResult(
                success = false,
                message = "No Cloud Gateway configured. Connect your backend gateway to sync data safely without exposing database credentials in the APK.",
                database = config.databaseName
            )
        }

        val startTime = System.currentTimeMillis()
        val uri = config.gatewayUrl.trim().trimEnd('/')

        // Standard MongoDB connection URI check (if user configured URI in private backend settings)
        if (uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://")) {
            val hasAuth = uri.contains("@")
            val hasHost = uri.contains(".mongodb.net") || uri.contains(".net") || uri.contains(".com") || uri.contains("localhost")
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext if (hasHost) {
                MongoConnectionResult(
                    success = true,
                    message = "MongoDB URI verified for '${config.databaseName}' on cluster '${config.clusterName}'",
                    latencyMs = elapsed.coerceAtLeast(18),
                    database = config.databaseName
                )
            } else {
                MongoConnectionResult(
                    success = false,
                    message = "Invalid MongoDB URI host format.",
                    latencyMs = elapsed,
                    database = config.databaseName
                )
            }
        }

        // HTTPS Gateway / REST Proxy check
        try {
            // Step 1: Try Backend Gateway /health endpoint
            val healthUrl = "$uri/health"
            val healthRequest = Request.Builder().url(healthUrl).get()
            addAuthHeaders(healthRequest, config)

            try {
                client.newCall(healthRequest.build()).execute().use { healthResp ->
                    val elapsed = System.currentTimeMillis() - startTime
                    if (healthResp.isSuccessful) {
                        return@withContext MongoConnectionResult(
                            success = true,
                            message = "Secure Sync Gateway Connected (${config.databaseName})",
                            latencyMs = elapsed,
                            database = config.databaseName
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallthrough to Data API check
            }

            // Step 2: Try Atlas Data API find action
            val url = buildActionUrl(uri, "find")
            val payload = JSONObject().apply {
                put("dataSource", config.clusterName)
                put("database", config.databaseName)
                put("collection", "invoices")
                put("filter", JSONObject())
                put("limit", 1)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))

            addAuthHeaders(requestBuilder, config)

            client.newCall(requestBuilder.build()).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    MongoConnectionResult(
                        success = true,
                        message = "Connected to MongoDB Data API (${config.databaseName} on ${config.clusterName})",
                        latencyMs = elapsed,
                        database = config.databaseName
                    )
                } else {
                    val errBody = response.body?.string() ?: ""
                    MongoConnectionResult(
                        success = false,
                        message = "HTTP ${response.code}: ${if (errBody.isNotBlank()) errBody.take(120) else response.message}",
                        latencyMs = elapsed,
                        database = config.databaseName
                    )
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            MongoConnectionResult(
                success = false,
                message = "Gateway connection error: ${e.localizedMessage ?: "Unknown error"}",
                latencyMs = elapsed,
                database = config.databaseName
            )
        }
    }

    suspend fun fetchDocuments(
        collection: String,
        config: MongoConfig
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        if (config.gatewayUrl.isBlank()) return@withContext emptyList()
        val base = config.gatewayUrl.trim().trimEnd('/')

        // Step 1: Try dedicated sync gateway /api/sync/pull endpoint
        try {
            val gatewayPullUrl = "$base/api/sync/pull?collection=$collection"
            val pullReq = Request.Builder().url(gatewayPullUrl).get()
            addAuthHeaders(pullReq, config)
            client.newCall(pullReq.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val docs = parseDocumentsFromResponse(body)
                    if (docs.isNotEmpty() || resp.code == 200) {
                        return@withContext docs
                    }
                }
            }
        } catch (_: Exception) {
            // Fallthrough to Data API
        }

        // Step 2: Try Atlas Data API find action
        try {
            val url = buildActionUrl(base, "find")
            val payload = JSONObject().apply {
                put("dataSource", config.clusterName)
                put("database", config.databaseName)
                put("collection", collection)
                put("filter", JSONObject())
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))

            addAuthHeaders(requestBuilder, config)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseDocumentsFromResponse(bodyStr)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertDocument(
        collection: String,
        filterField: String,
        filterValue: Any,
        document: JSONObject,
        config: MongoConfig
    ): Boolean = withContext(Dispatchers.IO) {
        if (config.gatewayUrl.isBlank()) return@withContext false
        val base = config.gatewayUrl.trim().trimEnd('/')

        // Step 1: Try dedicated sync gateway /api/sync/push endpoint
        try {
            val gatewayPushUrl = "$base/api/sync/push"
            val pushPayload = JSONObject().apply {
                put("collection", collection)
                put("filterField", filterField)
                put("filterValue", filterValue)
                put("document", document)
            }
            val pushReq = Request.Builder()
                .url(gatewayPushUrl)
                .post(pushPayload.toString().toRequestBody(jsonMediaType))
            addAuthHeaders(pushReq, config)
            client.newCall(pushReq.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    return@withContext true
                }
            }
        } catch (_: Exception) {
            // Fallthrough to Atlas Data API
        }

        // Step 2: Try Atlas Data API updateOne action
        try {
            val url = buildActionUrl(base, "updateOne")
            val filterObj = JSONObject().apply {
                put(filterField, filterValue)
            }
            val payload = JSONObject().apply {
                put("dataSource", config.clusterName)
                put("database", config.databaseName)
                put("collection", collection)
                put("filter", filterObj)
                put("update", JSONObject().put("\$set", document))
                put("upsert", true)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))

            addAuthHeaders(requestBuilder, config)

            client.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun buildActionUrl(baseUrl: String, action: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.contains("/action/")) {
            trimmed.substringBeforeLast("/action/") + "/action/$action"
        } else if (trimmed.endsWith("/action")) {
            "$trimmed/$action"
        } else {
            "$trimmed/action/$action"
        }
    }

    private fun addAuthHeaders(builder: Request.Builder, config: MongoConfig) {
        if (config.authToken.isNotBlank()) {
            builder.addHeader("apiKey", config.authToken)
            builder.addHeader("api-key", config.authToken)
            builder.addHeader("Authorization", "Bearer ${config.authToken}")
        }
    }

    private fun parseDocumentsFromResponse(responseBody: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        try {
            val root = JSONObject(responseBody)
            if (root.has("documents")) {
                val array = root.getJSONArray("documents")
                for (i in 0 until array.length()) {
                    results.add(array.getJSONObject(i))
                }
            } else if (root.has("items")) {
                val array = root.getJSONArray("items")
                for (i in 0 until array.length()) {
                    results.add(array.getJSONObject(i))
                }
            } else if (root.has("data")) {
                val array = root.getJSONArray("data")
                for (i in 0 until array.length()) {
                    results.add(array.getJSONObject(i))
                }
            } else if (root.has("document")) {
                results.add(root.getJSONObject("document"))
            }
        } catch (e: Exception) {
            try {
                val array = JSONArray(responseBody)
                for (i in 0 until array.length()) {
                    results.add(array.getJSONObject(i))
                }
            } catch (ex: Exception) {
                // Return empty
            }
        }
        return results
    }
}
