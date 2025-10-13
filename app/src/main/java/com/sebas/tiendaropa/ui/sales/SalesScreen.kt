package com.sebas.tiendaropa.ui.sales

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.dao.SaleWithDetails
import com.sebas.tiendaropa.data.entity.CategoryEntity
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.ProductEntity
import com.sebas.tiendaropa.data.prefs.SettingsRepository
import com.sebas.tiendaropa.data.prefs.SettingsState
import com.sebas.tiendaropa.ui.common.currencyFormatter
import com.sebas.tiendaropa.ui.common.formatPesosInput
import com.sebas.tiendaropa.ui.common.integerFormatter
import com.sebas.tiendaropa.ui.common.parsePesosToCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    vm: SalesViewModel,
    onAddSale: () -> Unit
) {
    val allSales by vm.sales.collectAsState()
    val sales by vm.filteredSales.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val currency = remember { currencyFormatter() }
    var paymentTarget by remember { mutableStateOf<SaleWithDetails?>(null) }
    var editTarget by remember { mutableStateOf<SaleWithDetails?>(null) }
    val isSavingPayment by vm.isSavingPayment.collectAsState()
    val isUpdatingSale by vm.isUpdatingSale.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val settings by settingsRepo.state.collectAsState(initial = SettingsState())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = {
            TopAppBar(title = { Text(salesString("sales_title", "Sales")) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSale) {
                Icon(Icons.Default.Add, contentDescription = salesString("action_add_sale", "Add sale"))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = vm::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(salesString("sales_search_customer_label", "Search customer")) },
                placeholder = { Text(salesString("sales_search_customer_placeholder", "Customer name")) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            when {
                allSales.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(salesString("sales_empty", "No sales registered yet."))
                    }
                }

                sales.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(salesString("sales_no_results", "No sales match your filters."))
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        items(
                            items = sales,
                            key = { it.sale.id }
                        ) { sale ->
                            SaleCard(
                                details = sale,
                                currency = currency,
                                onAddPayment = { paymentTarget = it },
                                onShare = { details ->
                                    scope.launch {
                                        shareSaleReceipt(context, details, settings, currency)
                                    }
                                },
                                onEdit = { editTarget = it }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    paymentTarget?.let { target ->
        AddPaymentDialog(
            sale = target,
            currency = currency,
            isSaving = isSavingPayment,
            onDismiss = { paymentTarget = null },
            onConfirm = { amount, description ->
                vm.addPayment(target.sale.id, amount, description) {
                    paymentTarget = null
                }
            }
        )
    }

    editTarget?.let { target ->
        EditSaleDialog(
            sale = target,
            isSaving = isUpdatingSale,
            onDismiss = { if (!isUpdatingSale) editTarget = null },
            onConfirm = { millis, description ->
                vm.updateSaleDetails(target.sale.id, millis, description) {
                    editTarget = null
                }
            }
        )
    }
}



@Composable
private fun SaleCard(
    details: SaleWithDetails,
    currency: NumberFormat,
    onAddPayment: (SaleWithDetails) -> Unit,
    onShare: (SaleWithDetails) -> Unit,
    onEdit: (SaleWithDetails) -> Unit
) {
    val total = currency.format(details.sale.totalCents / 100.0)
    val paid = currency.format(details.totalPaidCents / 100.0)
    val due = currency.format(details.amountDueCents / 100.0)
    val productSummary = details.items.joinToString(separator = ", ") { item ->
        val qty = item.item.quantity
        if (qty > 1) "${item.product.name} x$qty" else item.product.name
    }
    val canAddPayment = details.amountDueCents > 0
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { saleDateFormatter(locale) }
    val saleDateText = remember(details.sale.createdAtMillis, locale) {
        formatSaleDate(details.sale.createdAtMillis, dateFormatter)
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = details.customer.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = salesString("sales_date_label", "Sale date") + ": " + saleDateText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = { onShare(details) }) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(salesString("sales_products_label", "Products") + ": " + productSummary)
                Text(salesString("sales_total_label", "Sale total") + ": " + total)
                Text(salesString("sales_paid_label", "Payments") + ": " + paid)
                Text(salesString("sales_due_label", "Outstanding") + ": " + due)
                details.sale.description?.takeIf { it.isNotBlank() }?.let {
                    Text(salesString("field_description", "Description") + ": " + it)
                }
                if (!canAddPayment) {
                    Text(
                        text = salesString("sales_paid_in_full", "Paid in full"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                PaymentHistory(payments = details.payments, currency = currency)
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onEdit(details) }) {
                    Text(salesString("action_edit", "Edit"))
                }
                TextButton(onClick = { onAddPayment(details) }, enabled = canAddPayment) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(salesString("sales_add_payment", "Record payment"))
                }
            }
        }
    }
}

@Composable
private fun PaymentHistory(payments: List<PaymentEntity>, currency: NumberFormat) {
    if (payments.isEmpty()) return

    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = salesString("sales_payments_header", "Payment history"),
            style = MaterialTheme.typography.bodyMedium
        )
        payments.sortedByDescending { it.createdAtMillis }.forEach { payment ->
            val dateText = dateFormatter.format(Date(payment.createdAtMillis))
            val amount = currency.format(payment.amountCents / 100.0)
            val line = buildString {
                append("• $amount — $dateText")
                payment.description?.takeIf { it.isNotBlank() }?.let {
                    append(" — ")
                    append(it)
                }
            }
            Text(line)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSaleDialog(
    sale: SaleWithDetails,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit
) {
    var selectedDateMillis by rememberSaveable { mutableStateOf(sale.sale.createdAtMillis) }
    var description by rememberSaveable { mutableStateOf(sale.sale.description.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { saleDateFormatter(locale) }
    val formattedDate = remember(selectedDateMillis, locale) {
        formatSaleDate(selectedDateMillis, dateFormatter)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = { Text(salesString("sales_edit_dialog_title", "Edit sale")) },
        text = {
            Column {
                Text(salesString("sales_date_label", "Sale date") + ": " + formattedDate)
                TextButton(onClick = { showDatePicker = true }, enabled = !isSaving) {
                    Text(salesString("sales_edit_dialog_change_date", "Change date"))
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(salesString("field_description", "Description")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (!isSaving) {
                        onConfirm(selectedDateMillis, description.trim().takeIf { it.isNotEmpty() })
                    }
                },
                enabled = !isSaving
            ) {
                Text(salesString("action_save", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(salesString("action_cancel", "Cancel"))
            }
        }
    )

    if (showDatePicker) {
        val utcInitialDate = remember(selectedDateMillis) {
            localStartOfDayMillisToUtcMillis(selectedDateMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcInitialDate)
        LaunchedEffect(utcInitialDate) {
            datePickerState.selectedDateMillis = utcInitialDate
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = utcMillisToLocalStartOfDayMillis(it)
                    }
                    showDatePicker = false
                }) {
                    Text(salesString("action_accept", "Accept"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(salesString("action_cancel", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AddPaymentDialog(
    sale: SaleWithDetails,
    currency: NumberFormat,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val formatter = remember { integerFormatter() }
    val formattedDue = remember(sale.amountDueCents) { currency.format(sale.amountDueCents / 100.0) }
    val cents = remember(amount) { parsePesosToCents(amount) }
    val canSave = cents != null && cents > 0 && !isSaving

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = { Text(salesString("sales_payment_dialog_title", "Record payment")) },
        text = {
            Column {
                Text(stringResource(R.string.sales_payment_dialog_outstanding, formattedDue))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatPesosInput(it, formatter) },
                    label = { Text(salesString("sales_payment_dialog_amount_label", "Payment amount")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(salesString("field_description", "Description")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Text(
                    text = salesString("sales_payment_dialog_hint", "Enter the amount paid by the customer."),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { if (canSave) onConfirm(amount, description.takeIf { it.isNotBlank() }) },
                enabled = canSave
            ) {
                Text(salesString("sales_payment_dialog_confirm", "Save payment"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(salesString("action_cancel", "Cancel"))
            }
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun shareSaleReceipt(
    context: Context,
    details: SaleWithDetails,
    settings: SettingsState,
    currency: NumberFormat
) {
    val logoBitmap = loadStoreLogoBitmap(context, settings.logoUri)
    val bitmap = withContext(Dispatchers.Main) {
        val activity = context.findActivity()
            ?: error("No Activity found from context for sharing")

        // contenedor raíz del Activity
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        // ComposeView temporal adjuntado al árbol de vistas
        val composeView = ComposeView(activity).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            setContent {
                MaterialTheme {
                    PaymentReceiptShareLayout(
                        details = details,
                        settings = settings,
                        currency = currency,
                        logoBitmap = logoBitmap
                    )
                }
            }
        }

        root.addView(composeView)
        try {
            val width = if (root.width > 0) root.width
            else context.resources.displayMetrics.widthPixels

            composeView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
            composeView.drawToBitmap(Bitmap.Config.ARGB_8888)
        } finally {
            root.removeView(composeView)
        }
    }


    val uri = withContext(Dispatchers.IO) {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val safeName = details.customer.name.replace(Regex("""\s+"""), "_")
        val file = File(imagesDir, "historial_${safeName}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    }

    withContext(Dispatchers.Main) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Con ClipData el permiso se respeta mejor en varias apps
            clipData = ClipData.newUri(context.contentResolver, "image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.sales_share_title)
            )
            // Usa NEW_TASK solo si tu context NO es una Activity
            // chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooser)
        } catch (t: Throwable) {
            Log.e("Share", "Error al compartir", t)
            Toast.makeText(context, R.string.sales_share_error, Toast.LENGTH_LONG).show()
        }
    }

}



private suspend fun loadStoreLogoBitmap(context: Context, logoUri: String?): Bitmap? {
    val uri = logoUri?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable ?: return@withContext null
            drawable.toBitmap()
        }.getOrNull()
    }
}



@Composable
private fun PaymentReceiptShareLayout(
    details: SaleWithDetails,
    settings: SettingsState,
    currency: NumberFormat,
    logoBitmap: Bitmap?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Helpers / formateadores ---
            val locale = Locale.getDefault()
            val dateFmt = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
            val timeFmt = remember(locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale) }

            val saleDate = remember(details.sale.createdAtMillis, locale) {
                dateFmt.format(Date(details.sale.createdAtMillis))
            }

            val totalVenta = details.sale.totalCents
            val totalPagos = details.payments.sumOf { it.amountCents }
            val deudaActual = (totalVenta - totalPagos).coerceAtLeast(0)

            val pagosOrdenAsc = remember(details.payments) {
                details.payments.sortedBy { it.createdAtMillis }
            }
            val deudasPorPago = remember(details.payments) {
                val res = mutableListOf<Int>()
                var restante = totalVenta
                pagosOrdenAsc.forEach { p ->
                    restante -= p.amountCents
                    res.add(restante.coerceAtLeast(0).toInt())
                }
                res
            }

            // --- Encabezado (logo + nombre tienda/propietario) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = settings.storeName.ifBlank { "Tienda" },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    settings.ownerName.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                logoBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // --- Datos del cliente ---
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.sales_share_customer_info),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(details.customer.name, fontWeight = FontWeight.SemiBold)
                    details.customer.cedula?.takeIf { it.isNotBlank() }?.let { Text("C.C.: $it") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(details.customer.address?.takeIf { it.isNotBlank() } ?: "")
                        Text(details.customer.phone?.takeIf { it.isNotBlank() } ?: "")
                    }
                    Text(salesString("sales_date_label", "Sale date") + ": " + saleDate)
                }
            }

            // --- Tabla de productos ---
            Text(
                salesString("add_sale_selected_products", "Products in this sale"),
                style = MaterialTheme.typography.titleSmall
            )

            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TableRow(
                        listOf(
                            "Producto" to 0.38f,
                            "Valor" to 0.18f,
                            "Fecha" to 0.20f,
                            "Cant." to 0.10f,
                            "Total" to 0.14f,
                        ),
                        header = true
                    )
                    Divider()
                    details.items.forEach { item ->
                        val unit = currency.format(item.product.valorVentaCents / 100.0)
                        val total = currency.format(
                            (item.product.valorVentaCents * item.item.quantity) / 100.0
                        )
                        TableRow(
                            listOf(
                                item.product.name to 0.38f,
                                unit to 0.18f,
                                saleDate to 0.20f, // si luego guardas fecha por ítem, reemplázala aquí
                                "${item.item.quantity}" to 0.10f,
                                total to 0.14f
                            )
                        )
                    }
                }
            }

            // --- Totales ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.add_sale_total) + ": " +
                                currency.format(totalVenta / 100.0)
                    )
                    Text(
                        stringResource(R.string.sales_share_total_paid) + ": " +
                                currency.format(totalPagos / 100.0)
                    )
                    Text(
                        stringResource(R.string.sales_share_current_debt) + ": " +
                                currency.format(deudaActual / 100.0),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- Tabla de abonos ---
            Text(
                stringResource(R.string.sales_share_payments_title),
                style = MaterialTheme.typography.titleSmall
            )

            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TableRow(
                        listOf(
                            "Valor" to 0.22f,
                            "Fecha" to 0.28f,
                            salesString("field_description", "Description") to 0.30f,
                            "Deuda" to 0.20f
                        ),
                        header = true
                    )
                    Divider()

                    if (pagosOrdenAsc.isEmpty()) {
                        Text(stringResource(R.string.sales_share_no_payments))
                    } else {
                        pagosOrdenAsc.forEachIndexed { idx, p ->
                            val fecha = Date(p.createdAtMillis)
                            val fechaTxt = "${dateFmt.format(fecha)} ${timeFmt.format(fecha)}"
                            val valorTxt = currency.format(p.amountCents / 100.0)
                            val deudaTxt = currency.format(deudasPorPago[idx] / 100.0)
                            TableRow(
                                listOf(
                                    valorTxt to 0.22f,
                                    fechaTxt to 0.28f,
                                    (p.description ?: "") to 0.30f,
                                    deudaTxt to 0.20f
                                )
                            )
                        }
                    }
                }
            }

            // --- Pie ---
            Text("* Documento no válido como factura", style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(
                    R.string.sales_share_generated_on,
                    dateFmt.format(Date(System.currentTimeMillis()))
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


    @Composable
    private fun TableRow(
        cells: List<Pair<String, Float>>,
        header: Boolean = false
    ) {
        Row(Modifier.fillMaxWidth()) {
            cells.forEach { (text, weight) ->
                Text(
                    text = text,
                    modifier = Modifier
                        .weight(weight)
                        .padding(horizontal = 4.dp),
                    style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddSaleScreen(
        vm: SalesViewModel,
        onFinished: () -> Unit,
        navController: NavController
    ) {
        val step by vm.step.collectAsState()
        val products by vm.availableProducts.collectAsState()
        val customers by vm.customers.collectAsState()
        val draft by vm.draft.collectAsState()
        val canContinue by vm.draftCanContinueToCustomer.collectAsState()
        val canFinish by vm.draftCanFinish.collectAsState()
        val totalCents by vm.draftTotalCents.collectAsState()
        val categories by vm.categories.collectAsState()
        val isSaving by vm.isSaving.collectAsState()
        val currency = remember { currencyFormatter() }

        var productQuery by rememberSaveable { mutableStateOf("") }
        var customerQuery by rememberSaveable { mutableStateOf("") }
        var showProductDialog by remember { mutableStateOf(false) }
        var showCustomerDialog by remember { mutableStateOf(false) }
        var showSaleDatePicker by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(salesString("add_sale_title", "New sale")) })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                when (step) {
                    AddSaleStep.PRODUCTS -> {
                        Text(salesString("add_sale_step_products", "Select products"))
                        Spacer(Modifier.height(8.dp))
                        SelectedProductsList(
                            items = draft.items,
                            currency = currency,
                            onIncrease = { vm.addProduct(it.product) },
                            onDecrease = { vm.updateQuantity(it.product.id, it.quantity - 1) },
                            onRemove = { vm.removeProduct(it.product.id) }
                        )
                        OutlinedTextField(
                            value = productQuery,
                            onValueChange = { productQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            label = { Text(salesString("products_search_label", "Product")) },
                            placeholder = {
                                Text(
                                    salesString(
                                        "add_sale_product_search_placeholder",
                                        "Search products"
                                    )
                                )
                            }
                        )

                        TextButton(
                            onClick = {
                                navController.navigate("products?create=true")
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(salesString("add_sale_create_product", "Create product"))
                        }

                        val filtered = products.filter { product ->
                            if (productQuery.isBlank()) true
                            else product.name.contains(productQuery, ignoreCase = true) ||
                                    (product.description?.contains(
                                        productQuery,
                                        ignoreCase = true
                                    ) == true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .padding(top = 16.dp)
                        ) {
                            items(filtered, key = { it.id }) { product ->
                                ProductPickerRow(product = product, currency = currency) {
                                    vm.addProduct(product)
                                }
                                Divider()
                            }
                        }

                        if (!canContinue) {
                            Text(
                                text = salesString(
                                    "add_sale_no_products_hint",
                                    "Add at least one product to continue."
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { vm.back() },
                                enabled = draft.items.isNotEmpty()
                            ) {
                                Text(salesString("action_back", "Back"))
                            }
                            FilledTonalButton(
                                onClick = { vm.goToCustomerStep() },
                                enabled = canContinue
                            ) {
                                Text(salesString("action_next", "Next"))
                            }
                        }
                    }

                    AddSaleStep.CUSTOMER -> {
                        Text(salesString("add_sale_step_customer", "Select customer"))
                        Spacer(Modifier.height(8.dp))
                        draft.customer?.let {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        salesString(
                                            "add_sale_selected_customer",
                                            "Selected customer"
                                        )
                                    )
                                    Text(it.name)
                                    it.phone?.let { phone -> Text(phone) }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = customerQuery,
                            onValueChange = { customerQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            label = { Text(salesString("search_label", "Name / Phone")) },
                            placeholder = {
                                Text(
                                    salesString(
                                        "add_sale_customer_search_placeholder",
                                        "Search customers"
                                    )
                                )
                            }
                        )

                        TextButton(
                            onClick = { showCustomerDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(salesString("add_sale_create_customer", "Create customer"))
                        }

                        val filteredCustomers = customers.filter { customer ->
                            if (customerQuery.isBlank()) true
                            else customer.name.contains(customerQuery, ignoreCase = true) ||
                                    (customer.phone?.contains(
                                        customerQuery,
                                        ignoreCase = true
                                    ) == true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .padding(top = 16.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { customer ->
                                ListItem(
                                    headlineContent = { Text(customer.name) },
                                    supportingContent = {
                                        Text(
                                            listOfNotNull(
                                                customer.phone,
                                                customer.address
                                            ).joinToString(" • ")
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    trailingContent = {
                                        TextButton(onClick = { vm.selectCustomer(customer) }) {
                                            Text(salesString("action_select", "Select"))
                                        }
                                    }
                                )
                                Divider()
                            }
                        }

                        if (!canFinish) {
                            Text(
                                salesString(
                                    "add_sale_no_customers_hint",
                                    "Select or create a customer to continue."
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { vm.back() }) {
                                Text(salesString("action_back", "Back"))
                            }
                            FilledTonalButton(onClick = { vm.goToReview() }, enabled = canFinish) {
                                Text(salesString("action_next", "Next"))
                            }
                        }
                    }


                    AddSaleStep.REVIEW -> {

                        Text(salesString("add_sale_step_review", "Review and confirm"))
                        Text(
                            salesString(
                                "add_sale_review_hint",
                                "Review the order details before finishing."
                            ), modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            val locale = Locale.getDefault()
                            val dateFormatter = remember(locale) {
                                DateFormat.getDateInstance(
                                    DateFormat.MEDIUM,
                                    locale
                                )
                            }
                            val formattedDate = remember(draft.saleDateMillis, locale) {
                                dateFormatter.format(
                                    Date(draft.saleDateMillis)
                                )
                            }
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(salesString("add_sale_selected_customer", "Selected customer"))
                                Text(draft.customer?.name.orEmpty())
                                draft.customer?.phone?.let { Text(it) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val scroll = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                        ) {
                            Text(salesString("add_sale_step_review", "Review and confirm"))
                            Text(
                                salesString(
                                    "add_sale_review_hint",
                                    "Review the order details before finishing."
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val locale = Locale.getDefault()
                                    val dateFormatter = remember(locale) {
                                        DateFormat.getDateInstance(
                                            DateFormat.MEDIUM,
                                            locale
                                        )
                                    }
                                    val formattedDate = remember(draft.saleDateMillis, locale) {
                                        dateFormatter.format(Date(draft.saleDateMillis))
                                    }
                                    Text(
                                        salesString(
                                            "sales_date_label",
                                            "Sale date"
                                        ) + ": " + formattedDate
                                    )

                                    TextButton(onClick = { showSaleDatePicker = true }) {
                                        Text(
                                            salesString(
                                                "sales_edit_dialog_change_date",
                                                "Change date"
                                            )
                                        )
                                    }

                                    OutlinedTextField(
                                        value = draft.description,
                                        onValueChange = vm::updateDraftDescription,
                                        label = {
                                            Text(
                                                salesString(
                                                    "field_description",
                                                    "Description"
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            SelectedProductsList(
                                items = draft.items,
                                currency = currency,
                                onIncrease = { vm.addProduct(it.product) },
                                onDecrease = { vm.updateQuantity(it.product.id, it.quantity - 1) },
                                onRemove = { vm.removeProduct(it.product.id) },
                                readOnly = true
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                salesString("add_sale_total", "Total") + ": " + currency.format(
                                    totalCents / 100.0
                                )
                            )
                            Text(
                                salesString(
                                    "add_sale_outstanding",
                                    "Outstanding"
                                ) + ": " + currency.format(totalCents / 100.0)
                            )
                            Spacer(Modifier.height(12.dp)) // antes era weight(1f)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = { vm.back() }, enabled = !isSaving) {
                                    Text(salesString("action_back", "Back"))
                                }
                                Button(
                                    onClick = { vm.confirmSale { onFinished() } },
                                    enabled = canFinish && !isSaving
                                ) {
                                    Text(salesString("add_sale_save", "Save sale"))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SelectedProductsList(
                            items = draft.items,
                            currency = currency,
                            onIncrease = { vm.addProduct(it.product) },
                            onDecrease = { vm.updateQuantity(it.product.id, it.quantity - 1) },
                            onRemove = { vm.removeProduct(it.product.id) },
                            readOnly = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            salesString("add_sale_total", "Total") + ": " + currency.format(
                                totalCents / 100.0
                            )
                        )
                        Text(
                            salesString(
                                "add_sale_outstanding",
                                "Outstanding"
                            ) + ": " + currency.format(totalCents / 100.0)
                        )
                        Spacer(Modifier.weight(1f, fill = true))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { vm.back() }, enabled = !isSaving) {
                                Text(salesString("action_back", "Back"))
                            }
                            Button(
                                onClick = {
                                    vm.confirmSale {
                                        onFinished()
                                    }
                                },
                                enabled = canFinish && !isSaving
                            ) {
                                Text(salesString("add_sale_save", "Save sale"))
                            }
                        }
                        if (showSaleDatePicker) {
                            val utcInitialDate = remember(draft.saleDateMillis) {
                                localStartOfDayMillisToUtcMillis(draft.saleDateMillis)
                            }
                            val datePickerState =
                                rememberDatePickerState(initialSelectedDateMillis = utcInitialDate)
                            LaunchedEffect(utcInitialDate) {
                                datePickerState.selectedDateMillis = utcInitialDate
                            }
                            DatePickerDialog(
                                onDismissRequest = { showSaleDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            vm.updateDraftDate(
                                                it
                                            )
                                        }
                                        showSaleDatePicker = false
                                    }) {
                                        Text(salesString("action_accept", "Accept"))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSaleDatePicker = false }) {
                                        Text(salesString("action_cancel", "Cancel"))
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
                    }
                }
            }
        }

        if (showProductDialog) {
            CreateProductDialog(
                categories = categories,
                onDismiss = { showProductDialog = false },
                onSave = { name, description, avisos, categoryId, compra, venta ->
                    vm.createProduct(name, description, avisos, categoryId, compra, venta)
                    showProductDialog = false
                }
            )
        }

        if (showCustomerDialog) {
            CreateCustomerDialog(
                onDismiss = { showCustomerDialog = false },
                onSave = { name, address, phone, cedula, notes ->
                    vm.createCustomer(name, address, phone, cedula, notes)
                    showCustomerDialog = false
                }
            )
        }
    }

    @Composable
    private fun ProductPickerRow(
        product: ProductEntity,
        currency: NumberFormat,
        onAdd: () -> Unit
    ) {
        val price = currency.format(product.valorVentaCents / 100.0)
        ListItem(
            headlineContent = { Text(product.name) },
            supportingContent = {
                Column {
                    Text(price)
                    product.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                }
            },
            trailingContent = {
                TextButton(onClick = onAdd) { Text(salesString("action_add", "Add")) }
            }
        )
    }

    @Composable
    private fun SelectedProductsList(
        items: List<SaleDraftItem>,
        currency: NumberFormat,
        onIncrease: (SaleDraftItem) -> Unit,
        onDecrease: (SaleDraftItem) -> Unit,
        onRemove: (SaleDraftItem) -> Unit,
        readOnly: Boolean = false
    ) {
        if (items.isEmpty()) return

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(salesString("add_sale_selected_products", "Products in this sale"))
            items.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.product.name)
                            val subtotal =
                                currency.format(item.product.valorVentaCents * item.quantity / 100.0)
                            Text(subtotal)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!readOnly) {
                                IconButton(onClick = { onDecrease(item) }) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = salesString(
                                            "field_quantity",
                                            "Quantity"
                                        )
                                    )
                                }
                            }
                            Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                            if (!readOnly) {
                                IconButton(onClick = { onIncrease(item) }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = salesString(
                                            "field_quantity",
                                            "Quantity"
                                        )
                                    )
                                }
                                IconButton(onClick = { onRemove(item) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = salesString("delete", "Delete")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CreateProductDialog(
        categories: List<CategoryEntity>,
        onDismiss: () -> Unit,
        onSave: (String, String?, String?, Long, String, String) -> Unit
    ) {
        var name by rememberSaveable { mutableStateOf("") }
        var description by rememberSaveable { mutableStateOf("") }
        var avisos by rememberSaveable { mutableStateOf("") }
        var compra by rememberSaveable { mutableStateOf("") }
        var venta by rememberSaveable { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
        val formatter = remember { integerFormatter() }

        LaunchedEffect(categories) {
            if (selectedCategoryId == null) {
                selectedCategoryId = categories.firstOrNull()?.id
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(salesString("add_sale_create_product", "Create product")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(salesString("field_name", "Name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(salesString("field_description", "Description")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = avisos,
                        onValueChange = { avisos = it },
                        label = { Text(salesString("field_avisos", "Notes")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    CategoryDropdown(
                        categories = categories,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { selectedCategoryId = it }
                    )
                    OutlinedTextField(
                        value = compra,
                        onValueChange = { compra = formatPesosInput(it, formatter) },
                        label = { Text(salesString("field_purchase_price", "Purchase price")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = venta,
                        onValueChange = { venta = formatPesosInput(it, formatter) },
                        label = { Text(salesString("field_sale_price", "Sale price")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val categoryId = selectedCategoryId ?: return@FilledTonalButton
                        if (name.isNotBlank() && parsePesosToCents(compra) != null && parsePesosToCents(
                                venta
                            ) != null
                        ) {
                            onSave(
                                name,
                                description.takeIf { it.isNotBlank() },
                                avisos.takeIf { it.isNotBlank() },
                                categoryId,
                                compra,
                                venta
                            )
                        }
                    },
                    enabled = name.isNotBlank() && selectedCategoryId != null && categories.isNotEmpty()
                ) {
                    Text(salesString("action_save", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(salesString("action_cancel", "Cancel")) }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CategoryDropdown(
        categories: List<CategoryEntity>,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedCategoryId: Long?,
        onCategorySelected: (Long) -> Unit
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            val selected = categories.firstOrNull { it.id == selectedCategoryId }
            OutlinedTextField(
                value = selected?.name ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = categories.isNotEmpty(),
                label = { Text(salesString("field_category", "Category")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategorySelected(category.id)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                salesString(
                    "products_need_category_hint",
                    "You need at least one category to create products."
                ), modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CreateCustomerDialog(
        onDismiss: () -> Unit,
        onSave: (String, String?, String?, String?, String?) -> Unit
    ) {
        var name by rememberSaveable { mutableStateOf("") }
        var address by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var cedula by rememberSaveable { mutableStateOf("") }
        var notes by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(salesString("add_sale_create_customer", "Create customer")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(salesString("field_name", "Name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(salesString("field_address", "Address")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(salesString("field_phone", "Phone")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = { cedula = it },
                        label = { Text("Cédula") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(salesString("field_description", "Notes")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            address.takeIf { it.isNotBlank() },
                            phone.takeIf { it.isNotBlank() },
                            cedula.takeIf { it.isNotBlank() },
                            notes.takeIf { it.isNotBlank() }
                        )
                    }
                }, enabled = name.isNotBlank()) {
                    Text(salesString("action_save", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(salesString("action_cancel", "Cancel")) }
            }
        )
    }

    @Composable
    private fun salesString(
        name: String,
        fallback: String,
        vararg formatArgs: Any
    ): String {
        val context = LocalContext.current
        val resId = remember(name) {
            context.resources.getIdentifier(name, "string", context.packageName)
        }
        return if (resId != 0) {
            if (formatArgs.isNotEmpty()) stringResource(resId, *formatArgs)
            else stringResource(resId)
        } else {
            if (formatArgs.isNotEmpty())
                String.format(Locale.getDefault(), fallback, *formatArgs)
            else
                fallback
        }
    }










