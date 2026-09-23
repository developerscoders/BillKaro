package com.example.ui.screens.invoices

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.utils.GstUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(invoiceId) {
        viewModel.loadInvoiceDetail(invoiceId)
    }

    val invoiceWithItems by viewModel.selectedInvoiceDetail.collectAsStateWithLifecycle()
    var showPaymentSuccessDialog by remember { mutableStateOf(false) }

    if (invoiceWithItems == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val inv = invoiceWithItems!!.invoice
    val items = invoiceWithItems!!.items
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val settingsManager = remember { com.example.data.local.BusinessSettingsManager.getInstance(context) }
    val businessName = settingsManager.businessName.ifBlank { "Business Invoice" }
    val businessGstin = settingsManager.businessGstin
    val businessAddress = settingsManager.businessAddress

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(inv.invoiceNumber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Sharing GST Invoice PDF via WhatsApp...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("action_share_invoice")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share Invoice")
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!inv.isPaid) {
                        Button(
                            onClick = {
                                viewModel.markPaid(inv.id)
                                showPaymentSuccessDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_pay_razorpay"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Collect Payment (Razorpay/UPI)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "PDF Invoice Exported to Downloads!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_download_pdf"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download GST PDF")
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
            // Invoice Status & Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TAX INVOICE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = IndigoPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = inv.invoiceNumber,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = dateFormatter.format(Date(inv.date)),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (inv.isPaid) EmeraldLight else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (inv.isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = if (inv.isPaid) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (inv.isPaid) "PAID" else "PENDING",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (inv.isPaid) EmeraldSuccess else MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // Biller & Customer details
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Billed By:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(businessName, fontWeight = FontWeight.Bold)
                                if (businessGstin.isNotBlank()) {
                                    Text("GSTIN: $businessGstin", fontSize = 12.sp, color = IndigoPrimary)
                                }
                                if (businessAddress.isNotBlank()) {
                                    Text(businessAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Billed To:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(inv.customerName, fontWeight = FontWeight.Bold)
                                if (!inv.customerGstin.isNull_or_blank()) {
                                    Text("GSTIN: ${inv.customerGstin}", fontSize = 12.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                                Text("Phone: ${inv.customerPhone ?: "N/A"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Items Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Itemized Products",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Qty", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Rate", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Total", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("HSN: ${item.hsnCode ?: "N/A"} | GST ${item.gstRate}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${item.quantity}", modifier = Modifier.weight(0.8f), fontSize = 13.sp)
                                Text("₹${item.unitPrice}", modifier = Modifier.weight(1.2f), fontSize = 13.sp)
                                Text(GstUtils.formatRupee(item.totalAmount), modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Tax Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tax & Amount Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sub Total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GstUtils.formatRupee(inv.subTotal))
                        }

                        if (inv.isInterstate) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IGST", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GstUtils.formatRupee(inv.igstAmount))
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST (9%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GstUtils.formatRupee(inv.cgstAmount))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SGST (9%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GstUtils.formatRupee(inv.sgstAmount))
                            }
                        }

                        if (inv.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", color = EmeraldSuccess)
                                Text("- ${GstUtils.formatRupee(inv.discount)}", color = EmeraldSuccess)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(GstUtils.formatRupee(inv.grandTotal), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = IndigoPrimary))
                        }
                    }
                }
            }
        }
    }

    // Payment Success Dialog
    if (showPaymentSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentSuccessDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(56.dp))
            },
            title = { Text("Payment Recorded") },
            text = {
                Text("Payment of ${GstUtils.formatRupee(inv.grandTotal)} recorded successfully for ${inv.invoiceNumber}.")
            },
            confirmButton = {
                Button(onClick = { showPaymentSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
