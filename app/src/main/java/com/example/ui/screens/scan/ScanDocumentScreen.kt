package com.example.ui.screens.scan

import android.Manifest
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ScannerOverlay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.utils.GstUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanDocumentScreen(
    initialScanMode: String = "business_card",
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onCustomerCreated: () -> Unit,
    onSupplierBillCreated: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(initialScanMode) {
        viewModel.setScanMode(initialScanMode)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Camera take picture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.processBitmap(it, context, uiState.scanMode)
        }
    }

    // Gallery Image Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.processBitmap(bitmap, context, uiState.scanMode)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BillKaro Camera OCR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.testTag("action_pick_gallery")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = "Pick from Gallery",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = when (uiState.scanMode) {
                    "supplier_bill" -> 1
                    "barcode" -> 2
                    else -> 0
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = uiState.scanMode == "business_card",
                    onClick = { viewModel.setScanMode("business_card") },
                    text = { Text("Business Card") },
                    modifier = Modifier.testTag("tab_scan_card")
                )
                Tab(
                    selected = uiState.scanMode == "supplier_bill",
                    onClick = { viewModel.setScanMode("supplier_bill") },
                    text = { Text("Supplier Bill") },
                    modifier = Modifier.testTag("tab_scan_bill")
                )
                Tab(
                    selected = uiState.scanMode == "barcode",
                    onClick = { viewModel.setScanMode("barcode") },
                    text = { Text("Barcode") },
                    modifier = Modifier.testTag("tab_scan_barcode")
                )
            }

            // Camera View / Frame Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!cameraPermissionState.status.isGranted) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera access required to scan documents & barcodes",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            modifier = Modifier.testTag("btn_grant_camera_permission")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                } else {
                    // Camera Scanner Frame Overlay
                    ScannerOverlay(
                        scanMode = uiState.scanMode,
                        hint = when (uiState.scanMode) {
                            "barcode" -> "Align Product Barcode inside frame"
                            "supplier_bill" -> "Align Supplier Purchase Invoice inside frame"
                            else -> "Align Visiting Card inside frame"
                        }
                    )
                }

                if (uiState.isProcessing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = IndigoPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Running On-Device MLKit OCR...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Trigger Controls Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.testTag("btn_upload_gallery")
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }

                    // Main Camera Capture Button
                    FloatingActionButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                takePictureLauncher.launch(null)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        containerColor = IndigoPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("btn_capture_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Capture Photo",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Test Sample GST Bill Button
                    Button(
                        onClick = {
                            viewModel.processSampleGstInvoice(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("btn_test_sample_gst_invoice")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test GST Bill")
                    }
                }
            }
        }
    }

    // Result Bottom Sheet Dialogs
    if (uiState.cardResult != null) {
        val card = uiState.cardResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactPage, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Business Card Scanned!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${card.name.ifBlank { "N/A" }}", fontWeight = FontWeight.Bold)
                    Text("Phone: ${card.phone.ifBlank { "N/A" }}", fontSize = 13.sp)
                    if (card.gstin.isNotBlank()) {
                        Text("GSTIN: ${card.gstin}", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (card.address.isNotBlank()) {
                        Text("Address: ${card.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Will be saved to local database and synced to MongoDB collection: customers",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCardAsCustomer {
                            onCustomerCreated()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_scanned_customer")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to MongoDB & Khata")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearResult() }) {
                    Text("Retake")
                }
            }
        )
    }

    if (uiState.billResult != null) {
        val bill = uiState.billResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GST Invoice Scanned!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Supplier: ${bill.supplierName}", fontWeight = FontWeight.Bold)
                    Text("Bill #: ${bill.billNumber.ifBlank { "N/A" }}", fontSize = 13.sp)
                    Text("HSN Code: ${bill.hsnCode.ifBlank { "N/A" }}", fontSize = 13.sp)
                    Text("Amount: ${GstUtils.formatRupee(bill.amount)}", fontWeight = FontWeight.Bold, color = IndigoPrimary)
                    if (bill.gstin.isNotBlank()) {
                        Text("Supplier GSTIN: ${bill.gstin}", color = EmeraldSuccess, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Will be saved to local Khata and synced to MongoDB collection: supplier_bills",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSupplierBill {
                            onSupplierBillCreated()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_supplier_bill")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to MongoDB & Khata")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearResult() }) {
                    Text("Retake")
                }
            }
        )
    }

    if (uiState.barcodeResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Barcode Detected")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scanned Barcode: ${uiState.barcodeResult}", fontWeight = FontWeight.Bold)
                    if (uiState.matchedProduct != null) {
                        val prod = uiState.matchedProduct!!
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(prod.name, fontWeight = FontWeight.Bold)
                                Text("Selling Price: ₹${prod.sellingPrice}", color = IndigoPrimary)
                                Text("Current Stock: ${prod.stockQuantity} Pcs")
                            }
                        }
                    } else {
                        Text("No matching product found in local inventory. You can use this barcode to create a new product.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearResult() }) {
                    Text("Done")
                }
            }
        )
    }
}
