package com.sebas.tiendaropa.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class AppTypeConverters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list.isNullOrEmpty()) return null
        val array = JSONArray()
        list.forEach { uri ->
            array.put(uri)
        }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(serialized: String?): List<String> {
        if (serialized.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(serialized)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (!value.isNullOrBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
