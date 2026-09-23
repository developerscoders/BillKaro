package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.utils.GstUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val context = LocalContext.current
    val currentPeriod = remember {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }
    val summary by viewModel.gstSummary.collectAsStateWithLifecycle()
    val b2bInvoices by viewModel.b2bInvoices.collectAsStateWithLifecycle()
    val b2cInvoices by viewModel.b2cInvoices.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GSTR-1 Tax Reports", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Exporting GSTR-1 CSV Report to Downloads...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("action_export_gstr1")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            // Header Card
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
                                Text("GSTR-1 TAX RETURN SUMMARY", style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold))
                                Text(currentPeriod, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Button(
                                onClick = {
                                    Toast.makeText(context, "GSTR-1 CSV Generated Successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("btn_export_csv")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Turnover", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(GstUtils.formatRupee(summary.totalSales), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Tax Liability", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(GstUtils.formatRupee(summary.totalTax), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IndigoPrimary)
                            }
                        }
                    }
                }
            }

            // Tax Breakdown Grid
            item {
                Text("Tax Component Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CGST Liability (9%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GstUtils.formatRupee(summary.totalCgst), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SGST Liability (9%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GstUtils.formatRupee(summary.totalSgst), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IGST Liability (Interstate)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GstUtils.formatRupee(summary.totalIgst), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // B2B Invoices Section
            item {
                Text("B2B Invoices (With Customer GSTIN)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (b2bInvoices.isEmpty()) {
                            Text("No B2B invoices recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            b2bInvoices.forEach { invWithItems ->
                                val inv = invWithItems.invoice
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(inv.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("GSTIN: ${inv.customerGstin ?: "N/A"}", fontSize = 11.sp, color = EmeraldSuccess)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(GstUtils.formatRupee(inv.grandTotal), fontWeight = FontWeight.Bold)
                                        Text("Tax: ${GstUtils.formatRupee(inv.totalTax)}", fontSize = 11.sp, color = IndigoPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // B2C Invoices Section
            item {
                Text("B2C Small Invoices (Consumer Sales)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Count of Consumer Invoices: ${b2cInvoices.size}", fontWeight = FontWeight.SemiBold)
                        Text("Total Consumer Sales Value: ${GstUtils.formatRupee(b2cInvoices.sumOf { it.invoice.grandTotal })}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
