package com.sebas.tiendaropa.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCategoriesScreen(vm: ExpenseCategoriesViewModel) {
    val categories by vm.visibleCategories.collectAsState()
    val query by vm.query.collectAsState()
    val editing by vm.editing.collectAsState()
    val showDialog = remember { mutableStateOf(false) }

    fun openNew() {
        vm.clearEdit()
        showDialog.value = true
    }

    fun openEdit(category: ExpenseCategoryEntity) {
        vm.startEdit(category)
        showDialog.value = true
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.expense_categories_title)) })
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.expense_categories_search_label)) },
                    placeholder = { Text(stringResource(R.string.expense_categories_search_placeholder)) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openNew() }) {
                Text(stringResource(R.string.customers_add_symbol))
            }
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(categories, key = { it.id }) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { openEdit(category) }) {
                                Text(stringResource(R.string.edit))
                            }
                            TextButton(onClick = { vm.remove(category) }) {
                                Text(stringResource(R.string.delete))
                            }
                        }
                    },
                    modifier = Modifier
                        .clickable { openEdit(category) }
                        .padding(horizontal = 8.dp)
                )
                Divider()
            }
        }
    }

    if (showDialog.value) {
        val initial = editing
        ExpenseCategoryDialog(
            initial = initial,
            onDismiss = { showDialog.value = false },
            onSave = { name ->
                if (initial == null) vm.saveNew(name) else vm.saveEdit(initial.id, name)
                showDialog.value = false
            }
        )
    }
}

@Composable
private fun ExpenseCategoryDialog(
    initial: ExpenseCategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val nameState = remember { mutableStateOf(initial?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.expense_categories_new_title)
                else stringResource(R.string.expense_categories_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text(stringResource(R.string.field_name)) }
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                if (nameState.value.isNotBlank()) onSave(nameState.value)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
