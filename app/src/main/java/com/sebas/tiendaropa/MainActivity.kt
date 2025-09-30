package com.sebas.tiendaropa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
                                selected = currentRoute == "customers",
                                onClick = {
                                    nav.navigate("customers") {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Filled.People, contentDescription = "Clientes") },
                                label = { Text("Clientes") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "categories",
                                onClick = {
                                    nav.navigate("categories") {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Filled.Category, contentDescription = "Categorías") },
                                label = { Text("Categorías") }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = nav,
                        startDestination = "customers",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("customers") { CustomersScreen(customersVm) }
                        composable("categories") { CategoriesScreen(categoriesVm) }
                    }
                }
            }
        }
    }
}

