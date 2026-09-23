package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_bills")
data class SupplierBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierName: String,
    val gstin: String? = null,
    val billNumber: String? = null,
    val date: Long = System.currentTimeMillis(),
    val productName: String? = null,
    val hsnCode: String? = null,
    val amount: Double,
    val gstAmount: Double = 0.0,
    val rawOcrText: String? = null
)
