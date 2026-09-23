package com.example.ui.navigation

sealed class NavRoute(val route: String) {
    object Dashboard : NavRoute("dashboard")
    object InvoiceList : NavRoute("invoice_list")
    object CreateInvoice : NavRoute("create_invoice")
    object InvoiceDetail : NavRoute("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: Long) = "invoice_detail/$invoiceId"
    }
    object ScanDocument : NavRoute("scan_document?mode={mode}") {
        fun createRoute(mode: String = "business_card") = "scan_document?mode=$mode"
    }
    object ProductList : NavRoute("product_list")
    object CustomerList : NavRoute("customer_list")
    object Reports : NavRoute("reports")
    object Settings : NavRoute("settings")
}
