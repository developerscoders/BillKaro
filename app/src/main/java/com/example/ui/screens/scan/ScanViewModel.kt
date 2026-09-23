package com.example.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SupplierBillEntity
import com.example.data.repository.BillKaroRepository
import com.example.ui.utils.BusinessCardOcrResult
import com.example.ui.utils.OcrParsers
import com.example.ui.utils.SupplierBillOcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val scanMode: String = "business_card", // "business_card", "supplier_bill", "barcode"
    val isProcessing: Boolean = false,
    val cardResult: BusinessCardOcrResult? = null,
    val billResult: SupplierBillOcrResult? = null,
    val barcodeResult: String? = null,
    val matchedProduct: ProductEntity? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ScanViewModel(private val repository: BillKaroRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun setScanMode(mode: String) {
        _uiState.update {
            ScanUiState(scanMode = mode)
        }
    }

    fun processBitmap(bitmap: Bitmap, context: Context, scanMode: String) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                if (rawText.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "No text detected. Please ensure the document is well-lit and clear."
                        )
                    }
                    return@addOnSuccessListener
                }
                if (scanMode == "business_card") {
                    val result = OcrParsers.parseBusinessCard(rawText)
                    _uiState.update { it.copy(isProcessing = false, cardResult = result) }
                } else if (scanMode == "supplier_bill") {
                    val result = OcrParsers.parseSupplierBill(rawText)
                    _uiState.update { it.copy(isProcessing = false, billResult = result) }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Scan failed: ${e.localizedMessage ?: "Unable to read image"}"
                    )
                }
            }
    }

    fun processSampleGstInvoice(context: Context) {
        try {
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_sample_gst_invoice
            )
            if (bitmap != null) {
                setScanMode("supplier_bill")
                processBitmap(bitmap, context, "supplier_bill")
            } else {
                _uiState.update { it.copy(errorMessage = "Could not load sample GST invoice image.") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Failed to load sample invoice: ${e.localizedMessage}") }
        }
    }

    fun processBarcode(barcode: String) {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true, barcodeResult = barcode) }

        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    barcodeResult = barcode,
                    matchedProduct = product,
                    successMessage = if (product != null) "Found Product: ${product.name}" else "New Barcode Detected: $barcode"
                )
            }
        }
    }

    fun saveCardAsCustomer(onSuccess: () -> Unit) {
        val card = _uiState.value.cardResult ?: return
        viewModelScope.launch {
            repository.insertCustomer(
                CustomerEntity(
                    name = if (card.name.isNotBlank()) card.name else "Scanned Contact",
                    phone = if (card.phone.isNotBlank()) card.phone else "9800000000",
                    gstin = if (card.gstin.isNotBlank()) card.gstin else null,
                    address = card.address
                )
            )
            _uiState.update { it.copy(successMessage = "Customer Added & Stored to MongoDB!") }
            onSuccess()
        }
    }

    fun saveSupplierBill(onSuccess: () -> Unit) {
        val bill = _uiState.value.billResult ?: return
        viewModelScope.launch {
            repository.insertSupplierBill(
                SupplierBillEntity(
                    supplierName = if (bill.supplierName.isNotBlank()) bill.supplierName else "Scanned Supplier",
                    gstin = if (bill.gstin.isNotBlank()) bill.gstin else null,
                    billNumber = bill.billNumber,
                    productName = "Bulk Stock Purchase",
                    hsnCode = bill.hsnCode,
                    amount = bill.amount,
                    gstAmount = bill.amount * 0.18,
                    rawOcrText = bill.rawText
                )
            )
            _uiState.update { it.copy(successMessage = "Supplier Bill Recorded & Stored in MongoDB!") }
            onSuccess()
        }
    }

    fun clearResult() {
        _uiState.update { ScanUiState(scanMode = _uiState.value.scanMode) }
    }
}

class ScanViewModelFactory(private val repository: BillKaroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
