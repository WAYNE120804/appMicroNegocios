package com.sebas.tiendaropa.ui.expenses

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.dao.ExpenseWithCategory
import com.sebas.tiendaropa.data.entity.ExpenseCategoryEntity
import com.sebas.tiendaropa.data.entity.ExpenseEntity
import com.sebas.tiendaropa.ui.common.currencyFormatter
import com.sebas.tiendaropa.ui.common.formatPesosFromCents
import com.sebas.tiendaropa.ui.common.formatPesosInput
import com.sebas.tiendaropa.ui.common.integerFormatter
import com.sebas.tiendaropa.ui.common.parsePesosToCents
import com.sebas.tiendaropa.ui.sales.currentLocalDateStartMillis
import com.sebas.tiendaropa.ui.sales.formatSaleDate
import com.sebas.tiendaropa.ui.sales.localStartOfDayMillisToUtcMillis
import com.sebas.tiendaropa.ui.sales.saleDateFormatter
import com.sebas.tiendaropa.ui.sales.utcMillisToLocalStartOfDayMillis
import java.io.File
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    vm: ExpensesViewModel,
    onManageCategories: () -> Unit
) {
    val expenses by vm.visibleExpenses.collectAsState()
    val categories by vm.categories.collectAsState()
    val totalAmount by vm.totalAmountCents.collectAsState()
    val query by vm.query.collectAsState()
    val currencyFormatter = remember { currencyFormatter() }
    val locale = rememberLocale()
    val dateFormatter = remember(locale) { saleDateFormatter(locale) }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.expenses_title)) },
                    actions = {
                        TextButton(onClick = onManageCategories) {
                            Icon(Icons.Default.Category, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.expenses_manage_categories))
                        }
                    }
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.expenses_search_label)) },
                    placeholder = { Text(stringResource(R.string.expenses_search_placeholder)) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Text(stringResource(R.string.customers_add_symbol))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SummaryCard(
                    totalAmount = currencyFormatter.format(totalAmount / 100.0)
                )
            }
            items(expenses, key = { it.expense.id }) { item ->
                ExpenseCard(
                    expenseWithCategory = item,
                    onEdit = {
                        editing = item
                        showDialog = true
                    },
                    onDelete = { vm.deleteExpense(item.expense) },
                    dateFormatter = dateFormatter,
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }

    if (showDialog) {
        ExpenseFormDialog(
            initial = editing?.expense,
            categories = categories,
            onDismiss = { showDialog = false },
            onConfirm = { concept, amountCents, dateMillis, categoryId, paymentMethod, description, photoUri ->
                val id = editing?.expense?.id
                if (id == null) {
                    vm.createExpense(
                        concept = concept,
                        amountCents = amountCents,
                        dateMillis = dateMillis,
                        categoryId = categoryId,
                        paymentMethod = paymentMethod,
                        description = description,
                        photoUri = photoUri
                    )
                } else {
                    vm.updateExpense(
                        id = id,
                        concept = concept,
                        amountCents = amountCents,
                        dateMillis = dateMillis,
                        categoryId = categoryId,
                        paymentMethod = paymentMethod,
                        description = description,
                        photoUri = photoUri
                    )
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun SummaryCard(totalAmount: String) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.expenses_summary_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = totalAmount,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpenseCard(
    expenseWithCategory: ExpenseWithCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dateFormatter: java.text.DateFormat,
    currencyFormatter: java.text.NumberFormat
) {
    val expense = expenseWithCategory.expense
    val categoryName = expenseWithCategory.category?.name
    val formattedDate = remember(expense.dateMillis, dateFormatter) {
        formatSaleDate(expense.dateMillis, dateFormatter)
    }
    val formattedAmount = remember(expense.amountCents, currencyFormatter) {
        currencyFormatter.format(expense.amountCents / 100.0)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(expense.concept, style = MaterialTheme.typography.titleMedium)
                Text(formattedAmount, style = MaterialTheme.typography.titleMedium)
            }
            Text(formattedDate, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = categoryName ?: stringResource(R.string.expenses_no_category),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.expenses_payment_method_label, expense.paymentMethod),
                style = MaterialTheme.typography.bodyMedium
            )
            expense.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            expense.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            Divider()
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(
    initial: ExpenseEntity?,
    categories: List<ExpenseCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        concept: String,
        amountCents: Long?,
        dateMillis: Long,
        categoryId: Long?,
        paymentMethod: String,
        description: String?,
        photoUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val pesosFormatter = remember { integerFormatter() }
    val locale = rememberLocale()
    val dateFormatter = remember(locale) { saleDateFormatter(locale) }

    var concept by rememberSaveable { mutableStateOf(initial?.concept ?: "") }
    var amountText by rememberSaveable {
        mutableStateOf(initial?.let { formatPesosFromCents(it.amountCents, pesosFormatter) } ?: "")
    }
    var paymentMethod by rememberSaveable { mutableStateOf(initial?.paymentMethod ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var photoUri by rememberSaveable { mutableStateOf(initial?.photoUri) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var dateMillis by rememberSaveable {
        mutableStateOf(initial?.dateMillis ?: currentLocalDateStartMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            photoUri = uri.toString()
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            photoUri = uri.toString()
        } else if (uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchExpenseCameraCapture(
                context = context,
                onUriCreated = { uri ->
                    pendingCameraUri = uri
                    captureLauncher.launch(uri)
                }
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.expenses_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val amountCents = remember(amountText) { parsePesosToCents(amountText) }
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
        ?: stringResource(R.string.expenses_no_category)
    val canSave = concept.isNotBlank() && amountCents != null && amountCents > 0 && paymentMethod.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            FilledTonalButton(onClick = {
                if (canSave) {
                    onConfirm(
                        concept,
                        amountCents,
                        dateMillis,
                        selectedCategoryId,
                        paymentMethod,
                        description.takeIf { it.isNotBlank() },
                        photoUri
                    )
                }
            }, enabled = canSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(
                if (initial == null) stringResource(R.string.expenses_dialog_new_title)
                else stringResource(R.string.expenses_dialog_edit_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = concept,
                    onValueChange = { concept = it },
                    label = { Text(stringResource(R.string.expenses_field_concept)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = formatPesosInput(it, pesosFormatter) },
                    label = { Text(stringResource(R.string.expenses_field_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(
                        R.string.expenses_field_date_value,
                        formatSaleDate(dateMillis, dateFormatter)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.expenses_change_date))
                }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.field_category)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.expenses_no_category)) },
                            onClick = {
                                selectedCategoryId = null
                                categoryExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text(stringResource(R.string.expenses_field_payment_method)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.field_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.expenses_add_photo))
                    }
                    OutlinedButton(onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                launchExpenseCameraCapture(
                                    context = context,
                                    onUriCreated = { uri ->
                                        pendingCameraUri = uri
                                        captureLauncher.launch(uri)
                                    }
                                )
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.expenses_add_photo_camera))
                    }
                    photoUri?.let {
                        TextButton(onClick = { photoUri = null }) {
                            Text(stringResource(R.string.expenses_remove_photo))
                        }
                    }
                }
                photoUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }
    )

    if (showDatePicker) {
        val utcInitial = remember(dateMillis) { localStartOfDayMillisToUtcMillis(dateMillis) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcInitial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        dateMillis = utcMillisToLocalStartOfDayMillis(selected)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun rememberLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }
}

private fun launchExpenseCameraCapture(
    context: Context,
    onUriCreated: (Uri) -> Unit
) {
    val uri = createTempExpenseImageUri(context)
    if (uri == null) {
        Toast.makeText(context, context.getString(R.string.expenses_camera_error), Toast.LENGTH_SHORT).show()
    } else {
        onUriCreated(uri)
    }
}

private fun createTempExpenseImageUri(context: Context): Uri? = try {
    val imagesDir = File(context.cacheDir, "expense_images").apply { mkdirs() }
    val file = File.createTempFile("expense_${System.currentTimeMillis()}_", ".jpg", imagesDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (_: IOException) {
    null
}
