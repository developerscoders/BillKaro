package com.example.ui.utils

import java.text.NumberFormat
import java.util.Locale

object GstUtils {

    fun formatRupee(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }

    data class TaxBreakdown(
        val subTotal: Double,
        val totalTax: Double,
        val cgstAmount: Double,
        val sgstAmount: Double,
        val igstAmount: Double,
        val grandTotal: Double
    )

    fun calculateTax(
        subTotal: Double,
        gstRatePercent: Double,
        isInterstate: Boolean,
        discount: Double = 0.0
    ): TaxBreakdown {
        val discountedSubTotal = (subTotal - discount).coerceAtLeast(0.0)
        val totalTax = discountedSubTotal * (gstRatePercent / 100.0)

        val cgst: Double
        val sgst: Double
        val igst: Double

        if (isInterstate) {
            cgst = 0.0
            sgst = 0.0
            igst = totalTax
        } else {
            cgst = totalTax / 2.0
            sgst = totalTax / 2.0
            igst = 0.0
        }

        val grandTotal = discountedSubTotal + totalTax

        return TaxBreakdown(
            subTotal = subTotal,
            totalTax = totalTax,
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = igst,
            grandTotal = grandTotal
        )
    }
}
