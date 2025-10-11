package com.sebas.tiendaropa.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.entity.CustomerEntity



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(vm: CustomersViewModel) {
    val customers by vm.visibleCustomers.collectAsState()
    val query by vm.query.collectAsState()
    val editing by vm.editing.collectAsState()
    val showDialog = remember { mutableStateOf(false) }

    fun openNew() { vm.clearEdit(); showDialog.value = true }
    fun openEdit(c: CustomerEntity) { vm.startEdit(c); showDialog.value = true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.customers_title)) })
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Nombre / Número") }, // o stringResource(...)
                    placeholder = { Text("Buscar por nombre o celular") } // o stringResource(...)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openNew() }) { Text("+") }
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(customers) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = {
                        Column {
                            Text(listOfNotNull(c.phone, c.address, c.cedula).joinToString(" • "))
                            c.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(desc)
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { openEdit(c) }) { Text("Editar") }
                            TextButton(onClick = { vm.remove(c) }) { Text("Eliminar") }
                        }
                    },
                    modifier = Modifier
                        .clickable { openEdit(c) } // tocar el item también edita
                        .padding(horizontal = 8.dp)
                )
                Divider()
            }
        }
    }

    if (showDialog.value) {
        val initial = editing
        CustomerDialog(
            initial = initial,
            onDismiss = { showDialog.value = false },
            onSave = { name, addr, phone, cedula, description ->
                if (initial == null) vm.saveNew(name, addr, phone, cedula, description)
                else vm.saveEdit(initial.id, name, addr, phone, cedula, description)
                showDialog.value = false
            }
        )
    }
}

@Composable
private fun CustomerDialog(
    initial: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?, String?) -> Unit
) {
    val name = remember { mutableStateOf(initial?.name ?: "") }
    val address = remember { mutableStateOf(initial?.address ?: "") }
    val phone = remember { mutableStateOf(initial?.phone ?: "") }
    val cedula = remember { mutableStateOf(initial?.cedula ?: "") }
    val description = remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null)
                    stringResource(R.string.new_customer_title)
                else
                    "Editar cliente"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name.value,
                    onValueChange = { name.value = it },
                    label = { Text(stringResource(R.string.field_name)) }
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = address.value,
                    onValueChange = { address.value = it },
                    label = { Text(stringResource(R.string.field_address)) }
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = phone.value,
                    onValueChange = { phone.value = it },
                    label = { Text(stringResource(R.string.field_phone)) }
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = cedula.value,
                    onValueChange = { cedula.value = it },
                    label = { Text("Cédula") }
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = description.value,
                    onValueChange = { description.value = it },
                    label = { Text(stringResource(R.string.field_description)) }
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                onSave(
                    name.value,
                    address.value.ifBlank { null },
                    phone.value.ifBlank { null },
                    cedula.value.ifBlank { null },
                    description.value.ifBlank { null }
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
