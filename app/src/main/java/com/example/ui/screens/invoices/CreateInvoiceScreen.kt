package com.example.ui.screens.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.utils.GstUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onInvoiceCreated: (Long) -> Unit,
    onAddCustomerClick: () -> Unit
) {
    val formState by viewModel.createUiState.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductPickerIndex by remember { mutableStateOf<Int?>(null) }

    val rawSubTotal = formState.items.sumOf { it.quantity * it.unitPrice }
    val taxBreakdown = GstUtils.calculateTax(
        subTotal = rawSubTotal,
        gstRatePercent = if (formState.items.isNotEmpty()) formState.items.first().gstRate else 18.0,
        isInterstate = formState.isInterstate,
        discount = formState.discount
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create GST Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = GstUtils.formatRupee(taxBreakdown.grandTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveInvoice { newId ->
                                onInvoiceCreated(newId)
                            }
                        },
                        enabled = formState.selectedCustomer != null && !formState.isSubmitting,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_save_invoice"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Invoice")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_select_customer"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Details",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(onClick = onAddCustomerClick) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Customer")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (formState.selectedCustomer == null) {
                            OutlinedButton(
                                onClick = { showCustomerPicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_choose_customer"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Customer from Directory")
                            }
                        } else {
                            val cust = formState.selectedCustomer!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Phone: ${cust.phone}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    if (!cust.gstin.isNull_or_blank()) {
                                        Text(
                                            text = "GSTIN: ${cust.gstin}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                IconButton(onClick = { showCustomerPicker = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Change Customer")
                                }
                            }
                        }
                    }
                }
            }

            // Products Header & Items
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Line Items",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.testTag("btn_add_line_item")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Product")
                    }
                }
            }

            itemsIndexed(formState.items) { index, itemForm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item #${index + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            if (formState.items.size > 1) {
                                IconButton(onClick = { viewModel.removeItem(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Product selector button
                        OutlinedButton(
                            onClick = { showProductPickerIndex = index },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_select_product_$index"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (itemForm.productName.isNotBlank()) itemForm.productName else "Select Product from Inventory",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quantity Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            ) {
                                IconButton(onClick = { viewModel.updateItemQuantity(index, itemForm.quantity - 1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus")
                                }
                                Text(
                                    text = "${itemForm.quantity}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { viewModel.updateItemQuantity(index, itemForm.quantity + 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus")
                                }
                            }

                            // Price & Total
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rate: ₹${itemForm.unitPrice} | GST: ${itemForm.gstRate}%",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "Total: " + GstUtils.formatRupee(itemForm.quantity * itemForm.unitPrice * (1 + itemForm.gstRate / 100.0)),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Tax Settings Card (Interstate Toggle, Discount, Payment Mode)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GST & Payment Configuration",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Interstate Sale (IGST)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (formState.isInterstate) "Applies IGST" else "Applies CGST + SGST",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Switch(
                                checked = formState.isInterstate,
                                onCheckedChange = { viewModel.setInterstate(it) },
                                modifier = Modifier.testTag("switch_interstate")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Payment Mode selector
                        Text("Payment Method", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("UPI", "Cash", "Credit", "Bank").forEach { mode ->
                                FilterChip(
                                    selected = formState.paymentMode == mode,
                                    onClick = { viewModel.setPaymentMode(mode) },
                                    label = { Text(mode) },
                                    modifier = Modifier.testTag("payment_chip_$mode")
                                )
                            }
                        }
                    }
                }
            }

            // Tax Summary Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Calculation Summary",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        SummaryRow("Subtotal", GstUtils.formatRupee(taxBreakdown.subTotal))

                        if (formState.isInterstate) {
                            SummaryRow("IGST", GstUtils.formatRupee(taxBreakdown.igstAmount))
                        } else {
                            SummaryRow("CGST", GstUtils.formatRupee(taxBreakdown.cgstAmount))
                            SummaryRow("SGST", GstUtils.formatRupee(taxBreakdown.sgstAmount))
                        }

                        SummaryRow("Total Tax", GstUtils.formatRupee(taxBreakdown.totalTax))
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("Grand Total", GstUtils.formatRupee(taxBreakdown.grandTotal), isBold = true)
                    }
                }
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedCustomer(cust)
                                    showCustomerPicker = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text("Phone: ${cust.phone}", fontSize = 12.sp)
                                if (!cust.gstin.isNull_or_blank()) {
                                    Text("GSTIN: ${cust.gstin}", fontSize = 11.sp, color = EmeraldSuccess)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("Cancel") }
            }
        )
    }

    // Product Selection Dialog
    if (showProductPickerIndex != null) {
        val targetIdx = showProductPickerIndex!!
        AlertDialog(
            onDismissRequest = { showProductPickerIndex = null },
            title = { Text("Select Product") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateItemProduct(targetIdx, prod)
                                    showProductPickerIndex = null
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    Text("HSN: ${prod.hsnCode ?: "N/A"} | GST: ${prod.gstRate}%", fontSize = 11.sp)
                                }
                                Text("₹${prod.sellingPrice}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductPickerIndex = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
