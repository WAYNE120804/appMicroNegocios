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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sebas.tiendaropa.R
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
        Text(
            stringResource(R.string.home_greeting, settings.ownerName),
            style = MaterialTheme.typography.titleMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddSale, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_add_sale))
            }
            Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_action_add_payment))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddClient, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_action_add_client))
            }
            Button(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_action_add_expense))
            }
        }

        SummaryCard(stringResource(R.string.home_summary_expenses), totalExpenses)
        SummaryCard(stringResource(R.string.home_summary_purchases), totalPurchases)
        SummaryCard(stringResource(R.string.home_summary_profit), totalProfit)
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
