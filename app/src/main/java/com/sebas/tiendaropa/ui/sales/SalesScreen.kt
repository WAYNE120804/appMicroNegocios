package com.sebas.tiendaropa.ui.sales

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    vm: SalesViewModel,
    onAddSale: () -> Unit
) {
    val sales by vm.sales.collectAsState()
    val currency = remember { currencyFormatter() }
    var paymentTarget by remember { mutableStateOf<SaleWithDetails?>(null) }
    val isSavingPayment by vm.isSavingPayment.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val settings by settingsRepo.state.collectAsState(initial = SettingsState())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(salesString("sales_title", "Sales")) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSale) {
                Icon(Icons.Default.Add, contentDescription = salesString("action_add_sale", "Add sale"))
            }
        }
    ) { innerPadding ->
        if (sales.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(salesString("sales_empty", "No sales registered yet."))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                items(
                    items = sales,
                    key = { it.sale.id }
                ) { sale ->
                    SaleRow(
                        details = sale,
                        currency = currency,
                        onAddPayment = { paymentTarget = it },
                        onShare = { details ->
                            scope.launch {
                                shareSaleReceipt(context, details, settings, currency)
                            }
                        }
                    )
                    Divider()
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
}

@Composable
private fun SaleRow(
    details: SaleWithDetails,
    currency: NumberFormat,
    onAddPayment: (SaleWithDetails) -> Unit,
    onShare: (SaleWithDetails) -> Unit
) {
    val total = currency.format(details.sale.totalCents / 100.0)
    val paid = currency.format(details.totalPaidCents / 100.0)
    val due = currency.format(details.amountDueCents / 100.0)
    val productSummary = details.items.joinToString(separator = ", ") { item ->
        val qty = item.item.quantity
        if (qty > 1) "${item.product.name} x$qty" else item.product.name
    }
    val canAddPayment = details.amountDueCents > 0

    ListItem(
        headlineContent = { Text(details.customer.name) },
        supportingContent = {
            Column {
                Text(salesString("sales_products_label", "Products") + ": " + productSummary)
                Text(salesString("sales_total_label", "Sale total") + ": " + total)
                Text(salesString("sales_paid_label", "Payments") + ": " + paid)
                Text(salesString("sales_due_label", "Outstanding") + ": " + due)
                if (!canAddPayment) {
                    Text(
                        text = salesString("sales_paid_in_full", "Paid in full"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                PaymentHistory(payments = details.payments, currency = currency)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onShare(details) }) {
                    Text(salesString("sales_share_button", "Share history"))
                }
                TextButton(onClick = { onAddPayment(details) }, enabled = canAddPayment) {
                    Text(salesString("sales_add_payment", "Record payment"))
                }
            }
        }
    )
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
                Text(salesString("sales_payment_dialog_outstanding", "Outstanding balance: %s", formattedDue))
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

private suspend fun shareSaleReceipt(
    context: Context,
    details: SaleWithDetails,
    settings: SettingsState,
    currency: NumberFormat
) {
    val bitmap = withContext(Dispatchers.Main) {
        val composeView = ComposeView(context)
        composeView.setContent {
            MaterialTheme {
                PaymentReceiptShareLayout(details = details, settings = settings, currency = currency)
            }
        }
        val width = context.resources.displayMetrics.widthPixels
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
        composeView.drawToBitmap(Bitmap.Config.ARGB_8888)
    }

    val uri = withContext(Dispatchers.IO) {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val safeName = details.customer.name.replace(Regex("""\s+"""), "_")
        val file = File(imagesDir, "historial_${safeName}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    withContext(Dispatchers.Main) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.sales_share_title)))
    }
}

@Composable
private fun PaymentReceiptShareLayout(
    details: SaleWithDetails,
    settings: SettingsState,
    currency: NumberFormat
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
    val timeFormatter = remember(locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale) }
    val generatedText = remember {
        dateFormatter.format(Date(System.currentTimeMillis()))
    }
    val totalText = currency.format(details.sale.totalCents / 100.0)
    val paidText = currency.format(details.totalPaidCents / 100.0)
    val outstandingText = currency.format(details.amountDueCents / 100.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            settings.logoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
            }
            Text(settings.storeName, style = MaterialTheme.typography.headlineSmall)
            Text(settings.ownerName, style = MaterialTheme.typography.bodyMedium)
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.sales_share_customer_info), style = MaterialTheme.typography.titleSmall)
                Text(details.customer.name)
                details.customer.cedula?.let { Text("C.C.: $it") }
                details.customer.phone?.let { Text(it) }
                details.customer.address?.let { Text(it) }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.sales_share_total_debt) + ": " + totalText)
                Text(stringResource(R.string.sales_share_total_paid) + ": " + paidText)
                Text(stringResource(R.string.sales_share_current_debt) + ": " + outstandingText)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.sales_share_payments_title), style = MaterialTheme.typography.titleSmall)
            if (details.payments.isEmpty()) {
                Text(salesString("sales_payments_header", "Payment history") + ": 0")
            } else {
                details.payments
                    .sortedByDescending { it.createdAtMillis }
                    .forEach { payment ->
                        val date = Date(payment.createdAtMillis)
                        val lineDate = "${dateFormatter.format(date)} ${timeFormatter.format(date)}"
                        val amount = currency.format(payment.amountCents / 100.0)
                        val description = payment.description?.let { " — $it" } ?: ""
                        Text("$amount — $lineDate$description")
                    }
            }
        }

        Text(stringResource(R.string.sales_share_generated_on, generatedText), style = MaterialTheme.typography.bodySmall)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(
    vm: SalesViewModel,
    onFinished: () -> Unit
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
                        placeholder = { Text(salesString("add_sale_product_search_placeholder", "Search products")) }
                    )

                    TextButton(
                        onClick = { showProductDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(salesString("add_sale_create_product", "Create product"))
                    }

                    val filtered = products.filter { product ->
                        if (productQuery.isBlank()) true
                        else product.name.contains(productQuery, ignoreCase = true) ||
                                (product.description?.contains(productQuery, ignoreCase = true) == true)
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
                            text = salesString("add_sale_no_products_hint", "Add at least one product to continue."),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { vm.back() }, enabled = draft.items.isNotEmpty()) {
                            Text(salesString("action_back", "Back"))
                        }
                        FilledTonalButton(onClick = { vm.goToCustomerStep() }, enabled = canContinue) {
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
                                Text(salesString("add_sale_selected_customer", "Selected customer"))
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
                        placeholder = { Text(salesString("add_sale_customer_search_placeholder", "Search customers")) }
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
                                (customer.phone?.contains(customerQuery, ignoreCase = true) == true)
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
                                    Text(listOfNotNull(customer.phone, customer.address).joinToString(" • "))
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
                            salesString("add_sale_no_customers_hint", "Select or create a customer to continue."),
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
                    Text(salesString("add_sale_review_hint", "Review the order details before finishing."), modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(salesString("add_sale_selected_customer", "Selected customer"))
                            Text(draft.customer?.name.orEmpty())
                            draft.customer?.phone?.let { Text(it) }
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
                        salesString("add_sale_total", "Total") + ": " + currency.format(totalCents / 100.0)
                    )
                    Text(
                        salesString("add_sale_outstanding", "Outstanding") + ": " + currency.format(totalCents / 100.0)
                    )
                    Spacer(Modifier.weight(1f, fill = true))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { vm.back() }, enabled = !isSaving) {
                            Text(salesString("action_back", "Back"))
                        }
                        FilledTonalButton(
                            onClick = {
                                vm.confirmSale {
                                    onFinished()
                                }
                            },
                            enabled = canFinish && !isSaving
                        ) {
                            Text(salesString("add_sale_confirm", "Confirm sale"))
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
                        val subtotal = currency.format(item.product.valorVentaCents * item.quantity / 100.0)
                        Text(subtotal)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!readOnly) {
                            IconButton(onClick = { onDecrease(item) }) {
                                Icon(Icons.Default.Remove, contentDescription = salesString("field_quantity", "Quantity"))
                            }
                        }
                        Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                        if (!readOnly) {
                            IconButton(onClick = { onIncrease(item) }) {
                                Icon(Icons.Default.Add, contentDescription = salesString("field_quantity", "Quantity"))
                            }
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = salesString("delete", "Delete"))
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
                    if (name.isNotBlank() && parsePesosToCents(compra) != null && parsePesosToCents(venta) != null) {
                        onSave(name, description.takeIf { it.isNotBlank() }, avisos.takeIf { it.isNotBlank() }, categoryId, compra, venta)
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
        Text(salesString("products_need_category_hint", "You need at least one category to create products."), modifier = Modifier.padding(top = 4.dp))
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
                    label = { Text("Cédula" ) },
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
private fun salesString(name: String, fallback: String, vararg formatArgs: Any?): String {
    val context = LocalContext.current
    val resId = remember(name) { context.resources.getIdentifier(name, "string", context.packageName) }
    return if (resId != 0) {
        if (formatArgs.isNotEmpty()) stringResource(resId, *arrayOf(formatArgs)) else stringResource(resId)
    } else {
        if (formatArgs.isNotEmpty()) String.format(Locale.getDefault(), fallback, *formatArgs) else fallback
    }
}


