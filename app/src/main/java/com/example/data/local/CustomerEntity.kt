package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val gstin: String? = null,
    val address: String? = null,
    val email: String? = null,
    val totalPendingAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
