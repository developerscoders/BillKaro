package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BusinessSettingsManager
import com.example.data.remote.mongo.MongoSettingsManager
import com.example.data.repository.BillKaroRepository
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: BillKaroRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { BusinessSettingsManager.getInstance(context) }
    val mongoSettingsManager = remember { MongoSettingsManager(context) }

    // Business Profile Fields
    var businessName by remember { mutableStateOf(settingsManager.businessName) }
    var businessGstin by remember { mutableStateOf(settingsManager.businessGstin) }
    var businessPhone by remember { mutableStateOf(settingsManager.businessPhone) }
    var businessAddress by remember { mutableStateOf(settingsManager.businessAddress) }
    var upiId by remember { mutableStateOf(settingsManager.upiId) }

    // Cloud Gateway Configuration
    var mongoConfig by remember { mutableStateOf(mongoSettingsManager.getConfig()) }
    var gatewayUrlInput by remember { mutableStateOf(mongoConfig.gatewayUrl) }
    var gatewayTokenInput by remember { mutableStateOf(mongoConfig.authToken) }
    var isTokenVisible by remember { mutableStateOf(false) }

    // Async states
    var isTestingMongo by remember { mutableStateOf(false) }
    var isSyncingMongo by remember { mutableStateOf(false) }
    var mongoStatusMessage by remember { mutableStateOf<String?>(null) }
    var isMongoSuccess by remember { mutableStateOf<Boolean?>(null) }
    var lastSyncStatus by remember { mutableStateOf(mongoSettingsManager.getLastSyncStatus()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Cloud Database", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = SleekBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SleekBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Production-Ready Cloud Sync Gateway Card (Zero Database Secrets in APK)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldSuccess
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Cloud Sync Gateway",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "APK Protected",
                                        color = EmeraldSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Security explanation banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Zero Database Secrets in Client APK",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = EmeraldSuccess
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your MongoDB credentials are never baked into this APK. The app connects exclusively via your HTTPS sync gateway, so decompiling the APK reveals zero database passwords.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gateway URL Field
                        OutlinedTextField(
                            value = gatewayUrlInput,
                            onValueChange = { gatewayUrlInput = it },
                            label = { Text("Sync Gateway URL") },
                            placeholder = { Text("https://api.yourdomain.com or Cloud Run URL") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = IndigoPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gateway_url")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Optional Gateway Auth Token (Protected with password transformation)
                        OutlinedTextField(
                            value = gatewayTokenInput,
                            onValueChange = { gatewayTokenInput = it },
                            label = { Text("Gateway Bearer Token (Optional)") },
                            placeholder = { Text("Device or user session token") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = IndigoPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                    Icon(
                                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isTokenVisible) "Hide token" else "Show token"
                                    )
                                }
                            },
                            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gateway_token")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save Gateway URL button
                        OutlinedButton(
                            onClick = {
                                mongoSettingsManager.saveGatewayOverride(gatewayUrlInput, gatewayTokenInput)
                                mongoConfig = mongoSettingsManager.getConfig()
                                Toast.makeText(context, "Gateway URL saved successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_gateway_url")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Gateway URL")
                        }

                        // Status Banner (Result of test/sync)
                        if (mongoStatusMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isMongoSuccess == true) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMongoSuccess == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isMongoSuccess == true) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = mongoStatusMessage ?: "",
                                        fontSize = 12.sp,
                                        color = if (isMongoSuccess == true) EmeraldSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Connection Button
                        Button(
                            onClick = {
                                isTestingMongo = true
                                mongoStatusMessage = null
                                coroutineScope.launch {
                                    val res = repository.testMongoConnection()
                                    isTestingMongo = false
                                    isMongoSuccess = res.success
                                    mongoStatusMessage = if (res.success) {
                                        "${res.message} (${res.latencyMs}ms)"
                                    } else {
                                        res.message
                                    }
                                }
                            },
                            enabled = !isTestingMongo && !isSyncingMongo,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_test_mongo_connection")
                        ) {
                            if (isTestingMongo) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting to Gateway...")
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Gateway Connection")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fetch and Push Action Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    isSyncingMongo = true
                                    mongoStatusMessage = null
                                    coroutineScope.launch {
                                        val res = repository.fetchFromMongo()
                                        isSyncingMongo = false
                                        isMongoSuccess = res.success
                                        mongoStatusMessage = res.message
                                        lastSyncStatus = mongoSettingsManager.getLastSyncStatus()
                                    }
                                },
                                enabled = !isSyncingMongo && !isTestingMongo,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_fetch_from_mongo")
                            ) {
                                if (isSyncingMongo) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetching...")
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetch Cloud Data")
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    isSyncingMongo = true
                                    mongoStatusMessage = null
                                    coroutineScope.launch {
                                        val res = repository.pushToMongo()
                                        isSyncingMongo = false
                                        isMongoSuccess = res.success
                                        mongoStatusMessage = res.message
                                        lastSyncStatus = mongoSettingsManager.getLastSyncStatus()
                                    }
                                },
                                enabled = !isSyncingMongo && !isTestingMongo,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_push_to_mongo")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Push Local Data")
                            }
                        }

                        if (mongoConfig.lastSyncTime > 0) {
                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            Text(
                                text = "Last Sync: ${sdf.format(Date(mongoConfig.lastSyncTime))} ($lastSyncStatus)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }

            // Business Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = SleekPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Business Profile & Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "These details will appear on your generated invoices and receipts.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business / Store Name") },
                            placeholder = { Text("Enter your business name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_business_name")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = businessGstin,
                            onValueChange = { businessGstin = it },
                            label = { Text("Your GSTIN (Optional)") },
                            placeholder = { Text("e.g. 27AAAAA0000A1Z5") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_business_gstin")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = businessPhone,
                            onValueChange = { businessPhone = it },
                            label = { Text("Contact Phone") },
                            placeholder = { Text("Enter contact number") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_business_phone")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = businessAddress,
                            onValueChange = { businessAddress = it },
                            label = { Text("Business Address") },
                            placeholder = { Text("Enter street, city, state") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_business_address")
                        )
                    }
                }
            }

            // Payment Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = SleekPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Payment Collection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Set your UPI ID to print QR payment codes on customer invoices.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI VPA / ID") },
                            placeholder = { Text("e.g. yourstore@upi") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_upi_id")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                settingsManager.businessName = businessName
                                settingsManager.businessGstin = businessGstin
                                settingsManager.businessPhone = businessPhone
                                settingsManager.businessAddress = businessAddress
                                settingsManager.upiId = upiId
                                Toast.makeText(context, "Business Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_save_settings"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Business Profile")
                        }
                    }
                }
            }
        }
    }
}
