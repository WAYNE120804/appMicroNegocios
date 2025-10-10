package com.sebas.tiendaropa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.sebas.tiendaropa.ui.categories.CategoriesScreen
import com.sebas.tiendaropa.ui.categories.CategoriesViewModel
import com.sebas.tiendaropa.ui.customers.CustomersScreen
import com.sebas.tiendaropa.ui.customers.CustomersViewModel
import com.sebas.tiendaropa.ui.home.HomeScreen
import com.sebas.tiendaropa.ui.products.ProductsScreen
import com.sebas.tiendaropa.ui.products.ProductsViewModel
import com.sebas.tiendaropa.ui.settings.SettingsScreen
import com.sebas.tiendaropa.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val customersVm: CustomersViewModel by viewModels {
        CustomersViewModel.factory(applicationContext)
    }
    private val categoriesVm: CategoriesViewModel by viewModels {
        CategoriesViewModel.factory(applicationContext)
    }
    private val settingsVm: SettingsViewModel by viewModels {
        SettingsViewModel.factory(applicationContext)
    }

    private val productsVm: ProductsViewModel by viewModels {
        ProductsViewModel.factory(applicationContext)
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

                // Drawer state
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // 👇 Ítems del menú (agrega los que quieras después)
                data class DrawerItem(val route: String, val label: String, val icon: ImageVector)
                val items = listOf(
                    DrawerItem(Routes.Home,       "Inicio",        Icons.Default.Home),
                    DrawerItem(Routes.Customers,  "Clientes",      Icons.Default.People),
                    DrawerItem(Routes.Categories, "Categorías",    Icons.Default.Category),
                    DrawerItem(Routes.Products,   "Productos",     Icons.Default.ShoppingCart),
                    DrawerItem(Routes.Settings,   "Configuración", Icons.Default.Settings),
                )

                val settingsState = settingsVm.state.collectAsState().value
                val totalCompras = productsVm.totalCompras.collectAsState().value
                val totalGanancia = productsVm.totalGanancia.collectAsState().value

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text(
                                text = settingsState.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                            items.forEach { item ->
                                NavigationDrawerItem(
                                    label = { Text(item.label) },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        nav.navigate(item.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(settingsState.storeName)
                                        Spacer(Modifier.width(12.dp))
                                        // Logo (un poco más grande)
                                        settingsState.logoUri?.let {
                                            AsyncImage(
                                                model = it,
                                                contentDescription = "Logo",
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                                    }
                                }
                            )
                        }
                    ) { inner ->
                        NavHost(
                            navController = nav,
                            startDestination = Routes.Home,
                            modifier = Modifier.padding(inner)
                        ) {
                            composable(Routes.Home) {
                                HomeScreen(
                                    settings = settingsState,
                                    totalExpenses = 0.0,
                                    totalPurchases = totalCompras / 100.0,
                                    totalProfit = totalGanancia / 100.0,
                                    onAddSale = { nav.navigate(Routes.AddSale) },
                                    onAddPayment = { nav.navigate(Routes.AddPayment) },
                                    onAddClient = { nav.navigate(Routes.Customers) },
                                    onAddExpense = { nav.navigate(Routes.Expenses) }
                                )
                            }
                            composable(Routes.Customers) { CustomersScreen(customersVm) }
                            composable(Routes.Categories) { CategoriesScreen(categoriesVm) }
                            composable(Routes.Products) { ProductsScreen(productsVm) }

                            composable(Routes.Settings) {
                                SettingsScreen(
                                    state = settingsState,
                                    onSetStoreName = settingsVm::setStoreName,
                                    onSetOwnerName = settingsVm::setOwnerName,
                                    onSetLogoUri = settingsVm::setLogoUri,
                                    onSetPinEnabled = settingsVm::setPinEnabled,
                                    onSetBiometricEnabled = settingsVm::setBiometricEnabled
                                )
                            }
                            composable(Routes.AddSale)    { CenterText("Agregar venta — En construcción") }
                            composable(Routes.AddPayment) { CenterText("Agregar abono — En construcción") }
                            composable(Routes.Expenses)   { CenterText("Gastos — En construcción") }
                        }
                    }
                }
            }
        }
    }
}

private object Routes {
    const val Home = "home"
    const val Customers = "customers"
    const val Products = "products"
    const val Categories = "categories"
    const val Settings = "settings"
    const val AddSale = "addSale"
    const val AddPayment = "addPayment"
    const val Expenses = "expenses"


}

@Composable
private fun CenterText(txt: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { Text(txt) }
}

// Helper para navegación sin duplicar destinos
private fun androidx.navigation.NavHostController.safeNavigate(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
