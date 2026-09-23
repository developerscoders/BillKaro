package com.example.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.ProductEntity
import com.example.data.model.InvoiceWithItems
import com.example.data.repository.BillKaroRepository
import com.example.ui.utils.GstUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InvoiceItemForm(
    val product: ProductEntity? = null,
    val productName: String = "",
    val hsnCode: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val gstRate: Double = 18.0
)

data class CreateInvoiceUiState(
    val selectedCustomer: CustomerEntity? = null,
    val items: List<InvoiceItemForm> = listOf(InvoiceItemForm()),
    val isInterstate: Boolean = false,
    val discount: Double = 0.0,
    val paymentMode: String = "UPI",
    val notes: String = "Thank you for your business!",
    val isSubmitting: Boolean = false,
    val createdInvoiceId: Long? = null
)

class InvoiceViewModel(private val repository: BillKaroRepository) : ViewModel() {

    val allInvoices: StateFlow<List<InvoiceWithItems>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _createUiState = MutableStateFlow(CreateInvoiceUiState())
    val createUiState: StateFlow<CreateInvoiceUiState> = _createUiState.asStateFlow()

    private val _selectedInvoiceDetail = MutableStateFlow<InvoiceWithItems?>(null)
    val selectedInvoiceDetail: StateFlow<InvoiceWithItems?> = _selectedInvoiceDetail.asStateFlow()

    fun loadInvoiceDetail(id: Long) {
        viewModelScope.launch {
            _selectedInvoiceDetail.value = repository.getInvoiceById(id)
        }
    }

    fun markPaid(id: Long) {
        viewModelScope.launch {
            repository.updateInvoicePaymentStatus(id, true)
            loadInvoiceDetail(id)
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    // Create Invoice Actions
    fun setSelectedCustomer(customer: CustomerEntity) {
        // Auto check if state code or GSTIN differs for interstate
        val isInterstate = customer.gstin?.take(2)?.let { it != "27" } ?: false // Default MH is 27
        _createUiState.update { it.copy(selectedCustomer = customer, isInterstate = isInterstate) }
    }

    fun addItem() {
        _createUiState.update { it.copy(items = it.items + InvoiceItemForm()) }
    }

    fun removeItem(index: Int) {
        if (_createUiState.value.items.size > 1) {
            _createUiState.update { state ->
                val newItems = state.items.toMutableList()
                newItems.removeAt(index)
                state.copy(items = newItems)
            }
        }
    }

    fun updateItemProduct(index: Int, product: ProductEntity) {
        _createUiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(
                product = product,
                productName = product.name,
                hsnCode = product.hsnCode ?: "",
                unitPrice = product.sellingPrice,
                gstRate = product.gstRate
            )
            state.copy(items = newItems)
        }
    }

    fun updateItemQuantity(index: Int, qty: Int) {
        _createUiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(quantity = qty.coerceAtLeast(1))
            state.copy(items = newItems)
        }
    }

    fun updateItemUnitPrice(index: Int, price: Double) {
        _createUiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(unitPrice = price)
            state.copy(items = newItems)
        }
    }

    fun setInterstate(isInterstate: Boolean) {
        _createUiState.update { it.copy(isInterstate = isInterstate) }
    }

    fun setDiscount(discount: Double) {
        _createUiState.update { it.copy(discount = discount) }
    }

    fun setPaymentMode(mode: String) {
        _createUiState.update { it.copy(paymentMode = mode) }
    }

    fun setNotes(notes: String) {
        _createUiState.update { it.copy(notes = notes) }
    }

    fun saveInvoice(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val state = _createUiState.value
            val customer = state.selectedCustomer ?: return@launch

            _createUiState.update { it.copy(isSubmitting = true) }

            // Calculate Totals
            var rawSubTotal = 0.0
            var totalTaxSum = 0.0

            val itemEntities = mutableListOf<InvoiceItemEntity>()

            for (item in state.items) {
                if (item.productName.isBlank()) continue
                val lineSubTotal = item.quantity * item.unitPrice
                val lineTax = lineSubTotal * (item.gstRate / 100.0)
                val lineTotal = lineSubTotal + lineTax

                rawSubTotal += lineSubTotal
                totalTaxSum += lineTax

                itemEntities.add(
                    InvoiceItemEntity(
                        invoiceId = 0,
                        productId = item.product?.id ?: 0,
                        productName = item.productName,
                        hsnCode = item.hsnCode,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        gstRate = item.gstRate,
                        taxAmount = lineTax,
                        totalAmount = lineTotal
                    )
                )
            }

            val taxBreakdown = GstUtils.calculateTax(
                subTotal = rawSubTotal,
                gstRatePercent = if (itemEntities.isNotEmpty()) itemEntities.first().gstRate else 18.0,
                isInterstate = state.isInterstate,
                discount = state.discount
            )

            val invoiceNumber = "INV-2026-" + (System.currentTimeMillis() % 10000).toString().padStart(4, '0')

            val invoiceEntity = InvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerId = customer.id,
                customerName = customer.name,
                customerGstin = customer.gstin,
                customerPhone = customer.phone,
                date = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + (7 * 24 * 3600 * 1000L),
                subTotal = rawSubTotal,
                totalTax = taxBreakdown.totalTax,
                cgstAmount = taxBreakdown.cgstAmount,
                sgstAmount = taxBreakdown.sgstAmount,
                igstAmount = taxBreakdown.igstAmount,
                discount = state.discount,
                grandTotal = taxBreakdown.grandTotal,
                isPaid = state.paymentMode != "Credit",
                paymentMode = state.paymentMode,
                isInterstate = state.isInterstate,
                notes = state.notes
            )

            val newId = repository.createInvoice(invoiceEntity, itemEntities)
            _createUiState.value = CreateInvoiceUiState() // reset
            onSuccess(newId)
        }
    }
}

class InvoiceViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
