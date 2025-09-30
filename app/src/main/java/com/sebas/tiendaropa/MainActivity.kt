package com.sebas.tiendaropa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.sebas.tiendaropa.ui.customers.CustomersScreen
import com.sebas.tiendaropa.ui.customers.CustomersViewModel



class MainActivity : ComponentActivity() {

    private val vm: CustomersViewModel by viewModels {
        CustomersViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CustomersScreen(vm)
            }
        }
    }
}
