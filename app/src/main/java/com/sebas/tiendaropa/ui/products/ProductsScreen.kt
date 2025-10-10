package com.sebas.tiendaropa.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(vm: ProductsViewModel) {
    val products by vm.visibleProducts.collectAsState()
    val categories by vm.categories.collectAsState()
    val query by vm.query.collectAsState()
    val editing by vm.editing.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    fun openNew() { vm.clearEdit(); showDialog = true }
    fun openEdit(p: ProductEntity) { vm.startEdit(p); showDialog = true }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.products_title)) })
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        vm.setQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.products_search_label)) },
                    placeholder = { Text(stringResource(R.string.products_search_placeholder)) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                focusManager.clearFocus()
                openNew()
            }) { Text(stringResource(R.string.customers_add_symbol)) }
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(
                items = products,
                key = { it.id }
            ) { product ->
                val categoryName = categories.firstOrNull { it.id == product.categoryId }?.name
                    ?: stringResource(R.string.products_unknown_category)
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = {
                        ProductSupportingInfo(product = product, categoryName = categoryName)
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { openEdit(product) }) {
                                Text(stringResource(R.string.edit))
                            }
                            TextButton(onClick = { vm.remove(product) }) {
                                Text(stringResource(R.string.delete))
                            }
                        }
                    },
                    modifier = Modifier
                        .clickable { openEdit(product) }
                        .padding(horizontal = 8.dp)
                )
                Divider()
            }
        }
    }

    if (showDialog) {
        val initial = editing
        ProductDialog(
            categories = categories,
            initial = initial,
            onDismiss = {
                vm.clearEdit()
                showDialog = false
            },
            onSave = { name, description, avisos, categoryId, valorCompra, valorVenta ->
                if (initial == null) vm.saveNew(name, description, avisos, categoryId, valorCompra, valorVenta)
                else vm.saveEdit(initial.id, name, description, avisos, categoryId, valorCompra, valorVenta)
                vm.clearEdit()
                showDialog = false
            }
        )
    }
}

@Composable
private fun ProductSupportingInfo(product: ProductEntity, categoryName: String) {
    val formatter = remember { currencyFormatter() }
    val compra = formatter.format(product.valorCompraCents / 100.0)
    val venta = formatter.format(product.valorVentaCents / 100.0)
    val ganancia = formatter.format(product.gananciaCents() / 100.0)

    Column(Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.field_category) + ": " + categoryName)
        product.description?.takeIf { it.isNotBlank() }?.let {
            Text(it)
        }
        product.avisos?.takeIf { it.isNotBlank() }?.let {
            Text(stringResource(R.string.field_avisos) + ": " + it)
        }
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.field_purchase_price) + ": " + compra)
        Text(stringResource(R.string.field_sale_price) + ": " + venta)
        Text(stringResource(R.string.field_profit) + ": " + ganancia)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    categories: List<CategoryEntity>,
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Long, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var avisos by remember { mutableStateOf(initial?.avisos ?: "") }
    val pesosInputFormatter = remember { integerFormatter() }
    var valorCompra by remember {
        mutableStateOf(initial?.let { formatPesosFromCents(it.valorCompraCents, pesosInputFormatter) } ?: "")
    }
    var valorVenta by remember {
        mutableStateOf(initial?.let { formatPesosFromCents(it.valorVentaCents, pesosInputFormatter) } ?: "")
    }
    var selectedCategoryId by remember { mutableStateOf(initial?.categoryId) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(initial?.id) {
        name = initial?.name ?: ""
        description = initial?.description ?: ""
        avisos = initial?.avisos ?: ""
        valorCompra = initial?.let { formatPesosFromCents(it.valorCompraCents, pesosInputFormatter) } ?: ""
        valorVenta = initial?.let { formatPesosFromCents(it.valorVentaCents, pesosInputFormatter) } ?: ""
        selectedCategoryId = initial?.categoryId ?: categories.firstOrNull()?.id
    }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }

    val canSave = name.isNotBlank() && selectedCategoryId != null && categories.isNotEmpty()
    val profitPreview = remember(valorCompra, valorVenta) {
        val compra = parsePesosToCents(valorCompra)
        val venta = parsePesosToCents(valorVenta)
        if (compra != null && venta != null) venta - compra else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null)
                    stringResource(R.string.new_product_title)
                else
                    stringResource(R.string.edit_product_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.field_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = avisos,
                    onValueChange = { avisos = it },
                    label = { Text(stringResource(R.string.field_avisos)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.padding(top = 8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selected = categories.firstOrNull { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = selected?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = categories.isNotEmpty(),
                        label = { Text(stringResource(R.string.field_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        placeholder = {
                            if (categories.isEmpty()) {
                                Text(stringResource(R.string.products_empty_categories))
                            }
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                if (categories.isEmpty()) {
                    Text(
                        stringResource(R.string.products_need_category_hint),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = valorCompra,
                    onValueChange = { valorCompra = formatPesosInput(it, pesosInputFormatter) },
                    label = { Text(stringResource(R.string.field_purchase_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = valorVenta,
                    onValueChange = { valorVenta = formatPesosInput(it, pesosInputFormatter) },
                    label = { Text(stringResource(R.string.field_sale_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val formatter = remember { currencyFormatter() }
                profitPreview?.let {
                    Text(
                        text = stringResource(R.string.field_profit) + ": " + formatter.format(it / 100.0),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (canSave) {
                        onSave(
                            name,
                            description.takeIf { it.isNotBlank() },
                            avisos.takeIf { it.isNotBlank() },
                            selectedCategoryId!!,
                            valorCompra,
                            valorVenta
                        )
                    }
                },
                enabled = canSave
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun currencyFormatter(): NumberFormat =
    NumberFormat.getCurrencyInstance(Locale("es", "CO"))

private fun integerFormatter(): NumberFormat =
    NumberFormat.getIntegerInstance(Locale("es", "CO")).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

private fun formatPesosInput(raw: String, formatter: NumberFormat): String {
    val digitsOnly = raw.filter(Char::isDigit)
    if (digitsOnly.isEmpty()) return ""
    val value = digitsOnly.toLongOrNull() ?: return ""
    return formatter.format(value)
}

private fun formatPesosFromCents(amountCents: Long, formatter: NumberFormat): String =
    formatPesosInput((amountCents / 100L).toString(), formatter)

private fun parsePesosToCents(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    if (digits.isBlank()) return null
    return runCatching { digits.toLong() * 100L }.getOrNull()
}