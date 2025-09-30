package com.sebas.tiendaropa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sebas.tiendaropa.ui.categories.CategoriesScreen
import com.sebas.tiendaropa.ui.categories.CategoriesViewModel
import com.sebas.tiendaropa.ui.customers.CustomersScreen
import com.sebas.tiendaropa.ui.customers.CustomersViewModel

class MainActivity : ComponentActivity() {

    private val customersVm: CustomersViewModel by viewModels {
        CustomersViewModel.factory(applicationContext)
    }
    private val categoriesVm: CategoriesViewModel by viewModels {
        CategoriesViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val backStackEntry by nav.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == Routes.Home,
                                onClick = {
                                    nav.navigate(Routes.Home) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, "Inicio") },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == Routes.Customers,
                                onClick = {
                                    nav.navigate(Routes.Customers) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.People, "Clientes") },
                                label = { Text("Clientes") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == Routes.Categories,
                                onClick = {
                                    nav.navigate(Routes.Categories) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Category, "Categorías") },
                                label = { Text("Categorías") }
                            )
                        }
                    }
                ) { inner ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.Home,
                        modifier = Modifier.padding(inner)
                    ) {
                        composable(Routes.Home) {
                            HomeScreen(
                                totalExpenses = 0.0,     // TODO: enlazar a Room cuando tengas gastos
                                totalPurchases = 0.0,    // TODO: suma precioCompra de productos comprados
                                totalProfit = 0.0,       // TODO: ventas - compras
                                onAddSale = { nav.navigate(Routes.AddSale) },
                                onAddPayment = { nav.navigate(Routes.AddPayment) },
                                onAddClient = { nav.navigate(Routes.Customers) },
                                onAddExpense = { nav.navigate(Routes.Expenses) }
                            )
                        }
                        composable(Routes.Customers) { CustomersScreen(customersVm) }
                        composable(Routes.Categories) { CategoriesScreen(categoriesVm) }

                        // ===== Stubs (pantallas provisionales) =====
                        composable(Routes.AddSale) { ComingSoonScreen("Agregar venta") }
                        composable(Routes.AddPayment) { ComingSoonScreen("Agregar abono") }
                        composable(Routes.Expenses) { ComingSoonScreen("Gastos") }
                    }
                }
            }
        }
    }
}

private object Routes {
    const val Home = "home"
    const val Customers = "customers"
    const val Categories = "categories"
    const val AddSale = "addSale"
    const val AddPayment = "addPayment"
    const val Expenses = "expenses"
}

@Composable
private fun HomeScreen(
    totalExpenses: Double,
    totalPurchases: Double,
    totalProfit: Double,
    onAddSale: () -> Unit,
    onAddPayment: () -> Unit,
    onAddClient: () -> Unit,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CloudStore", style = MaterialTheme.typography.headlineMedium)
        Text("Hola Alisson, ¿qué quieres hacer hoy?")

        // Acciones rápidas
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddSale, modifier = Modifier.weight(1f)) { Text("Agregar venta") }
            Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) { Text("Agregar abono") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddClient, modifier = Modifier.weight(1f)) { Text("Agregar cliente") }
            Button(onClick = onAddExpense, modifier = Modifier.weight(1f)) { Text("Agregar gasto") }
        }

        // Tarjetas con totales
        SummaryCard(title = "Gastos", value = totalExpenses)
        SummaryCard(title = "Compra productos", value = totalPurchases)
        SummaryCard(title = "Ganancias", value = totalProfit)
    }
}

@Composable
private fun SummaryCard(title: String, value: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$${"%,.2f".format(value)}",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun ComingSoonScreen(nombre: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$nombre — En construcción")
    }
}
