package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val hsnCode: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val gstRate: Double,
    val taxAmount: Double,
    val totalAmount: Double
)
