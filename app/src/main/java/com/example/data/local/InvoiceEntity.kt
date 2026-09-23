package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val customerName: String,
    val customerGstin: String? = null,
    val customerPhone: String? = null,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
    val subTotal: Double,
    val totalTax: Double,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val discount: Double = 0.0,
    val grandTotal: Double,
    val isPaid: Boolean = false,
    val paymentMode: String = "UPI", // Cash, UPI, Credit Card, Bank
    val paymentReference: String? = null,
    val isInterstate: Boolean = false,
    val notes: String? = null
)
