package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.GstSummary
import com.example.data.model.InvoiceWithItems
import com.example.data.remote.mongo.MongoConnectionResult
import com.example.data.remote.mongo.MongoSyncManager
import com.example.data.remote.mongo.MongoSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BillKaroRepository(
    private val db: AppDatabase,
    val mongoSyncManager: MongoSyncManager? = null
) {
    private val customerDao = db.customerDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val supplierBillDao = db.supplierBillDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allInvoices: Flow<List<InvoiceWithItems>> = invoiceDao.getAllInvoicesWithItems()
    val allSupplierBills: Flow<List<SupplierBillEntity>> = supplierBillDao.getAllSupplierBills()

    val totalSales: Flow<Double> = invoiceDao.getTotalSales().map { it ?: 0.0 }
    val outstandingTotal: Flow<Double> = invoiceDao.getOutstandingTotal().map { it ?: 0.0 }
    val totalGstCollected: Flow<Double> = invoiceDao.getTotalGstCollected().map { it ?: 0.0 }

    val gstSummary: Flow<GstSummary> = allInvoices.map { invoices ->
        var cgst = 0.0
        var sgst = 0.0
        var igst = 0.0
        var totalTax = 0.0
        var sales = 0.0

        for (invWithItems in invoices) {
            val inv = invWithItems.invoice
            cgst += inv.cgstAmount
            sgst += inv.sgstAmount
            igst += inv.igstAmount
            totalTax += inv.totalTax
            sales += inv.grandTotal
        }
        GstSummary(
            totalCgst = cgst,
            totalSgst = sgst,
            totalIgst = igst,
            totalTax = totalTax,
            totalSales = sales
        )
    }

    suspend fun getInvoiceById(id: Long): InvoiceWithItems? {
        return invoiceDao.getInvoiceWithItemsById(id)
    }

    suspend fun createInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        val itemsWithInvoiceId = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertInvoiceItems(itemsWithInvoiceId)

        // Deduct product stock
        for (item in itemsWithInvoiceId) {
            if (item.productId > 0) {
                productDao.updateStock(item.productId, -item.quantity)
            }
        }

        // Sync to MongoDB in background
        mongoSyncManager?.pushSingleInvoice(invoice.copy(id = invoiceId), itemsWithInvoiceId)

        return invoiceId
    }

    suspend fun updateInvoicePaymentStatus(id: Long, isPaid: Boolean) {
        invoiceDao.updatePaymentStatus(id, isPaid)
        val inv = invoiceDao.getInvoiceWithItemsById(id)
        if (inv != null) {
            mongoSyncManager?.pushSingleInvoice(inv.invoice, inv.items)
        }
    }

    suspend fun deleteInvoice(id: Long) {
        invoiceDao.deleteInvoiceItemsByInvoiceId(id)
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun insertCustomer(customer: CustomerEntity): Long {
        val id = customerDao.insertCustomer(customer)
        mongoSyncManager?.pushSingleCustomer(customer.copy(id = id))
        return id
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        val id = productDao.insertProduct(product)
        mongoSyncManager?.pushSingleProduct(product.copy(id = id))
        return id
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertSupplierBill(bill: SupplierBillEntity): Long {
        val id = supplierBillDao.insertSupplierBill(bill)
        mongoSyncManager?.pushSingleSupplierBill(bill.copy(id = id))
        return id
    }

    // MongoDB Sync Operations
    suspend fun fetchFromMongo(): MongoSyncResult {
        return mongoSyncManager?.fetchAndStoreAllFromMongo()
            ?: MongoSyncResult(false, "MongoDB sync is not configured")
    }

    suspend fun pushToMongo(): MongoSyncResult {
        return mongoSyncManager?.pushAllToMongo()
            ?: MongoSyncResult(false, "MongoDB sync is not configured")
    }

    suspend fun testMongoConnection(): MongoConnectionResult {
        return mongoSyncManager?.testConnection()
            ?: MongoConnectionResult(false, "MongoDB sync is not configured")
    }

    suspend fun seedInitialDataIfEmpty() {
        // Clean initialization - starts with a clean database ready for user entry
    }
}
