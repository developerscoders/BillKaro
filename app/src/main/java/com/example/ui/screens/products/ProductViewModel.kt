package com.example.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProductEntity
import com.example.data.repository.BillKaroRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: BillKaroRepository) : ViewModel() {

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProduct(
        name: String,
        barcode: String?,
        hsnCode: String?,
        purchasePrice: Double,
        sellingPrice: Double,
        gstRate: Double,
        stockQuantity: Int,
        category: String
    ) {
        viewModelScope.launch {
            repository.insertProduct(
                ProductEntity(
                    name = name,
                    barcode = if (barcode.isNull_or_blank()) null else barcode,
                    hsnCode = if (hsnCode.isNull_or_blank()) null else hsnCode,
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    gstRate = gstRate,
                    stockQuantity = stockQuantity,
                    category = category.ifBlank { "General" }
                )
            )
        }
    }
}

class ProductViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
