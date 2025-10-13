package com.sebas.tiendaropa.ui.products

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.ui.common.currencyFormatter
import com.sebas.tiendaropa.ui.common.formatPesosFromCents
import com.sebas.tiendaropa.ui.common.formatPesosInput
import com.sebas.tiendaropa.ui.common.integerFormatter
import com.sebas.tiendaropa.ui.common.parsePesosToCents
import java.io.File
import java.io.IOException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(vm: ProductsViewModel,  startWithCreateDialog: Boolean = false) {
    val products by vm.visibleProducts.collectAsState()
    val categories by vm.categories.collectAsState()
    val query by vm.query.collectAsState()
    val filter by vm.filter.collectAsState()
    val editing by vm.editing.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<ProductEntity?>(null) }
    LaunchedEffect(startWithCreateDialog) {
        if (startWithCreateDialog) {
            vm.clearEdit()
            showDialog = true
        }
    }

    fun openNew() { vm.clearEdit(); showDialog = true }
    fun openEdit(p: ProductEntity) { vm.startEdit(p); showDialog = true }

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    placeholder = { Text(stringResource(R.string.products_search_placeholder)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == ProductFilter.ALL,
                    onClick = { vm.setFilter(ProductFilter.ALL) },
                    label = { Text(stringResource(R.string.products_filter_all)) }
                )
                FilterChip(
                    selected = filter == ProductFilter.AVAILABLE,
                    onClick = { vm.setFilter(ProductFilter.AVAILABLE) },
                    label = { Text(stringResource(R.string.products_filter_available)) }
                )
                FilterChip(
                    selected = filter == ProductFilter.SOLD,
                    onClick = { vm.setFilter(ProductFilter.SOLD) },
                    label = { Text(stringResource(R.string.products_filter_sold)) }
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    val categoryName = categories.firstOrNull { it.id == product.categoryId }?.name
                        ?: stringResource(R.string.products_unknown_category)
                    ProductCard(
                        product = product,
                        categoryName = categoryName,
                        onView = { viewing = product },
                        onEdit = { openEdit(product) },
                        onDelete = { vm.remove(product) }
                    )
                }
                item { Spacer(modifier = Modifier.size(8.dp)) }
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
            onSave = { name, description, avisos, categoryId, valorCompra, valorVenta, images, soldSaleId ->
                if (initial == null) vm.saveNew(name, description, avisos, categoryId, valorCompra, valorVenta, images)
                else vm.saveEdit(initial.id, name, description, avisos, categoryId, valorCompra, valorVenta, images, soldSaleId)
                vm.clearEdit()
                showDialog = false
            }
        )
    }

    viewing?.let { selected ->
        ProductDetailDialog(
            product = selected,
            categoryName = categories.firstOrNull { it.id == selected.categoryId }?.name
                ?: stringResource(R.string.products_unknown_category),
            onDismiss = { viewing = null }
        )
    }
}

@Composable
private fun ProductSupportingInfo(product: ProductEntity, categoryName: String) {
    val formatter = remember { currencyFormatter() }
    val compra = formatter.format(product.valorCompraCents / 100.0)
    val venta = formatter.format(product.valorVentaCents / 100.0)
    val ganancia = formatter.format(product.gananciaCents() / 100.0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = {
                Text(
                    if (product.soldSaleId != null)
                        stringResource(R.string.products_status_sold)
                    else
                        stringResource(R.string.products_status_available)
                )
            }
        )
        Text(
            text = stringResource(R.string.field_category) + ": " + categoryName,
            style = MaterialTheme.typography.bodyMedium
        )
        product.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        product.avisos?.takeIf { it.isNotBlank() }?.let {
            Text(
                stringResource(R.string.field_avisos) + ": " + it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            stringResource(R.string.field_purchase_price) + ": " + compra,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.field_sale_price) + ": " + venta,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.field_profit) + ": " + ganancia,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    categoryName: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { currencyFormatter() }
    val salePrice = remember(product.valorVentaCents) {
        formatter.format(product.valorVentaCents / 100.0)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = salePrice,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            product.imageUris.firstOrNull()?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            ProductSupportingInfo(product = product, categoryName = categoryName)
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.view))
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit))
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    categories: List<CategoryEntity>,
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Long, String, String, List<String>, Long?) -> Unit,
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
    val context = LocalContext.current

    val imageUris = remember { mutableStateListOf<String>() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }

    // Cargar estado inicial
    LaunchedEffect(initial?.id) {
        imageUris.clear()
        imageUris.addAll(initial?.imageUris ?: emptyList())
        name = initial?.name ?: ""
        description = initial?.description ?: ""
        avisos = initial?.avisos ?: ""
        valorCompra = initial?.let { formatPesosFromCents(it.valorCompraCents, pesosInputFormatter) } ?: ""
        valorVenta = initial?.let { formatPesosFromCents(it.valorVentaCents, pesosInputFormatter) } ?: ""
        selectedCategoryId = initial?.categoryId ?: categories.firstOrNull()?.id
    }
    LaunchedEffect(categories) {
        if (selectedCategoryId == null) selectedCategoryId = categories.firstOrNull()?.id
    }

    // Launchers
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            if (!imageUris.contains(it.toString())) {
                if (imageUris.size >= 3) {
                    Toast.makeText(context, context.getString(R.string.products_max_photos), Toast.LENGTH_SHORT).show()
                } else {
                    imageUris.add(it.toString())
                }
            }
        }
    }
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            if (imageUris.size >= 3) {
                context.contentResolver.delete(uri, null, null)
                Toast.makeText(context, context.getString(R.string.products_max_photos), Toast.LENGTH_SHORT).show()
            } else {
                imageUris.add(uri.toString())
            }
        } else if (uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        pendingCameraUri = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture(
                context = context,
                imageUris = imageUris,
                onUriCreated = { uri ->
                    pendingCameraUri = uri
                    captureLauncher.launch(uri)
                }
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.products_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val canSave = name.isNotBlank() && selectedCategoryId != null && categories.isNotEmpty()
    val profitPreview = remember(valorCompra, valorVenta) {
        val compra = parsePesosToCents(valorCompra)
        val venta = parsePesosToCents(valorVenta)
        if (compra != null && venta != null) venta - compra else null
    }
    val formScrollState = rememberScrollState()
    val formatter = remember { currencyFormatter() }

    // ---------- Dialog principal ----------
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) stringResource(R.string.new_product_title)
            else stringResource(R.string.edit_product_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(formScrollState)
                    .padding(bottom = 4.dp)
            ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (imageUris.size >= 3) {
                                Toast.makeText(context, context.getString(R.string.products_max_photos), Toast.LENGTH_SHORT).show()
                            } else {
                                pickImageLauncher.launch(arrayOf("image/*"))
                            }
                        },
                        enabled = imageUris.size < 3
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.products_add_photo_gallery))
                    }
                    OutlinedButton(
                        onClick = {
                            if (imageUris.size >= 3) {
                                Toast.makeText(context, context.getString(R.string.products_max_photos), Toast.LENGTH_SHORT).show()
                            } else {
                                when (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)) {
                                    PackageManager.PERMISSION_GRANTED -> {
                                        launchCameraCapture(
                                            context = context,
                                            imageUris = imageUris,
                                            onUriCreated = { uri ->
                                                pendingCameraUri = uri
                                                captureLauncher.launch(uri)
                                            }
                                        )
                                    }
                                    else -> cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.products_add_photo_camera))
                    }
                }

                if (imageUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(imageUris, key = { it }) { uri ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clickable { expandedImageUri = uri }
                                )
                                IconButton(onClick = { imageUris.remove(uri) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }

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
                            valorVenta,
                            imageUris.toList(),
                            initial?.soldSaleId
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

    // ---------- Viewer de imagen ampliada (fuera del AlertDialog) ----------
    expandedImageUri?.let { uri ->
        Dialog(onDismissRequest = { expandedImageUri = null }) {
            Card(shape = MaterialTheme.shapes.large) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


private fun launchCameraCapture(
    context: Context,
    imageUris: List<String>,
    onUriCreated: (Uri) -> Unit
) {
    if (imageUris.size >= 3) {
        Toast.makeText(context, context.getString(R.string.products_max_photos), Toast.LENGTH_SHORT).show()
        return
    }
    val uri = createTempImageUri(context)
    if (uri == null) {
        Toast.makeText(context, context.getString(R.string.products_camera_error), Toast.LENGTH_SHORT).show()
    } else {
        onUriCreated(uri)
    }
}

private fun createTempImageUri(context: Context): Uri? = try {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File.createTempFile("product_${System.currentTimeMillis()}_", ".jpg", imagesDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (_: IOException) {
    null
}

@Composable
private fun ProductDetailDialog(
    product: ProductEntity,
    categoryName: String,
    onDismiss: () -> Unit
) {
    val formatter = remember { currencyFormatter() }
    val total = formatter.format(product.valorVentaCents / 100.0)
    val purchase = formatter.format(product.valorCompraCents / 100.0)
    val profit = formatter.format(product.gananciaCents() / 100.0)
    var expandedImageUri by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.products_detail_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            if (product.soldSaleId != null)
                                stringResource(R.string.products_status_sold)
                            else
                                stringResource(R.string.products_status_available)
                        )
                    }
                )
                Text(stringResource(R.string.field_category) + ": " + categoryName)
                product.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                product.avisos?.takeIf { it.isNotBlank() }?.let {
                    Text(stringResource(R.string.field_avisos) + ": " + it)
                }
                Text(stringResource(R.string.field_purchase_price) + ": " + purchase)
                Text(stringResource(R.string.field_sale_price) + ": " + total)
                Text(stringResource(R.string.field_profit) + ": " + profit)

                if (product.imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(product.imageUris, key = { it }) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { expandedImageUri = uri }
                            )
                        }
                    }
                } else {
                    Text(stringResource(R.string.products_no_photos))
                }
            }
            expandedImageUri?.let { uri ->
                Dialog(onDismissRequest = { expandedImageUri = null }) {
                    Card(shape = MaterialTheme.shapes.large) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 480.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}