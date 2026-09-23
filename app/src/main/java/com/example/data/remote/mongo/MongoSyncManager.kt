package com.example.data.remote.mongo

import com.example.data.local.*
import com.example.data.model.InvoiceWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MongoSyncManager(
    private val db: AppDatabase,
    private val mongoSettings: MongoSettingsManager,
    private val apiClient: MongoApiClient = MongoApiClient()
) {

    private val customerDao = db.customerDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val supplierBillDao = db.supplierBillDao()

    suspend fun testConnection(): MongoConnectionResult {
        val config = mongoSettings.getConfig()
        return apiClient.testConnection(config)
    }

    suspend fun fetchAndStoreAllFromMongo(): MongoSyncResult = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) {
            return@withContext MongoSyncResult(
                success = false,
                message = "MongoDB endpoint URL is empty. Configure it in Settings."
            )
        }

        try {
            var syncedCustCount = 0
            var syncedProdCount = 0
            var syncedInvCount = 0
            var syncedBillCount = 0

            // 1. Fetch Customers
            val customerDocs = apiClient.fetchDocuments("customers", config)
            for (doc in customerDocs) {
                val customer = parseCustomer(doc)
                if (customer != null) {
                    customerDao.insertCustomer(customer)
                    syncedCustCount++
                }
            }

            // 2. Fetch Products
            val productDocs = apiClient.fetchDocuments("products", config)
            for (doc in productDocs) {
                val product = parseProduct(doc)
                if (product != null) {
                    productDao.insertProduct(product)
                    syncedProdCount++
                }
            }

            // 3. Fetch Invoices
            val invoiceDocs = apiClient.fetchDocuments("invoices", config)
            for (doc in invoiceDocs) {
                val pair = parseInvoice(doc)
                if (pair != null) {
                    val (inv, items) = pair
                    val insertedId = invoiceDao.insertInvoice(inv)
                    val itemsWithId = items.map { it.copy(invoiceId = insertedId) }
                    invoiceDao.insertInvoiceItems(itemsWithId)
                    syncedInvCount++
                }
            }

            // 4. Fetch Supplier Bills / Scanned Documents
            val billDocs = apiClient.fetchDocuments("supplier_bills", config)
            for (doc in billDocs) {
                val bill = parseSupplierBill(doc)
                if (bill != null) {
                    supplierBillDao.insertSupplierBill(bill)
                    syncedBillCount++
                }
            }

            mongoSettings.updateLastSyncTime(System.currentTimeMillis(), "Fetched successfully")

            MongoSyncResult(
                success = true,
                message = "Successfully fetched from MongoDB: $syncedInvCount invoices, $syncedBillCount scans, $syncedProdCount items, $syncedCustCount customers.",
                syncedInvoices = syncedInvCount,
                syncedCustomers = syncedCustCount,
                syncedProducts = syncedProdCount,
                syncedBills = syncedBillCount
            )
        } catch (e: Exception) {
            mongoSettings.updateLastSyncTime(System.currentTimeMillis(), "Fetch error: ${e.localizedMessage}")
            MongoSyncResult(
                success = false,
                message = "Failed to fetch from MongoDB: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun pushAllToMongo(): MongoSyncResult = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) {
            return@withContext MongoSyncResult(
                success = false,
                message = "MongoDB endpoint URL is empty. Configure it in Settings."
            )
        }

        try {
            var pushedCust = 0
            var pushedProd = 0
            var pushedInv = 0
            var pushedBill = 0

            // Push customers
            val customers = customerDao.getAllCustomers().first()
            for (cust in customers) {
                if (apiClient.upsertDocument("customers", "phone", cust.phone, customerToJson(cust), config)) {
                    pushedCust++
                }
            }

            // Push products
            val products = productDao.getAllProducts().first()
            for (prod in products) {
                val filterKey = if (!prod.barcode.isNullOrBlank()) "barcode" else "name"
                val filterVal = prod.barcode ?: prod.name
                if (apiClient.upsertDocument("products", filterKey, filterVal, productToJson(prod), config)) {
                    pushedProd++
                }
            }

            // Push invoices
            val invoices = invoiceDao.getAllInvoicesWithItems().first()
            for (inv in invoices) {
                if (apiClient.upsertDocument("invoices", "invoiceNumber", inv.invoice.invoiceNumber, invoiceToJson(inv), config)) {
                    pushedInv++
                }
            }

            // Push supplier bills
            val bills = supplierBillDao.getAllSupplierBills().first()
            for (bill in bills) {
                val filterVal = bill.billNumber ?: "${bill.supplierName}_${bill.date}"
                if (apiClient.upsertDocument("supplier_bills", "billNumber", filterVal, supplierBillToJson(bill), config)) {
                    pushedBill++
                }
            }

            mongoSettings.updateLastSyncTime(System.currentTimeMillis(), "Pushed successfully")

            MongoSyncResult(
                success = true,
                message = "Pushed to MongoDB: $pushedInv invoices, $pushedBill scans, $pushedProd items, $pushedCust customers.",
                syncedInvoices = pushedInv,
                syncedCustomers = pushedCust,
                syncedProducts = pushedProd,
                syncedBills = pushedBill
            )
        } catch (e: Exception) {
            mongoSettings.updateLastSyncTime(System.currentTimeMillis(), "Push error: ${e.localizedMessage}")
            MongoSyncResult(
                success = false,
                message = "Push failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun pushSingleInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Boolean = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) return@withContext false
        val invWithItems = InvoiceWithItems(invoice, items)
        apiClient.upsertDocument("invoices", "invoiceNumber", invoice.invoiceNumber, invoiceToJson(invWithItems), config)
    }

    suspend fun pushSingleCustomer(customer: CustomerEntity): Boolean = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) return@withContext false
        apiClient.upsertDocument("customers", "phone", customer.phone, customerToJson(customer), config)
    }

    suspend fun pushSingleProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) return@withContext false
        val filterKey = if (!product.barcode.isNullOrBlank()) "barcode" else "name"
        val filterVal = product.barcode ?: product.name
        apiClient.upsertDocument("products", filterKey, filterVal, productToJson(product), config)
    }

    suspend fun pushSingleSupplierBill(bill: SupplierBillEntity): Boolean = withContext(Dispatchers.IO) {
        val config = mongoSettings.getConfig()
        if (config.endpointUrl.isBlank()) return@withContext false
        val filterVal = bill.billNumber ?: "${bill.supplierName}_${bill.date}"
        apiClient.upsertDocument("supplier_bills", "billNumber", filterVal, supplierBillToJson(bill), config)
    }

    // JSON Serializers
    fun customerToJson(customer: CustomerEntity): JSONObject = JSONObject().apply {
        put("name", customer.name)
        put("phone", customer.phone)
        put("gstin", customer.gstin ?: "")
        put("address", customer.address ?: "")
        put("email", customer.email ?: "")
        put("totalPendingAmount", customer.totalPendingAmount)
        put("createdAt", customer.createdAt)
    }

    fun productToJson(product: ProductEntity): JSONObject = JSONObject().apply {
        put("name", product.name)
        put("barcode", product.barcode ?: "")
        put("hsnCode", product.hsnCode ?: "")
        put("purchasePrice", product.purchasePrice)
        put("sellingPrice", product.sellingPrice)
        put("gstRate", product.gstRate)
        put("stockQuantity", product.stockQuantity)
        put("category", product.category)
        put("unit", product.unit)
    }

    fun invoiceToJson(inv: InvoiceWithItems): JSONObject = JSONObject().apply {
        val invoice = inv.invoice
        put("invoiceNumber", invoice.invoiceNumber)
        put("customerName", invoice.customerName)
        put("customerGstin", invoice.customerGstin ?: "")
        put("customerPhone", invoice.customerPhone ?: "")
        put("date", invoice.date)
        put("dueDate", invoice.dueDate)
        put("subTotal", invoice.subTotal)
        put("totalTax", invoice.totalTax)
        put("cgstAmount", invoice.cgstAmount)
        put("sgstAmount", invoice.sgstAmount)
        put("igstAmount", invoice.igstAmount)
        put("discount", invoice.discount)
        put("grandTotal", invoice.grandTotal)
        put("isPaid", invoice.isPaid)
        put("paymentMode", invoice.paymentMode)
        put("notes", invoice.notes ?: "")

        val itemsArray = JSONArray()
        for (item in inv.items) {
            val itemObj = JSONObject().apply {
                put("productName", item.productName)
                put("hsnCode", item.hsnCode ?: "")
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("gstRate", item.gstRate)
                put("taxAmount", item.taxAmount)
                put("totalAmount", item.totalAmount)
            }
            itemsArray.put(itemObj)
        }
        put("items", itemsArray)
    }

    fun supplierBillToJson(bill: SupplierBillEntity): JSONObject = JSONObject().apply {
        put("supplierName", bill.supplierName)
        put("gstin", bill.gstin ?: "")
        put("billNumber", bill.billNumber ?: "")
        put("date", bill.date)
        put("productName", bill.productName ?: "")
        put("hsnCode", bill.hsnCode ?: "")
        put("amount", bill.amount)
        put("gstAmount", bill.gstAmount)
        put("rawOcrText", bill.rawOcrText ?: "")
    }

    // JSON Deserializers
    fun parseCustomer(doc: JSONObject): CustomerEntity? {
        val name = doc.optString("name").takeIf { it.isNotBlank() } ?: return null
        val phone = doc.optString("phone").takeIf { it.isNotBlank() } ?: "0000000000"
        return CustomerEntity(
            name = name,
            phone = phone,
            gstin = doc.optString("gstin").takeIf { it.isNotBlank() },
            address = doc.optString("address").takeIf { it.isNotBlank() },
            email = doc.optString("email").takeIf { it.isNotBlank() },
            totalPendingAmount = doc.optDouble("totalPendingAmount", 0.0),
            createdAt = doc.optLong("createdAt", System.currentTimeMillis())
        )
    }

    fun parseProduct(doc: JSONObject): ProductEntity? {
        val name = doc.optString("name").takeIf { it.isNotBlank() } ?: return null
        return ProductEntity(
            name = name,
            barcode = doc.optString("barcode").takeIf { it.isNotBlank() },
            hsnCode = doc.optString("hsnCode").takeIf { it.isNotBlank() },
            purchasePrice = doc.optDouble("purchasePrice", 0.0),
            sellingPrice = doc.optDouble("sellingPrice", 0.0),
            gstRate = doc.optDouble("gstRate", 18.0),
            stockQuantity = doc.optInt("stockQuantity", 0),
            category = doc.optString("category", "General"),
            unit = doc.optString("unit", "Pcs")
        )
    }

    fun parseInvoice(doc: JSONObject): Pair<InvoiceEntity, List<InvoiceItemEntity>>? {
        val invNum = doc.optString("invoiceNumber").takeIf { it.isNotBlank() } ?: return null
        val custName = doc.optString("customerName", "Customer")
        val inv = InvoiceEntity(
            invoiceNumber = invNum,
            customerId = 0,
            customerName = custName,
            customerGstin = doc.optString("customerGstin").takeIf { it.isNotBlank() },
            customerPhone = doc.optString("customerPhone").takeIf { it.isNotBlank() },
            date = doc.optLong("date", System.currentTimeMillis()),
            dueDate = doc.optLong("dueDate", System.currentTimeMillis() + 7 * 86400000L),
            subTotal = doc.optDouble("subTotal", 0.0),
            totalTax = doc.optDouble("totalTax", 0.0),
            cgstAmount = doc.optDouble("cgstAmount", 0.0),
            sgstAmount = doc.optDouble("sgstAmount", 0.0),
            igstAmount = doc.optDouble("igstAmount", 0.0),
            discount = doc.optDouble("discount", 0.0),
            grandTotal = doc.optDouble("grandTotal", 0.0),
            isPaid = doc.optBoolean("isPaid", false),
            paymentMode = doc.optString("paymentMode", "UPI"),
            notes = doc.optString("notes").takeIf { it.isNotBlank() }
        )

        val items = mutableListOf<InvoiceItemEntity>()
        val itemsArray = doc.optJSONArray("items")
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val pName = itemObj.optString("productName", "Item")
                items.add(
                    InvoiceItemEntity(
                        invoiceId = 0,
                        productId = 0,
                        productName = pName,
                        hsnCode = itemObj.optString("hsnCode").takeIf { it.isNotBlank() },
                        quantity = itemObj.optInt("quantity", 1),
                        unitPrice = itemObj.optDouble("unitPrice", 0.0),
                        gstRate = itemObj.optDouble("gstRate", 18.0),
                        taxAmount = itemObj.optDouble("taxAmount", 0.0),
                        totalAmount = itemObj.optDouble("totalAmount", 0.0)
                    )
                )
            }
        }
        return Pair(inv, items)
    }

    fun parseSupplierBill(doc: JSONObject): SupplierBillEntity? {
        val suppName = doc.optString("supplierName").takeIf { it.isNotBlank() } ?: return null
        return SupplierBillEntity(
            supplierName = suppName,
            gstin = doc.optString("gstin").takeIf { it.isNotBlank() },
            billNumber = doc.optString("billNumber").takeIf { it.isNotBlank() },
            date = doc.optLong("date", System.currentTimeMillis()),
            productName = doc.optString("productName", "Bulk Stock Purchase"),
            hsnCode = doc.optString("hsnCode").takeIf { it.isNotBlank() },
            amount = doc.optDouble("amount", 0.0),
            gstAmount = doc.optDouble("gstAmount", 0.0),
            rawOcrText = doc.optString("rawOcrText").takeIf { it.isNotBlank() }
        )
    }
}
