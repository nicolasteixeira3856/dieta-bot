package com.nutri.android.core.database

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

class NutriConverters {
    @TypeConverter
    fun intListToText(value: List<Int>?): String {
        val arr = JSONArray()
        val src = value ?: listOf(2000, 2000, 2000, 2000, 2000, 2000, 2000)
        for (n in src) {
            arr.put(n)
        }
        return arr.toString()
    }

    @TypeConverter
    fun textToIntList(value: String?): List<Int> {
        val fallback = listOf(2000, 2000, 2000, 2000, 2000, 2000, 2000)
        if (value.isNullOrEmpty()) {
            return fallback
        }
        return try {
            val arr = JSONArray(value)
            val out = mutableListOf<Int>()
            for (i in 0 until arr.length()) {
                out.add(arr.getInt(i))
            }
            if (out.size == 7) out else fallback
        } catch (e: JSONException) {
            fallback
        }
    }

    @TypeConverter
    fun stringListToText(value: List<String>?): String {
        val arr = JSONArray()
        if (value != null) {
            for (s in value) {
                arr.put(s)
            }
        }
        return arr.toString()
    }

    @TypeConverter
    fun textToStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val arr = JSONArray(value)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                out.add(arr.getString(i))
            }
            out
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
