package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AnimatedStatCard
import com.example.ui.components.GstDonutChart
import com.example.ui.components.SwipeableInvoiceRow
import com.example.ui.theme.*
import com.example.ui.utils.GstUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateInvoiceClick: () -> Unit,
    onScanClick: (String) -> Unit, // "business_card", "supplier_bill", "barcode"
    onAddProductClick: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onViewAllInvoicesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val businessName = remember { com.example.data.local.BusinessSettingsManager.getInstance(context).businessName }
    val avatarInitial = remember(businessName) {
        if (businessName.isNotBlank()) businessName.take(2).uppercase() else "BK"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "BUSINESS MODE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SleekPrimary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "BillKaro",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                fontSize = 24.sp
                            )
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                viewModel.syncWithMongo { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync from MongoDB",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekPrimaryContainer)
                            .clickable { onScanClick("barcode") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search / Scan",
                            tint = SleekOnPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary)
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarInitial,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBackground
                )
            )
        },
        containerColor = SleekBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SleekBackground),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Key Metrics Section (2 Card Row per Mockup)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Outstanding
                    AnimatedStatCard(
                        title = "Outstanding",
                        value = GstUtils.formatRupee(uiState.outstandingTotal),
                        icon = Icons.Default.PendingActions,
                        iconBgColor = SleekOutstandingBadgeBg,
                        iconColor = SleekOutstandingRed,
                        badgeText = if (uiState.outstandingTotal > 0) "Pending" else "Nil",
                        badgeBgColor = SleekOutstandingBadgeBg,
                        badgeTextColor = SleekOutstandingBadgeText,
                        cardBgColor = Color.White,
                        valueColor = SleekOutstandingRed,
                        titleColor = SleekTextMuted,
                        borderColor = SleekBorder,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Total Sales
                    AnimatedStatCard(
                        title = "Total Sales",
                        value = GstUtils.formatRupee(uiState.totalSales),
                        icon = Icons.Default.TrendingUp,
                        iconBgColor = Color.White.copy(alpha = 0.6f),
                        iconColor = SleekOnPrimaryContainer,
                        badgeText = "Total",
                        badgeBgColor = Color.White.copy(alpha = 0.5f),
                        badgeTextColor = SleekOnPrimaryContainer,
                        cardBgColor = SleekPrimaryContainer,
                        valueColor = SleekOnPrimaryContainer,
                        titleColor = SleekTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // OCR Smart Scan Quick Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_ocr_smart_scan"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekDarkContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SleekDarkContainerAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "OCR Smart Scan",
                                    tint = SleekDarkContainerIconText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "OCR Smart Scan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Scan bills or business cards",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SleekDarkContainerAccent,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onScanClick("business_card") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = "Scan Now",
                                tint = SleekDarkContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Horizontal Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "New Bill",
                        icon = Icons.Default.Add,
                        bgColor = SleekPrimary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_create_invoice"),
                        onClick = onCreateInvoiceClick
                    )

                    QuickActionButton(
                        title = "Scan Card",
                        icon = Icons.Default.ContactPage,
                        bgColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_scan_card"),
                        onClick = { onScanClick("business_card") }
                    )

                    QuickActionButton(
                        title = "Scan Bill",
                        icon = Icons.Default.DocumentScanner,
                        bgColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_scan_bill"),
                        onClick = { onScanClick("supplier_bill") }
                    )

                    QuickActionButton(
                        title = "Barcode",
                        icon = Icons.Outlined.QrCodeScanner,
                        bgColor = Color(0xFF059669),
                        contentColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_scan_barcode_quick"),
                        onClick = { onScanClick("barcode") }
                    )
                }
            }

            // Secondary Overview Grid (GST & Customers)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnimatedStatCard(
                        title = "GST Collected",
                        value = GstUtils.formatRupee(uiState.totalGstCollected),
                        icon = Icons.Default.ReceiptLong,
                        iconBgColor = SleekPrimaryContainer,
                        iconColor = SleekPrimary,
                        cardBgColor = Color.White,
                        borderColor = SleekBorder,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedStatCard(
                        title = "Customers",
                        value = "${uiState.totalCustomers}",
                        icon = Icons.Default.People,
                        iconBgColor = SleekDarkContainerAccent.copy(alpha = 0.3f),
                        iconColor = SleekDarkContainer,
                        cardBgColor = Color.White,
                        borderColor = SleekBorder,
                        modifier = Modifier.weight(1f),
                        onClick = onAddCustomerClick
                    )
                }
            }

            // GST Donut Chart
            item {
                GstDonutChart(
                    cgst = uiState.gstSummary.totalCgst,
                    sgst = uiState.gstSummary.totalSgst,
                    igst = uiState.gstSummary.totalIgst,
                    totalTax = uiState.gstSummary.totalTax
                )
            }

            // Recent Invoices Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT INVOICES",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextSecondary,
                            letterSpacing = 1.2.sp,
                            fontSize = 12.sp
                        )
                    )

                    TextButton(
                        onClick = onViewAllInvoicesClick,
                        modifier = Modifier.testTag("btn_view_all_invoices")
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = SleekPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Recent Invoices Items List
            if (uiState.recentInvoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = SleekTextMuted.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No invoices created yet",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SleekTextMuted)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onCreateInvoiceClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Create First Invoice")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.recentInvoices,
                    key = { it.invoice.id }
                ) { invWithItems ->
                    SwipeableInvoiceRow(
                        invoiceWithItems = invWithItems,
                        onClick = { onInvoiceClick(invWithItems.invoice.id) },
                        onMarkPaid = { viewModel.markInvoicePaid(invWithItems.invoice.id) },
                        onDelete = { viewModel.deleteInvoice(invWithItems.invoice.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(70.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

