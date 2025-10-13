package com.sebas.tiendaropa.ui.sales

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilidades de fecha para la sección de ventas (compatibles con minSdk 24).
 *
 * Nota: evitamos java.time (API 26+) y usamos Calendar/Date/DateFormat.
 */

/** Millis del inicio del día local (00:00:00.000) del día actual. */
internal fun currentLocalDateStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Convierte un instante UTC (por ejemplo, devuelto por DatePicker) a
 * el inicio del día en hora local (00:00:00.000) en millis.
 */
internal fun utcMillisToLocalStartOfDayMillis(utcMillis: Long): Long {
    val cal = Calendar.getInstance() // zona horaria local por defecto
    cal.timeInMillis = utcMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Convierte el inicio del día local (millis) a un aproximado en UTC (millis),
 * útil para inicializar DatePicker que espera UTC.
 */
internal fun localStartOfDayMillisToUtcMillis(localStartMillis: Long): Long {
    val tz: TimeZone = TimeZone.getDefault()
    return localStartMillis - tz.getOffset(localStartMillis)
}

/** Devuelve un formateador de fecha estilo MEDIUM para el locale dado. */
internal fun saleDateFormatter(locale: Locale): DateFormat =
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

/** Formatea un instante en millis usando el DateFormat dado. */
internal fun formatSaleDate(millis: Long, formatter: DateFormat): String =
    formatter.format(Date(millis))
