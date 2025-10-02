package com.sebas.tiendaropa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebas.tiendaropa.data.prefs.SettingsState

@Composable
fun HomeScreen(
    settings: SettingsState,
    totalExpenses: Double,
    totalPurchases: Double,
    totalProfit: Double,
    onAddSale: () -> Unit,
    onAddPayment: () -> Unit,
    onAddClient: () -> Unit,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hola ${settings.ownerName}, ¿qué quieres hacer hoy?",
            style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddSale, modifier = Modifier.weight(1f)) { Text("Agregar venta") }
            Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) { Text("Agregar abono") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddClient, modifier = Modifier.weight(1f)) { Text("Agregar cliente") }
            Button(onClick = onAddExpense, modifier = Modifier.weight(1f)) { Text("Agregar gasto") }
        }

        SummaryCard("Gastos", totalExpenses)
        SummaryCard("Compra productos", totalPurchases)
        SummaryCard("Ganancias", totalProfit)
    }
}

@Composable
private fun SummaryCard(title: String, value: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("$${"%,.2f".format(value)}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
