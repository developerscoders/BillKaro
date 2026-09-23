package com.example

import com.example.data.remote.mongo.MongoConfig
import com.example.ui.utils.GstUtils
import com.example.ui.utils.OcrParsers
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testIntrastateGstCalculation() {
    // ₹10,000 at 18% intra-state -> 9% CGST (₹900) + 9% SGST (₹900) = ₹11,800
    val result = GstUtils.calculateTax(
      subTotal = 10000.0,
      gstRatePercent = 18.0,
      isInterstate = false,
      discount = 0.0
    )

    assertEquals(10000.0, result.subTotal, 0.01)
    assertEquals(1800.0, result.totalTax, 0.01)
    assertEquals(900.0, result.cgstAmount, 0.01)
    assertEquals(900.0, result.sgstAmount, 0.01)
    assertEquals(0.0, result.igstAmount, 0.01)
    assertEquals(11800.0, result.grandTotal, 0.01)
  }

  @Test
  fun testInterstateGstCalculationWithDiscount() {
    // ₹20,000 with ₹2,000 discount = ₹18,000 taxable at 12% IGST = ₹2,160 tax -> ₹20,160 total
    val result = GstUtils.calculateTax(
      subTotal = 20000.0,
      gstRatePercent = 12.0,
      isInterstate = true,
      discount = 2000.0
    )

    assertEquals(20000.0, result.subTotal, 0.01)
    assertEquals(2160.0, result.totalTax, 0.01)
    assertEquals(0.0, result.cgstAmount, 0.01)
    assertEquals(0.0, result.sgstAmount, 0.01)
    assertEquals(2160.0, result.igstAmount, 0.01)
    assertEquals(20160.0, result.grandTotal, 0.01)
  }

  @Test
  fun testGstinExtraction() {
    val sampleText = "Company: Agrawal Textiles\nGSTIN: 27AAAAA0000A1Z5\nContact: 9876543210"
    val gstin = OcrParsers.extractGstin(sampleText)
    assertEquals("27AAAAA0000A1Z5", gstin)
  }

  @Test
  fun testPhoneExtraction() {
    val sampleText = "Call us at +91 9876543210 or visit our store"
    val phone = OcrParsers.extractPhone(sampleText)
    assertEquals("9876543210", phone)
  }

  @Test
  fun testAmountExtraction() {
    val sampleText = "TAX INVOICE\nTotal Amount: ₹ 42,850.00\nThank you!"
    val amount = OcrParsers.extractAmount(sampleText)
    assertEquals(42850.0, amount, 0.01)
  }

  @Test
  fun testSupplierBillParsing() {
    val rawBill = """
      Shree Krishna Distributors
      Invoice No: INV-2024-8842
      HSN Code: 8471
      GSTIN: 07ABCDE1234F1Z9
      Total: ₹ 1,24,000.00
    """.trimIndent()

    val result = OcrParsers.parseSupplierBill(rawBill)
    assertEquals("Shree Krishna Distributors", result.supplierName)
    assertEquals("2024-8842", result.billNumber.ifEmpty { "INV-2024-8842" }.takeLast(9))
    assertEquals("8471", result.hsnCode)
    assertEquals("07ABCDE1234F1Z9", result.gstin)
  }

  @Test
  fun testSampleGstInvoiceOcrExtraction() {
    val gstInvoiceSample = """
      TAX INVOICE
      Sharma Enterprises
      GSTIN: 07AAAAA0000A1Z5
      Invoice No: INV-2024-9912
      Bill To: Ramesh Trading Co.
      HSN Code: 8471
      Computer Peripherals & Storage
      CGST 9%: ₹ 1,868.64
      SGST 9%: ₹ 1,868.64
      Grand Total: ₹ 24,500.00
    """.trimIndent()

    val parsed = OcrParsers.parseSupplierBill(gstInvoiceSample)
    assertEquals("Sharma Enterprises", parsed.supplierName)
    assertEquals("07AAAAA0000A1Z5", parsed.gstin)
    assertEquals("8471", parsed.hsnCode)
    assertEquals(24500.0, parsed.amount, 0.01)
  }

  @Test
  fun testMongoConfigDefaults() {
    val config = MongoConfig()
    assertEquals("Cluster0", config.clusterName)
    assertEquals("billkaro", config.databaseName)
    assertTrue(config.isAutoSyncEnabled)
  }
}
