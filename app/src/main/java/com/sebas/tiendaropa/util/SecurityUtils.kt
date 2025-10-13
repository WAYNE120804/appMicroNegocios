package com.sebas.tiendaropa.util

import java.security.MessageDigest

object SecurityUtils {

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte ->
            val i = byte.toInt() and 0xff
            i.toString(16).padStart(2, '0')
        }
    }

    fun isFourDigitPin(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }
}
