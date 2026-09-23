package com.example.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GstSummary
import com.example.data.model.InvoiceWithItems
import com.example.data.repository.BillKaroRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReportsViewModel(private val repository: BillKaroRepository) : ViewModel() {

    val gstSummary: StateFlow<GstSummary> = repository.gstSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GstSummary())

    val b2bInvoices: StateFlow<List<InvoiceWithItems>> = repository.allInvoices.map { invoices ->
        invoices.filter { !it.invoice.customerGstin.isNullOrBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val b2cInvoices: StateFlow<List<InvoiceWithItems>> = repository.allInvoices.map { invoices ->
        invoices.filter { it.invoice.customerGstin.isNullOrBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class ReportsViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
