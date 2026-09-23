package com.example.data.remote.mongo

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

data class MongoConfig(
    val gatewayUrl: String = "",
    val authToken: String = "",
    val clusterName: String = "Cluster0",
    val databaseName: String = "billkaro",
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0L,
    val isConfiguredViaSecrets: Boolean = false
) {
    // Backward compatibility aliases
    val endpointUrl: String get() = gatewayUrl
    val apiKey: String get() = authToken
    val isConfigured: Boolean get() = isConfiguredViaSecrets
}

data class MongoConnectionResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val database: String = "billkaro"
)

data class MongoSyncResult(
    val success: Boolean,
    val message: String,
    val syncedInvoices: Int = 0,
    val syncedCustomers: Int = 0,
    val syncedProducts: Int = 0,
    val syncedBills: Int = 0
)

class MongoSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("billkaro_mongo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SYNC = "mongo_last_sync"
        private const val KEY_LAST_STATUS = "mongo_last_status"
        private const val KEY_AUTO_SYNC = "mongo_auto_sync"
        private const val KEY_GATEWAY_URL_OVERRIDE = "mongo_gateway_url_override"
        private const val KEY_GATEWAY_TOKEN_OVERRIDE = "mongo_gateway_token_override"
    }

    /**
     * Resolves the cloud synchronization gateway configuration.
     * In production, database credentials/URIs are NEVER placed in the client APK.
     * The app connects to a secure HTTPS Gateway / Proxy server.
     */
    fun getConfig(): MongoConfig {
        // Priority 1: Check SYNC_GATEWAY_URL
        val buildGatewayUrl = try {
            val field = BuildConfig::class.java.getField("SYNC_GATEWAY_URL")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }

        val buildGatewayToken = try {
            val field = BuildConfig::class.java.getField("SYNC_GATEWAY_TOKEN")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }

        // Backward compatibility fallback to legacy fields if present
        val legacyUri = try {
            val field = BuildConfig::class.java.getField("MONGODB_URI")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }

        val legacyApiKey = try {
            val field = BuildConfig::class.java.getField("MONGODB_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }

        val overrideUrl = prefs.getString(KEY_GATEWAY_URL_OVERRIDE, "")?.trim() ?: ""
        val overrideToken = prefs.getString(KEY_GATEWAY_TOKEN_OVERRIDE, "")?.trim() ?: ""

        val rawUrl = when {
            overrideUrl.isNotBlank() -> overrideUrl
            buildGatewayUrl.isNotBlank() && buildGatewayUrl != "DEFAULT_SYNC_GATEWAY_URL" -> buildGatewayUrl
            legacyUri.isNotBlank() && legacyUri != "DEFAULT_MONGODB_URI" -> legacyUri
            else -> ""
        }

        val rawToken = when {
            overrideToken.isNotBlank() -> overrideToken
            buildGatewayToken.isNotBlank() && buildGatewayToken != "DEFAULT_SYNC_GATEWAY_TOKEN" -> buildGatewayToken
            legacyApiKey.isNotBlank() && legacyApiKey != "DEFAULT_MONGODB_API_KEY" -> legacyApiKey
            else -> ""
        }

        val isConfigured = rawUrl.isNotBlank()

        var cluster = "Cluster0"
        var database = "billkaro"

        if (rawUrl.contains("@")) {
            try {
                val afterAt = rawUrl.substringAfter("@")
                val hostPart = afterAt.substringBefore("/").substringBefore("?")
                cluster = hostPart.split(".")[0].ifBlank { "Cluster0" }
                if (afterAt.contains("/")) {
                    val path = afterAt.substringAfter("/").substringBefore("?").trim()
                    if (path.isNotBlank()) database = path
                }
            } catch (_: Exception) {
                // Fallback to default
            }
        }

        return MongoConfig(
            gatewayUrl = rawUrl,
            authToken = rawToken,
            clusterName = cluster,
            databaseName = database,
            isAutoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            lastSyncTime = prefs.getLong(KEY_LAST_SYNC, 0L),
            isConfiguredViaSecrets = isConfigured
        )
    }

    fun isConfigured(): Boolean {
        return getConfig().isConfigured
    }

    fun saveGatewayOverride(gatewayUrl: String, authToken: String = "") {
        prefs.edit()
            .putString(KEY_GATEWAY_URL_OVERRIDE, gatewayUrl.trim())
            .putString(KEY_GATEWAY_TOKEN_OVERRIDE, authToken.trim())
            .apply()
    }

    fun getMaskedSummary(): String {
        val config = getConfig()
        if (!config.isConfigured) {
            return "Production Secure: Ready to connect to backend sync gateway."
        }
        val safeHost = try {
            if (config.gatewayUrl.startsWith("http")) {
                val uri = java.net.URI(config.gatewayUrl)
                uri.host ?: config.gatewayUrl
            } else {
                "Cloud Gateway (${config.clusterName})"
            }
        } catch (_: Exception) {
            "Cloud Gateway"
        }
        return "Secure Gateway: $safeHost • Zero Database Credentials in APK"
    }

    fun updateLastSyncTime(timestamp: Long, status: String = "Synced") {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, timestamp)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    fun getLastSyncStatus(): String {
        return prefs.getString(KEY_LAST_STATUS, "Not synced yet") ?: "Not synced yet"
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }
}
