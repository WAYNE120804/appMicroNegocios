package com.sebas.tiendaropa

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.sebas.tiendaropa.ui.categories.CategoriesScreen
import com.sebas.tiendaropa.ui.categories.CategoriesViewModel
import com.sebas.tiendaropa.ui.customers.CustomersScreen
import com.sebas.tiendaropa.ui.customers.CustomersViewModel
import com.sebas.tiendaropa.ui.expenses.ExpenseCategoriesScreen
import com.sebas.tiendaropa.ui.expenses.ExpenseCategoriesViewModel
import com.sebas.tiendaropa.ui.expenses.ExpensesScreen
import com.sebas.tiendaropa.ui.expenses.ExpensesViewModel
import com.sebas.tiendaropa.ui.home.HomeScreen
import com.sebas.tiendaropa.ui.products.ProductsScreen
import com.sebas.tiendaropa.ui.products.ProductsViewModel
import com.sebas.tiendaropa.ui.sales.AddSaleScreen
import com.sebas.tiendaropa.ui.sales.SalesScreen
import com.sebas.tiendaropa.ui.sales.SalesViewModel
import com.sebas.tiendaropa.ui.security.AuthenticationScreen
import com.sebas.tiendaropa.ui.settings.SettingsScreen
import com.sebas.tiendaropa.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch


class MainActivity : FragmentActivity() {

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

    private val salesVm: SalesViewModel by viewModels {
        SalesViewModel.factory(applicationContext)
    }

    private val expensesVm: ExpensesViewModel by viewModels {
        ExpensesViewModel.factory(applicationContext)
    }

    private val expenseCategoriesVm: ExpenseCategoriesViewModel by viewModels {
        ExpenseCategoriesViewModel.factory(applicationContext)
    }


    @SuppressLint("ComposableDestinationInComposeScope")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val settingsState = settingsVm.state.collectAsState().value
                var isUnlocked by rememberSaveable { mutableStateOf(false) }
                val needsSecurity = settingsState.pinEnabled || settingsState.biometricEnabled

                LaunchedEffect(needsSecurity) {
                    isUnlocked = !needsSecurity
                }

                if (needsSecurity && !isUnlocked) {
                    AuthenticationScreen(
                        state = settingsState,
                        onUnlock = { isUnlocked = true },
                        onResetPin = {
                            settingsVm.savePin(it)
                            settingsVm.setPinEnabled(true)
                        }
                    )
                } else {
                    val nav = rememberNavController()
                    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    data class DrawerItem(val route: String, val label: String, val icon: ImageVector)

                    val items = listOf(
                        DrawerItem(Routes.Home, "Inicio", Icons.Default.Home),
                        DrawerItem(Routes.Customers, "Clientes", Icons.Default.People),
                        DrawerItem(Routes.Products, "Productos", Icons.Default.ShoppingCart),
                        DrawerItem(Routes.Sales, "Ventas", Icons.Default.ReceiptLong),
                        DrawerItem(Routes.Expenses, "Gastos", Icons.Default.AttachMoney),
                        DrawerItem(Routes.Categories, "Categorías", Icons.Default.Category),
                        DrawerItem(Routes.Settings, "Configuración", Icons.Default.Settings),
                    )

                    val totalCompras = productsVm.totalCompras.collectAsState().value
                    val totalGanancia = productsVm.totalGanancia.collectAsState().value
                    val totalGastos = expensesVm.totalAmountCents.collectAsState().value

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
                                                popUpTo(nav.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
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
                                        totalExpenses = totalGastos / 100.0,
                                        totalPurchases = totalCompras / 100.0,
                                        totalProfit = totalGanancia / 100.0,
                                        onAddSale = { nav.navigate(Routes.AddSale) },
                                        onAddPayment = { nav.navigate(Routes.Sales) },
                                        onAddClient = { nav.navigate(Routes.Customers) },
                                        onAddExpense = { nav.navigate(Routes.Expenses) }
                                    )
                                }
                                composable(Routes.Customers) { CustomersScreen(customersVm) }
                                composable(Routes.Categories) { CategoriesScreen(categoriesVm) }
                                composable(
                                    route = Routes.Products + "?create={create}",
                                    arguments = listOf(
                                        navArgument("create") {
                                            type = NavType.BoolType
                                            defaultValue = false
                                        }
                                    )
                                ) { backStackEntry ->
                                    val startWithCreateDialog =
                                        backStackEntry.arguments?.getBoolean("create") ?: false

                                    ProductsScreen(
                                        vm = productsVm,
                                        startWithCreateDialog = startWithCreateDialog
                                    )
                                }
                                composable(Routes.Sales) {
                                    SalesScreen(
                                        vm = salesVm,
                                        onAddSale = { nav.navigate(Routes.AddSale) }
                                    )

                                }
                                composable(Routes.Expenses) {
                                    ExpensesScreen(
                                        vm = expensesVm,
                                        onManageCategories = { nav.navigate(Routes.ExpenseCategories) }
                                    )
                                }
                                composable(Routes.Settings) {
                                    SettingsScreen(
                                        state = settingsState,
                                        onSetStoreName = settingsVm::setStoreName,
                                        onSetOwnerName = settingsVm::setOwnerName,
                                        onSetLogoUri = settingsVm::setLogoUri,
                                        onSetPinEnabled = settingsVm::setPinEnabled,
                                        onSetBiometricEnabled = settingsVm::setBiometricEnabled,
                                        onSavePin = settingsVm::savePin,
                                        onSetSecurityQuestion = settingsVm::setSecurityQuestion,
                                        onSaveSecurityAnswer = settingsVm::saveSecurityAnswer
                                    )
                                }
                                composable(Routes.AddSale) {
                                    AddSaleScreen(
                                        vm = salesVm,
                                        onFinished = {
                                            nav.navigate(Routes.Sales) {
                                                popUpTo(nav.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        navController = nav
                                    )
                                }
                                composable(Routes.ExpenseCategories) {
                                    ExpenseCategoriesScreen(expenseCategoriesVm)
                                }
                            }
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

        const val ExpenseCategories = "expenseCategories"
        const val Sales = "sales"
    }

    // Helper para navegación sin duplicar destinos
    private fun androidx.navigation.NavHostController.safeNavigate(route: String) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

