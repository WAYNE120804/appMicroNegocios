package com.sebas.tiendaropa.ui.sales

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun currentLocalDateStartMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun utcMillisToLocalStartOfDayMillis(utcMillis: Long): Long {
    val utcDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return utcDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

internal fun localStartOfDayMillisToUtcMillis(localMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

internal fun saleDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

internal fun formatSaleDate(millis: Long, formatter: DateTimeFormatter): String {
    val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return formatter.format(localDate)
}
