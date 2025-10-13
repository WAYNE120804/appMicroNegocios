package com.sebas.tiendaropa.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sebas.tiendaropa.R
import com.sebas.tiendaropa.data.dao.ExpenseWithCategory
import com.sebas.tiendaropa.data.dao.SaleWithDetails
import com.sebas.tiendaropa.data.prefs.DashboardPeriodType
import com.sebas.tiendaropa.data.prefs.SettingsState
import com.sebas.tiendaropa.ui.common.currencyFormatter
import com.sebas.tiendaropa.ui.common.integerFormatter
import com.sebas.tiendaropa.ui.common.percentFormatter
import com.sebas.tiendaropa.ui.sales.currentLocalDateStartMillis
import com.sebas.tiendaropa.ui.sales.formatSaleDate
import com.sebas.tiendaropa.ui.sales.localStartOfDayMillisToUtcMillis
import com.sebas.tiendaropa.ui.sales.saleDateFormatter
import com.sebas.tiendaropa.ui.sales.utcMillisToLocalStartOfDayMillis
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: SettingsState,
    sales: List<SaleWithDetails>,
    expenses: List<ExpenseWithCategory>,
    onAddSale: () -> Unit,
    onAddPayment: () -> Unit,
    onAddClient: () -> Unit,
    onAddExpense: () -> Unit,
    onViewSales: () -> Unit,
    onViewExpenses: () -> Unit,
    onViewDebts: () -> Unit,
    onUpdateDashboardPeriod: (DashboardPeriodType, Long?, Long?) -> Unit,
) {
    val scrollState = rememberScrollState()
    val locale = LocalConfiguration.current.locales[0]
    val currencyFormatter = remember { currencyFormatter() }
    val integerFormatter = remember { integerFormatter() }
    val percentFormatter = remember { percentFormatter() }
    val dateFormatter = remember(locale) { saleDateFormatter(locale) }

    val todayStart = remember { currentLocalDateStartMillis() }
    var selectedPeriod by remember { mutableStateOf(settings.dashboardPeriod) }
    var customStart by remember { mutableStateOf(settings.dashboardCustomStartMillis ?: todayStart) }
    var customEnd by remember { mutableStateOf(settings.dashboardCustomEndMillis ?: customStart) }

    LaunchedEffect(
        settings.dashboardPeriod,
        settings.dashboardCustomStartMillis,
        settings.dashboardCustomEndMillis,
    ) {
        selectedPeriod = settings.dashboardPeriod
        val storedStart = settings.dashboardCustomStartMillis ?: todayStart
        val storedEnd = settings.dashboardCustomEndMillis ?: storedStart
        customStart = minOf(storedStart, storedEnd)
        customEnd = max(storedStart, storedEnd)
    }

    val range = remember(selectedPeriod, customStart, customEnd) {
        resolveDashboardRange(selectedPeriod, customStart, customEnd)
    }
    val previousRange = remember(range) { range.previous() }
    val stats = remember(sales, expenses, range, previousRange) {
        computeDashboardStats(sales, expenses, range, previousRange)
    }

    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_greeting, settings.ownerName),
            style = MaterialTheme.typography.titleLarge,
        )

        QuickActionsRow(
            onAddSale = onAddSale,
            onAddPayment = onAddPayment,
            onAddClient = onAddClient,
            onAddExpense = onAddExpense,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_period_title),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DashboardPeriodType.entries.forEach { period ->
                    val label = when (period) {
                        DashboardPeriodType.TODAY -> stringResource(R.string.home_period_today)
                        DashboardPeriodType.LAST_7_DAYS -> stringResource(R.string.home_period_last7)
                        DashboardPeriodType.THIS_MONTH -> stringResource(R.string.home_period_month)
                        DashboardPeriodType.CUSTOM -> stringResource(R.string.home_period_custom)
                    }
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            selectedPeriod = period
                            onUpdateDashboardPeriod(period, customStart, customEnd)
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            if (selectedPeriod == DashboardPeriodType.CUSTOM) {
                CustomRangeSelector(
                    startMillis = customStart,
                    endMillis = customEnd,
                    dateFormatter = dateFormatter,
                    onOpenStartPicker = { showStartPicker = true },
                    onOpenEndPicker = { showEndPicker = true },
                )
            }
        }

        DashboardSections(
            stats = stats,
            currencyFormatter = currencyFormatter,
            integerFormatter = integerFormatter,
            percentFormatter = percentFormatter,
            onViewSales = onViewSales,
            onViewExpenses = onViewExpenses,
            onViewDebts = onViewDebts,
        )

        TrendSection(
            points = stats.dailyPoints,
            onViewSales = onViewSales,
        )
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localStartOfDayMillisToUtcMillis(customStart),
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { selected ->
                            val start = utcMillisToLocalStartOfDayMillis(selected)
                            customStart = start
                            if (customEnd < customStart) customEnd = customStart
                            selectedPeriod = DashboardPeriodType.CUSTOM
                            onUpdateDashboardPeriod(DashboardPeriodType.CUSTOM, customStart, customEnd)
                        }
                        showStartPicker = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localStartOfDayMillisToUtcMillis(customEnd),
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { selected ->
                            val end = utcMillisToLocalStartOfDayMillis(selected)
                            customEnd = end
                            if (customEnd < customStart) customStart = customEnd
                            selectedPeriod = DashboardPeriodType.CUSTOM
                            onUpdateDashboardPeriod(DashboardPeriodType.CUSTOM, customStart, customEnd)
                        }
                        showEndPicker = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsRow(
    onAddSale: () -> Unit,
    onAddPayment: () -> Unit,
    onAddClient: () -> Unit,
    onAddExpense: () -> Unit,
) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        QuickActionButton(
            text = stringResource(R.string.action_add_sale),
            onClick = onAddSale,
            modifier = Modifier.weight(1f, fill = true)
        )
        QuickActionButton(
            text = stringResource(R.string.home_action_add_payment),
            onClick = onAddPayment,
            modifier = Modifier.weight(1f, fill = true)
        )
        QuickActionButton(
            text = stringResource(R.string.home_action_add_client),
            onClick = onAddClient,
            modifier = Modifier.weight(1f, fill = true)
        )
        QuickActionButton(
            text = stringResource(R.string.home_action_add_expense),
            onClick = onAddExpense,
            modifier = Modifier.weight(1f, fill = true)
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomRangeSelector(
    startMillis: Long,
    endMillis: Long,
    dateFormatter: DateFormat,
    onOpenStartPicker: () -> Unit,
    onOpenEndPicker: () -> Unit,
) {
    val startText = remember(startMillis, dateFormatter) { formatSaleDate(startMillis, dateFormatter) }
    val endText = remember(endMillis, dateFormatter) { formatSaleDate(endMillis, dateFormatter) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onOpenStartPicker) {
                Text(stringResource(R.string.home_period_custom_start))
            }
            TextButton(onClick = onOpenEndPicker) {
                Text(stringResource(R.string.home_period_custom_end))
            }
        }
        Text(
            text = stringResource(R.string.home_period_selected_range, startText, endText),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardSections(
    stats: DashboardStats,
    currencyFormatter: NumberFormat,
    integerFormatter: NumberFormat,
    percentFormatter: NumberFormat,
    onViewSales: () -> Unit,
    onViewExpenses: () -> Unit,
    onViewDebts: () -> Unit,
) {
    val current = stats.current
    val previous = stats.previous

    val salesTrend = remember(current.salesTotal, previous.salesTotal) {
        computeTrend(current.salesTotal, previous.salesTotal)
    }
    val expensesTrend = remember(current.totalExpenses, previous.totalExpenses) {
        computeTrend(current.totalExpenses, previous.totalExpenses)
    }
    val grossTrend = remember(current.grossProfit, previous.grossProfit) {
        computeTrend(current.grossProfit, previous.grossProfit)
    }
    val netTrend = remember(current.netProfit, previous.netProfit) {
        computeTrend(current.netProfit, previous.netProfit)
    }
    val countTrend = remember(current.salesCount, previous.salesCount) {
        computeTrend(current.salesCount.toDouble(), previous.salesCount.toDouble())
    }
    val ticketTrend = remember(current.averageTicket, previous.averageTicket) {
        computeTrend(current.averageTicket, previous.averageTicket)
    }
    val marginTrend = remember(current.marginRatio, previous.marginRatio) {
        computeTrend(current.marginRatio, previous.marginRatio)
    }
    val debtTrend = remember(current.totalDebt, previous.totalDebt) {
        computeTrend(current.totalDebt, previous.totalDebt)
    }

    val emptySalesMessage =
        if (current.salesTotal <= 0.0) stringResource(R.string.home_empty_sales) else null

    val emptyExpensesMessage =
        if (current.totalExpenses <= 0.0) stringResource(R.string.home_empty_expenses) else null

    val emptyDebtMessage =
        if (current.totalDebt <= 0.0) stringResource(R.string.home_empty_debt) else null


    val cardModifier = Modifier.widthIn(min = 220.dp)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        KpiCard(
            title = stringResource(R.string.home_kpi_sales),
            valueText = currencyFormatter.format(current.salesTotal),
            trend = salesTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_expenses),
            valueText = currencyFormatter.format(current.totalExpenses),
            trend = expensesTrend,
            onViewMore = onViewExpenses,
            emptyMessage = emptyExpensesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_gross_profit),
            valueText = currencyFormatter.format(current.grossProfit),
            trend = grossTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_net_profit),
            valueText = currencyFormatter.format(current.netProfit),
            trend = netTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_sales_count),
            valueText = integerFormatter.format(current.salesCount),
            trend = countTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_avg_ticket),
            valueText = currencyFormatter.format(current.averageTicket),
            trend = ticketTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_margin),
            valueText = percentFormatter.format(current.marginRatio),
            trend = marginTrend,
            onViewMore = onViewSales,
            emptyMessage = emptySalesMessage,
            modifier = cardModifier,
        )
        KpiCard(
            title = stringResource(R.string.home_kpi_total_debt),
            valueText = currencyFormatter.format(current.totalDebt),
            trend = debtTrend,
            onViewMore = onViewDebts,
            emptyMessage = emptyDebtMessage,
            modifier = cardModifier,
        )
    }

    SectionTitle(text = stringResource(R.string.home_top_debtors_title))
    TopListCard(
        items = stats.topDebtors.map {
            TopListItem(label = it.name, value = currencyFormatter.format(it.amount))
        },
        emptyMessage = stringResource(R.string.home_top_empty_debtors),
        onViewMore = onViewDebts,
    )

    SectionTitle(text = stringResource(R.string.home_top_expense_categories_title))
    TopListCard(
        items = stats.topExpenseCategories.map {
            val name = it.name ?: stringResource(R.string.expenses_no_category)
            TopListItem(label = name, value = currencyFormatter.format(it.amount))
        },
        emptyMessage = stringResource(R.string.home_top_empty_categories),
        onViewMore = onViewExpenses,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun KpiCard(
    title: String,
    valueText: String,
    trend: TrendResult,
    onViewMore: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(valueText, style = MaterialTheme.typography.headlineSmall)
            emptyMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            TrendRow(trend = trend)
            TextButton(onClick = onViewMore) {
                Text(stringResource(R.string.home_view_more))
            }
        }
    }
}

@Composable
private fun TrendRow(trend: TrendResult) {
    val color = trendColor(trend.direction, MaterialTheme.colorScheme)
    val icon = when (trend.direction) {
        TrendDirection.UP -> Icons.Default.ArrowUpward
        TrendDirection.DOWN -> Icons.Default.ArrowDownward
        TrendDirection.FLAT -> Icons.Default.Remove
    }
    val changeText = formatTrendPercent(trend.percent)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(
            text = stringResource(R.string.home_variation_label, changeText),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TopListCard(
    items: List<TopListItem>,
    emptyMessage: String,
    onViewMore: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (items.isEmpty()) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.label, style = MaterialTheme.typography.bodyMedium)
                        Text(item.value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            TextButton(onClick = onViewMore) {
                Text(stringResource(R.string.home_view_more))
            }
        }
    }
}

@Composable
private fun TrendSection(
    points: List<DailyPoint>,
    onViewSales: () -> Unit,
) {
    SectionTitle(text = stringResource(R.string.home_trend_title))
    TrendCard(
        points = points,
        salesLabel = stringResource(R.string.home_kpi_sales),
        expensesLabel = stringResource(R.string.home_kpi_expenses),
        onViewMore = onViewSales,
    )
}

@Composable
private fun TrendCard(
    points: List<DailyPoint>,
    salesLabel: String,
    expensesLabel: String,
    onViewMore: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                val maxValue = remember(points) {
                    points.maxOfOrNull { max(it.salesTotal, it.expensesTotal) } ?: 0.0
                }
                if (maxValue <= 0.0) {
                    Text(stringResource(R.string.home_trend_empty), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val scheme = MaterialTheme.colorScheme
                    val salesColor = scheme.primary
                    val expensesColor = scheme.tertiary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
                        val scale = if (maxValue == 0.0) 0f else size.height / maxValue.toFloat()
                        val salesPath = Path()
                        val expensesPath = Path()
                        points.forEachIndexed { index, point ->
                            val x = stepX * index
                            val salesY = size.height - (point.salesTotal.toFloat() * scale)
                            val expensesY = size.height - (point.expensesTotal.toFloat() * scale)
                            if (index == 0) {
                                salesPath.moveTo(x, salesY)
                                expensesPath.moveTo(x, expensesY)
                            } else {
                                salesPath.lineTo(x, salesY)
                                expensesPath.lineTo(x, expensesY)
                            }
                        }
                        drawPath(
                            path = expensesPath,
                            color = expensesColor,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawPath(
                            path = salesPath,
                            color = salesColor,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = MaterialTheme.colorScheme.primary)
                Text(salesLabel, style = MaterialTheme.typography.bodySmall)
                LegendDot(color = MaterialTheme.colorScheme.tertiary)
                Text(expensesLabel, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onViewMore) {
                Text(stringResource(R.string.home_view_more))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, shape = MaterialTheme.shapes.small),
    )
}

private fun trendColor(direction: TrendDirection, scheme: ColorScheme): Color = when (direction) {
    TrendDirection.UP -> scheme.primary
    TrendDirection.DOWN -> scheme.error
    TrendDirection.FLAT -> scheme.outline
}

private fun formatTrendPercent(percent: Double): String =
    String.format(Locale.getDefault(), "%+.1f%%", percent)

private data class TopListItem(val label: String, val value: String)

private data class DashboardStats(
    val current: DashboardSnapshot,
    val previous: DashboardSnapshot,
    val topDebtors: List<DebtorSummary>,
    val topExpenseCategories: List<ExpenseCategorySummary>,
    val dailyPoints: List<DailyPoint>,
)

private data class DashboardSnapshot(
    val salesTotal: Double,
    val salesCount: Int,
    val grossProfit: Double,
    val totalExpenses: Double,
    val totalDebt: Double,
    val averageTicket: Double,
    val netProfit: Double,
    val marginRatio: Double,
)

private data class DebtorSummary(
    val id: Long,
    val name: String,
    val amount: Double,
)

private data class ExpenseCategorySummary(
    val id: Long?,
    val name: String?,
    val amount: Double,
)

private data class DailyPoint(
    val dayStartMillis: Long,
    val salesTotal: Double,
    val expensesTotal: Double,
)

private enum class TrendDirection { UP, DOWN, FLAT }

private data class TrendResult(val percent: Double, val direction: TrendDirection)

private fun computeDashboardStats(
    sales: List<SaleWithDetails>,
    expenses: List<ExpenseWithCategory>,
    range: DashboardDateRange,
    previousRange: DashboardDateRange,
): DashboardStats {
    val currentSales = sales.filter { range.contains(it.sale.createdAtMillis) }
    val currentExpenses = expenses.filter { range.contains(it.expense.dateMillis) }
    val previousSales = sales.filter { previousRange.contains(it.sale.createdAtMillis) }
    val previousExpenses = expenses.filter { previousRange.contains(it.expense.dateMillis) }

    val currentSnapshot = createSnapshot(currentSales, currentExpenses)
    val previousSnapshot = createSnapshot(previousSales, previousExpenses)

    val topDebtors = computeTopDebtors(currentSales)
    val topCategories = computeTopExpenseCategories(currentExpenses)
    val dailyPoints = buildDailyPoints(range, currentSales, currentExpenses)

    return DashboardStats(
        current = currentSnapshot,
        previous = previousSnapshot,
        topDebtors = topDebtors,
        topExpenseCategories = topCategories,
        dailyPoints = dailyPoints,
    )
}

private fun createSnapshot(
    sales: List<SaleWithDetails>,
    expenses: List<ExpenseWithCategory>,
): DashboardSnapshot {
    var salesTotalCents = 0L
    var grossProfitCents = 0L
    var totalDebtCents = 0L
    sales.forEach { sale ->
        salesTotalCents += sale.sale.totalCents
        totalDebtCents += sale.amountDueCents
        sale.items.forEach { item ->
            val revenue = item.item.lineTotalCents
            val cost = item.product.valorCompraCents * item.item.quantity
            grossProfitCents += revenue - cost
        }
    }
    val totalExpensesCents = expenses.sumOf { it.expense.amountCents }
    val salesCount = sales.size

    val salesTotal = salesTotalCents / 100.0
    val grossProfit = grossProfitCents / 100.0
    val totalExpenses = totalExpensesCents / 100.0
    val totalDebt = totalDebtCents / 100.0
    val averageTicket = if (salesCount > 0) salesTotal / salesCount else 0.0
    val netProfit = grossProfit - totalExpenses
    val marginRatio = if (salesTotal > 0) grossProfit / salesTotal else 0.0

    return DashboardSnapshot(
        salesTotal = salesTotal,
        salesCount = salesCount,
        grossProfit = grossProfit,
        totalExpenses = totalExpenses,
        totalDebt = totalDebt,
        averageTicket = averageTicket,
        netProfit = netProfit,
        marginRatio = marginRatio,
    )
}

private fun computeTopDebtors(sales: List<SaleWithDetails>): List<DebtorSummary> {
    val accumulator = mutableMapOf<Long, Pair<String, Long>>()
    sales.forEach { sale ->
        val due = sale.amountDueCents
        if (due > 0) {
            val current = accumulator[sale.customer.id]
            val updatedAmount = (current?.second ?: 0L) + due
            accumulator[sale.customer.id] = sale.customer.name to updatedAmount
        }
    }
    return accumulator.entries
        .map { (id, pair) -> DebtorSummary(id, pair.first, pair.second / 100.0) }
        .sortedByDescending { it.amount }
        .take(5)
}

private fun computeTopExpenseCategories(expenses: List<ExpenseWithCategory>): List<ExpenseCategorySummary> {
    data class CategoryAccumulator(var name: String?, var amountCents: Long)

    val accumulator = mutableMapOf<Long?, CategoryAccumulator>()
    expenses.forEach { item ->
        val key = item.category?.id
        val entry = accumulator.getOrPut(key) { CategoryAccumulator(item.category?.name, 0L) }
        if (entry.name == null && item.category?.name != null) {
            entry.name = item.category.name
        }
        entry.amountCents += item.expense.amountCents
    }
    return accumulator.entries
        .map { (id, acc) -> ExpenseCategorySummary(id, acc.name, acc.amountCents / 100.0) }
        .sortedByDescending { it.amount }
        .take(5)
}

private fun buildDailyPoints(
    range: DashboardDateRange,
    sales: List<SaleWithDetails>,
    expenses: List<ExpenseWithCategory>,
): List<DailyPoint> {
    val salesByDay = mutableMapOf<Long, Long>()
    val expensesByDay = mutableMapOf<Long, Long>()

    sales.forEach { sale ->
        val day = startOfDay(sale.sale.createdAtMillis)
        salesByDay[day] = (salesByDay[day] ?: 0L) + sale.sale.totalCents
    }
    expenses.forEach { expense ->
        val day = startOfDay(expense.expense.dateMillis)
        expensesByDay[day] = (expensesByDay[day] ?: 0L) + expense.expense.amountCents
    }

    val result = mutableListOf<DailyPoint>()
    var day = range.start
    while (day < range.endExclusive) {
        val salesTotal = (salesByDay[day] ?: 0L) / 100.0
        val expensesTotal = (expensesByDay[day] ?: 0L) / 100.0
        result.add(DailyPoint(dayStartMillis = day, salesTotal = salesTotal, expensesTotal = expensesTotal))
        day += DAY_MILLIS
    }
    return result
}

private fun computeTrend(current: Double, previous: Double): TrendResult {
    val epsilon = 1e-6
    if (abs(previous) < epsilon) {
        if (abs(current) < epsilon) return TrendResult(0.0, TrendDirection.FLAT)
        return TrendResult(100.0, if (current > 0) TrendDirection.UP else TrendDirection.DOWN)
    }
    val change = ((current - previous) / previous) * 100.0
    val direction = when {
        change > epsilon -> TrendDirection.UP
        change < -epsilon -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }
    return TrendResult(change, direction)
}

private data class DashboardDateRange(val start: Long, val endExclusive: Long) {
    fun contains(millis: Long): Boolean = millis in start until endExclusive
    fun previous(): DashboardDateRange {
        val duration = (endExclusive - start).coerceAtLeast(DAY_MILLIS)
        val newEnd = start
        val newStart = start - duration
        return DashboardDateRange(newStart, newEnd)
    }
}

private fun resolveDashboardRange(
    period: DashboardPeriodType,
    customStart: Long,
    customEnd: Long,
): DashboardDateRange {
    return when (period) {
        DashboardPeriodType.TODAY -> {
            val start = currentLocalDateStartMillis()
            DashboardDateRange(start, start + DAY_MILLIS)
        }
        DashboardPeriodType.LAST_7_DAYS -> {
            val end = currentLocalDateStartMillis() + DAY_MILLIS
            DashboardDateRange(end - DAY_MILLIS * 7, end)
        }
        DashboardPeriodType.THIS_MONTH -> {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentLocalDateStartMillis()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis
            DashboardDateRange(start, end)
        }
        DashboardPeriodType.CUSTOM -> {
            val start = minOf(customStart, customEnd)
            val end = max(customStart, customEnd) + DAY_MILLIS
            DashboardDateRange(start, end)
        }
    }
}

private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
