package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class BusinessSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("billkaro_business_prefs", Context.MODE_PRIVATE)

    var businessName: String
        get() = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_NAME, value.trim()).apply()

    var businessGstin: String
        get() = prefs.getString(KEY_BUSINESS_GSTIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_GSTIN, value.trim().uppercase()).apply()

    var businessPhone: String
        get() = prefs.getString(KEY_BUSINESS_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_PHONE, value.trim()).apply()

    var businessAddress: String
        get() = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_ADDRESS, value.trim()).apply()

    var upiId: String
        get() = prefs.getString(KEY_UPI_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_UPI_ID, value.trim()).apply()

    companion object {
        private const val KEY_BUSINESS_NAME = "key_business_name"
        private const val KEY_BUSINESS_GSTIN = "key_business_gstin"
        private const val KEY_BUSINESS_PHONE = "key_business_phone"
        private const val KEY_BUSINESS_ADDRESS = "key_business_address"
        private const val KEY_UPI_ID = "key_upi_id"

        @Volatile
        private var instance: BusinessSettingsManager? = null

        fun getInstance(context: Context): BusinessSettingsManager {
            return instance ?: synchronized(this) {
                instance ?: BusinessSettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
