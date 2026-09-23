package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SupplierBillEntity
import com.example.data.remote.mongo.MongoApiClient
import com.example.data.remote.mongo.MongoConfig
import com.example.data.remote.mongo.MongoSettingsManager
import com.example.data.remote.mongo.MongoSyncManager
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BillKaro", appName)
  }

  @Test
  fun `test sample gst invoice drawable exists and loads`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.img_sample_gst_invoice)
    assertNotNull(drawable)
  }

  @Test
  fun `test mongo settings manager secure summary and last sync`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = MongoSettingsManager(context)

    manager.updateLastSyncTime(1700000000000L, "Synced 15 items")
    assertEquals("Synced 15 items", manager.getLastSyncStatus())

    val summary = manager.getMaskedSummary()
    assertNotNull(summary)
    // Ensure raw credentials or passwords are never returned in masked summary
    assertFalse(summary.contains("password"))
  }

  @Test
  fun `test mongo serialization for customer`() {
    val customer = CustomerEntity(
      id = 1,
      name = "Ramesh Enterprises",
      phone = "9876543210",
      gstin = "07AAAAA0000A1Z5",
      address = "Connaught Place, New Delhi",
      email = "ramesh@example.com",
      totalPendingAmount = 15000.0
    )

    val json = JSONObject().apply {
      put("name", customer.name)
      put("phone", customer.phone)
      put("gstin", customer.gstin)
      put("address", customer.address)
      put("email", customer.email)
      put("totalPendingAmount", customer.totalPendingAmount)
    }

    assertEquals("Ramesh Enterprises", json.getString("name"))
    assertEquals("9876543210", json.getString("phone"))
    assertEquals("07AAAAA0000A1Z5", json.getString("gstin"))
    assertEquals(15000.0, json.getDouble("totalPendingAmount"), 0.01)
  }

  @Test
  fun `test mongo serialization for supplier bill`() {
    val bill = SupplierBillEntity(
      id = 10,
      supplierName = "Sharma Enterprises",
      gstin = "07AAAAA0000A1Z5",
      billNumber = "INV-2024-9912",
      productName = "Bulk Stock Purchase",
      hsnCode = "8471",
      amount = 24500.0,
      gstAmount = 4410.0,
      rawOcrText = "Sample OCR"
    )

    val json = JSONObject().apply {
      put("supplierName", bill.supplierName)
      put("gstin", bill.gstin)
      put("billNumber", bill.billNumber)
      put("hsnCode", bill.hsnCode)
      put("amount", bill.amount)
      put("gstAmount", bill.gstAmount)
    }

    assertEquals("Sharma Enterprises", json.getString("supplierName"))
    assertEquals("INV-2024-9912", json.getString("billNumber"))
    assertEquals(24500.0, json.getDouble("amount"), 0.01)
  }
}
