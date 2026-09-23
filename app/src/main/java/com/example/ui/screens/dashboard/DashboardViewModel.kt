package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.model.GstSummary
import com.example.data.model.InvoiceWithItems
import com.example.data.repository.BillKaroRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalSales: Double = 0.0,
    val outstandingTotal: Double = 0.0,
    val totalGstCollected: Double = 0.0,
    val totalCustomers: Int = 0,
    val gstSummary: GstSummary = GstSummary(),
    val recentInvoices: List<InvoiceWithItems> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(private val repository: BillKaroRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        combine(
            repository.totalSales,
            repository.outstandingTotal,
            repository.totalGstCollected,
            repository.allCustomers,
            repository.gstSummary,
            repository.allInvoices
        ) { flowArray ->
            val sales = flowArray[0] as Double
            val outstanding = flowArray[1] as Double
            val gst = flowArray[2] as Double
            @Suppress("UNCHECKED_CAST")
            val customers = flowArray[3] as List<CustomerEntity>
            val gstSum = flowArray[4] as GstSummary
            @Suppress("UNCHECKED_CAST")
            val invoices = flowArray[5] as List<InvoiceWithItems>

            DashboardUiState(
                totalSales = sales,
                outstandingTotal = outstanding,
                totalGstCollected = gst,
                totalCustomers = customers.size,
                gstSummary = gstSum,
                recentInvoices = invoices.take(5),
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun markInvoicePaid(id: Long) {
        viewModelScope.launch {
            repository.updateInvoicePaymentStatus(id, true)
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    fun syncWithMongo(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.fetchFromMongo()
            onResult(result.message)
        }
    }
}

class DashboardViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
