package com.example.ui.utils

import java.util.regex.Pattern

data class BusinessCardOcrResult(
    val name: String = "",
    val phone: String = "",
    val gstin: String = "",
    val address: String = "",
    val rawText: String = ""
)

data class SupplierBillOcrResult(
    val supplierName: String = "",
    val billNumber: String = "",
    val hsnCode: String = "",
    val amount: Double = 0.0,
    val gstin: String = "",
    val rawText: String = ""
)

object OcrParsers {

    fun extractGstin(text: String): String {
        val pattern = Pattern.compile("\\b\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}\\b", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group().uppercase() else ""
    }

    fun extractPhone(text: String): String {
        val pattern = Pattern.compile("\\b(?:\\+91[\\-\\s]?)?[6-9]\\d{9}\\b")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            val phone = matcher.group().replace(Regex("[^0-9]"), "")
            if (phone.length > 10) phone.takeLast(10) else phone
        } else ""
    }

    fun extractAmount(text: String): Double {
        // Priority 1: Check for Grand Total / Total Amount / Net Amount / Invoice Total
        val totalPattern = Pattern.compile("(?:Grand\\s*Total|Total\\s*Amount|Net\\s*Total|Invoice\\s*Total|Total)\\s*[:\\-=]*\\s*(?:₹|Rs\\.?|INR)?\\s*([\\d,]+\\.?\\d{0,2})", Pattern.CASE_INSENSITIVE)
        val totalMatcher = totalPattern.matcher(text)
        var lastTotalAmount: Double? = null
        while (totalMatcher.find()) {
            val raw = totalMatcher.group(1)?.replace(",", "") ?: ""
            raw.toDoubleOrNull()?.let { lastTotalAmount = it }
        }
        if (lastTotalAmount != null && lastTotalAmount!! > 0.0) {
            return lastTotalAmount!!
        }

        // Priority 2: Matches currency pattern like ₹ 1,500.00, Rs. 1500 and take maximum
        val pattern = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([\\d,]+\\.?\\d{0,2})", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        var maxCurrencyAmount = 0.0
        while (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: ""
            val amt = raw.toDoubleOrNull() ?: 0.0
            if (amt > maxCurrencyAmount && amt < 10000000) {
                maxCurrencyAmount = amt
            }
        }
        if (maxCurrencyAmount > 0.0) {
            return maxCurrencyAmount
        }

        // Fallback: look for largest monetary number in text
        val numberPattern = Pattern.compile("\\b\\d{3,7}(?:\\.\\d{1,2})?\\b")
        val numMatcher = numberPattern.matcher(text)
        var maxAmount = 0.0
        while (numMatcher.find()) {
            val amt = numMatcher.group().toDoubleOrNull() ?: 0.0
            if (amt > maxAmount && amt < 10000000) { // filter out dates/phone numbers
                maxAmount = amt
            }
        }
        return maxAmount
    }

    fun extractHsn(text: String): String {
        val pattern = Pattern.compile("\\b(?:HSN|SAC)(?:\\s*Code)?[\\s:]*(\\d{4,8})\\b", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    fun extractBillNumber(text: String): String {
        val pattern = Pattern.compile("\\b(?:Invoice|Bill|INV)(?:\\s*(?:No|Num|Number)\\.?)?[\\s#:-]+([A-Z0-9\\-/]+)\\b", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    fun parseBusinessCard(fullText: String): BusinessCardOcrResult {
        val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val name = lines.firstOrNull { line ->
            !line.contains("GST", ignoreCase = true) &&
            !line.contains("Phone", ignoreCase = true) &&
            !line.contains("Tel", ignoreCase = true) &&
            !line.contains("Address", ignoreCase = true) &&
            line.length in 3..30
        } ?: lines.firstOrNull() ?: ""

        val phone = extractPhone(fullText)
        val gstin = extractGstin(fullText)
        val addressLines = lines.filter { line ->
            line.contains("Street", ignoreCase = true) ||
            line.contains("Road", ignoreCase = true) ||
            line.contains("Nagar", ignoreCase = true) ||
            line.contains("Market", ignoreCase = true) ||
            line.contains("City", ignoreCase = true) ||
            line.contains("Pin", ignoreCase = true) ||
            line.contains("India", ignoreCase = true)
        }
        val address = if (addressLines.isNotEmpty()) addressLines.joinToString(", ") else ""

        return BusinessCardOcrResult(
            name = name,
            phone = phone,
            gstin = gstin,
            address = address,
            rawText = fullText
        )
    }

    fun parseSupplierBill(fullText: String): SupplierBillOcrResult {
        val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val supplierName = lines.firstOrNull { line ->
            !line.equals("TAX INVOICE", ignoreCase = true) &&
            !line.equals("INVOICE", ignoreCase = true) &&
            !line.equals("RETAIL INVOICE", ignoreCase = true) &&
            !line.equals("BILL OF SUPPLY", ignoreCase = true) &&
            !line.contains("ORIGINAL FOR", ignoreCase = true) &&
            !line.contains("DUPLICATE FOR", ignoreCase = true) &&
            !line.contains("GSTIN", ignoreCase = true) &&
            line.length in 3..60
        } ?: lines.firstOrNull() ?: ""
        val billNum = extractBillNumber(fullText)
        val hsn = extractHsn(fullText)
        val amount = extractAmount(fullText)
        val gstin = extractGstin(fullText)

        return SupplierBillOcrResult(
            supplierName = supplierName,
            billNumber = billNum,
            hsnCode = hsn,
            amount = amount,
            gstin = gstin,
            rawText = fullText
        )
    }
}
