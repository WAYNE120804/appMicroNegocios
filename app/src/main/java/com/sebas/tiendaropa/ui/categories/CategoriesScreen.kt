package com.sebas.tiendaropa.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sebas.tiendaropa.data.entity.CategoryEntity
import androidx.compose.foundation.lazy.items


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: CategoriesViewModel) {
    val categories by vm.visibleCategories.collectAsState()
    val query by vm.query.collectAsState()
    val editing by vm.editing.collectAsState()
    val showDialog = remember { mutableStateOf(false) }

    fun openNew() { vm.clearEdit(); showDialog.value = true }
    fun openEdit(c: CategoryEntity) { vm.startEdit(c); showDialog.value = true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.categories_title)) })
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Categoría") },
                    placeholder = { Text("Buscar categoría...") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openNew() }) { Text("+") }
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(categories) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { openEdit(c) }) { Text("Editar") }
                            TextButton(onClick = { vm.remove(c) }) { Text("Eliminar") }
                        }
                    },
                    modifier = Modifier
                        .clickable { openEdit(c) }
                        .padding(horizontal = 8.dp)
                )
                Divider()
            }
        }
    }

    if (showDialog.value) {
        val initial = editing
        CategoryDialog(
            initial = initial,
            onDismiss = { showDialog.value = false },
            onSave = { name ->
                if (initial == null) vm.saveNew(name)
                else vm.saveEdit(initial.id, name)
                showDialog.value = false
            }
        )
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val name = remember { mutableStateOf(initial?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null)
                    stringResource(R.string.new_category_title)
                else
                    "Editar categoría"
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
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                if (name.value.isNotBlank()) onSave(name.value)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
