package com.sebas.tiendaropa.ui.common

import java.text.NumberFormat
import java.util.Locale

fun currencyFormatter(): NumberFormat =
    NumberFormat.getCurrencyInstance(Locale("es", "CO"))

fun integerFormatter(): NumberFormat =
    NumberFormat.getIntegerInstance(Locale("es", "CO")).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

fun percentFormatter(): NumberFormat =
    NumberFormat.getPercentInstance(Locale("es", "CO")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

fun formatPesosInput(raw: String, formatter: NumberFormat): String {
    val digitsOnly = raw.filter(Char::isDigit)
    if (digitsOnly.isEmpty()) return ""
    val value = digitsOnly.toLongOrNull() ?: return ""
    return formatter.format(value)
}

fun formatPesosFromCents(amountCents: Long, formatter: NumberFormat): String =
    formatPesosInput((amountCents / 100L).toString(), formatter)

fun parsePesosToCents(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    if (digits.isBlank()) return null
    return runCatching { digits.toLong() * 100L }.getOrNull()
}