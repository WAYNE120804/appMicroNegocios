package com.sebas.tiendaropa.util

import android.content.Context
import com.sebas.tiendaropa.data.db.AppDatabase
import com.sebas.tiendaropa.data.entity.PaymentEntity
import com.sebas.tiendaropa.data.entity.SaleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

class DataExportManager(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)

    suspend fun createBackupZip(): File = withContext(Dispatchers.IO) {
        val exportDir = File(appContext.cacheDir, EXPORT_DIR).apply { mkdirs() }
        exportDir.listFiles()?.forEach { it.deleteRecursively() }

        val customers = database.customerDao().getAllSnapshot()
        val categories = database.categoryDao().getAllSnapshot()
        val products = database.productDao().getAllSnapshot()
        val payments = database.saleDao().getAllPayments()
        val salesById = loadSalesById(payments)
        val expenses = database.expenseDao().getAllSnapshot()

        val customersById = customers.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }

        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timestampFormatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val timestamp = timestampFormatter.format(Date())
        val zipFile = File(exportDir, "respaldo_$timestamp.zip")

        val customerRows = customers.map { customer ->
            listOf(
                customer.id.toString(),
                customer.name,
                customer.address.orEmpty(),
                customer.phone.orEmpty(),
                customer.cedula.orEmpty(),
                customer.description.orEmpty()
            )
        }

        val productRows = products.map { product ->
            val categoryName = categoriesById[product.categoryId]?.name ?: ""
            val status = if (product.soldSaleId != null) "Vendido" else "Disponible"
            listOf(
                product.id.toString(),
                product.name,
                categoryName,
                formatMoney(product.valorCompraCents),
                formatMoney(product.valorVentaCents),
                formatMoney(product.gananciaCents()),
                status,
                product.soldSaleId?.toString() ?: "",
                product.description.orEmpty(),
                product.avisos.orEmpty()
            )
        }

        val paymentRows = payments.map { payment ->
            val sale = salesById[payment.saleId]
            val customerName = sale?.let { customersById[it.customerId]?.name } ?: ""
            listOf(
                payment.id.toString(),
                dateTimeFormatter.format(Date(payment.createdAtMillis)),
                formatMoney(payment.amountCents),
                payment.saleId.toString(),
                customerName,
                sale?.let { dateFormatter.format(Date(it.createdAtMillis)) } ?: "",
                sale?.totalCents?.let { formatMoney(it) } ?: "",
                sale?.description.orEmpty(),
                payment.description.orEmpty()
            )
        }

        val expenseRows = expenses.map { expenseWithCategory ->
            val expense = expenseWithCategory.expense
            val categoryName = expenseWithCategory.category?.name ?: "Sin categoría"
            listOf(
                expense.id.toString(),
                dateFormatter.format(Date(expense.dateMillis)),
                formatMoney(expense.amountCents),
                categoryName,
                expense.paymentMethod,
                expense.concept,
                expense.description.orEmpty()
            )
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            zip.writeCsv(
                "clientes.csv",
                listOf("ID", "Nombre", "Dirección", "Teléfono", "Cédula", "Descripción"),
                customerRows
            )
            zip.writeCsv(
                "productos.csv",
                listOf(
                    "ID",
                    "Nombre",
                    "Categoría",
                    "Costo",
                    "Precio venta",
                    "Ganancia",
                    "Estado",
                    "Venta ID",
                    "Descripción",
                    "Notas"
                ),
                productRows
            )
            zip.writeCsv(
                "abonos.csv",
                listOf(
                    "ID",
                    "Fecha abono",
                    "Monto",
                    "Venta ID",
                    "Cliente",
                    "Fecha venta",
                    "Total venta",
                    "Descripción venta",
                    "Descripción abono"
                ),
                paymentRows
            )
            zip.writeCsv(
                "gastos.csv",
                listOf(
                    "ID",
                    "Fecha",
                    "Monto",
                    "Categoría",
                    "Método de pago",
                    "Concepto",
                    "Descripción"
                ),
                expenseRows
            )
        }

        zipFile
    }

    private suspend fun loadSalesById(payments: List<PaymentEntity>): Map<Long, SaleEntity> =
        if (payments.isEmpty()) {
            emptyMap()
        } else {
            val saleIds = payments.map { it.saleId }.distinct()
            database.saleDao().getSalesByIds(saleIds).associateBy { it.id }
        }

    private fun formatMoney(amountCents: Long): String {
        val sign = if (amountCents < 0) "-" else ""
        val absolute = abs(amountCents)
        val units = absolute / 100
        val cents = (absolute % 100).toString().padStart(2, '0')
        return "$sign$units.$cents"
    }

    private fun ZipOutputStream.writeCsv(
        entryName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        putNextEntry(ZipEntry(entryName))
        val builder = StringBuilder()
        builder.append(headers.joinToString(",") { it.toCsvField() })
        builder.append('\n')
        rows.forEach { row ->
            builder.append(row.joinToString(",") { it.toCsvField() })
            builder.append('\n')
        }
        write(builder.toString().toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private fun String.toCsvField(): String =
        if (contains('"') || contains(',') || contains('\n') || contains('\r')) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }

    companion object {
        private const val EXPORT_DIR = "exports"
    }
}
