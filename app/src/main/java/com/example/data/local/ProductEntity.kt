package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val hsnCode: String? = null,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double,
    val gstRate: Double = 18.0, // e.g. 0, 5, 12, 18, 28
    val stockQuantity: Int = 0,
    val category: String = "General",
    val unit: String = "Pcs"
)
