package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.repository.BillKaroRepository
import com.example.ui.theme.*
import com.example.ui.screens.customers.CustomerListScreen
import com.example.ui.screens.customers.CustomerViewModel
import com.example.ui.screens.customers.CustomerViewModelFactory
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.dashboard.DashboardViewModelFactory
import com.example.ui.screens.invoices.*
import com.example.ui.screens.products.ProductListScreen
import com.example.ui.screens.products.ProductViewModel
import com.example.ui.screens.products.ProductViewModelFactory
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.reports.ReportsViewModelFactory
import com.example.ui.screens.scan.ScanDocumentScreen
import com.example.ui.screens.scan.ScanViewModel
import com.example.ui.screens.scan.ScanViewModelFactory
import com.example.ui.screens.settings.SettingsScreen

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun BillKaroApp(repository: BillKaroRepository) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(
            title = "Dashboard",
            route = NavRoute.Dashboard.route,
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_dashboard"
        ),
        BottomNavItem(
            title = "Invoices",
            route = NavRoute.InvoiceList.route,
            selectedIcon = Icons.Filled.Receipt,
            unselectedIcon = Icons.Outlined.Receipt,
            testTag = "nav_invoices"
        ),
        BottomNavItem(
            title = "Scan OCR",
            route = NavRoute.ScanDocument.createRoute("business_card"),
            selectedIcon = Icons.Filled.DocumentScanner,
            unselectedIcon = Icons.Outlined.DocumentScanner,
            testTag = "nav_scan"
        ),
        BottomNavItem(
            title = "Products",
            route = NavRoute.ProductList.route,
            selectedIcon = Icons.Filled.Inventory,
            unselectedIcon = Icons.Outlined.Inventory2,
            testTag = "nav_products"
        ),
        BottomNavItem(
            title = "Customers",
            route = NavRoute.CustomerList.route,
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            testTag = "nav_customers"
        ),
        BottomNavItem(
            title = "GSTR-1",
            route = NavRoute.Reports.route,
            selectedIcon = Icons.Filled.Analytics,
            unselectedIcon = Icons.Outlined.Analytics,
            testTag = "nav_reports"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Show bottom bar on primary screens
            if (currentRoute in listOf(
                    NavRoute.Dashboard.route,
                    NavRoute.InvoiceList.route,
                    NavRoute.ProductList.route,
                    NavRoute.CustomerList.route,
                    NavRoute.Reports.route
                )
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SleekPrimary,
                                selectedTextColor = SleekPrimary,
                                indicatorColor = SleekPrimaryContainer,
                                unselectedIconColor = SleekTextSecondary,
                                unselectedTextColor = SleekTextSecondary
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Dashboard
            composable(NavRoute.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(repository))
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onCreateInvoiceClick = { navController.navigate(NavRoute.CreateInvoice.route) },
                    onScanClick = { mode -> navController.navigate(NavRoute.ScanDocument.createRoute(mode)) },
                    onAddProductClick = { navController.navigate(NavRoute.ProductList.route) },
                    onAddCustomerClick = { navController.navigate(NavRoute.CustomerList.route) },
                    onInvoiceClick = { id -> navController.navigate(NavRoute.InvoiceDetail.createRoute(id)) },
                    onViewAllInvoicesClick = { navController.navigate(NavRoute.InvoiceList.route) },
                    onSettingsClick = { navController.navigate(NavRoute.Settings.route) }
                )
            }

            // Invoices List
            composable(NavRoute.InvoiceList.route) {
                val invoiceViewModel: InvoiceViewModel = viewModel(factory = InvoiceViewModelFactory(repository))
                InvoiceListScreen(
                    viewModel = invoiceViewModel,
                    onCreateInvoiceClick = { navController.navigate(NavRoute.CreateInvoice.route) },
                    onInvoiceClick = { id -> navController.navigate(NavRoute.InvoiceDetail.createRoute(id)) }
                )
            }

            // Create Invoice
            composable(NavRoute.CreateInvoice.route) {
                val invoiceViewModel: InvoiceViewModel = viewModel(factory = InvoiceViewModelFactory(repository))
                CreateInvoiceScreen(
                    viewModel = invoiceViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onInvoiceCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(NavRoute.InvoiceDetail.createRoute(id))
                    },
                    onAddCustomerClick = { navController.navigate(NavRoute.CustomerList.route) }
                )
            }

            // Invoice Detail
            composable(
                route = NavRoute.InvoiceDetail.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                val invoiceViewModel: InvoiceViewModel = viewModel(factory = InvoiceViewModelFactory(repository))
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    viewModel = invoiceViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Camera OCR Scan Screen
            composable(
                route = NavRoute.ScanDocument.route,
                arguments = listOf(navArgument("mode") { defaultValue = "business_card" })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "business_card"
                val scanViewModel: ScanViewModel = viewModel(factory = ScanViewModelFactory(repository))
                ScanDocumentScreen(
                    initialScanMode = mode,
                    viewModel = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCustomerCreated = {
                        navController.navigate(NavRoute.CustomerList.route) {
                            popUpTo(NavRoute.Dashboard.route)
                        }
                    },
                    onSupplierBillCreated = {
                        navController.navigate(NavRoute.Dashboard.route) {
                            popUpTo(NavRoute.Dashboard.route)
                        }
                    }
                )
            }

            // Product Inventory
            composable(NavRoute.ProductList.route) {
                val productViewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(repository))
                ProductListScreen(
                    viewModel = productViewModel,
                    onScanBarcodeClick = { navController.navigate(NavRoute.ScanDocument.createRoute("barcode")) }
                )
            }

            // Customer Directory
            composable(NavRoute.CustomerList.route) {
                val customerViewModel: CustomerViewModel = viewModel(factory = CustomerViewModelFactory(repository))
                CustomerListScreen(
                    viewModel = customerViewModel,
                    onScanBusinessCardClick = { navController.navigate(NavRoute.ScanDocument.createRoute("business_card")) }
                )
            }

            // GSTR-1 Tax Reports
            composable(NavRoute.Reports.route) {
                val reportsViewModel: ReportsViewModel = viewModel(factory = ReportsViewModelFactory(repository))
                ReportsScreen(viewModel = reportsViewModel)
            }

            // Settings & Environment Configuration
            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
