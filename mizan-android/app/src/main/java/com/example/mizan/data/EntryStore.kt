package com.example.mizan.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class EntryStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("mizan_local_data", Context.MODE_PRIVATE)

    fun loadEntries(): List<LedgerEntry> {
        val saved = preferences.getString(KEY_ENTRIES, "[]") ?: "[]"
        val array = runCatching { JSONArray(saved) }.getOrElse { JSONArray() }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = runCatching {
                    EntryType.valueOf(item.optString("type"))
                }.getOrNull() ?: continue
                val amount = item.optString("amount")
                if (amount.toBigDecimalOrNull() == null) continue

                add(
                    LedgerEntry(
                        id = item.optString("id"),
                        amount = amount,
                        type = type,
                        category = item.optString("category", "أخرى"),
                        note = item.optString("note"),
                        createdAt = item.optLong("createdAt"),
                    ),
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun saveEntries(entries: List<LedgerEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("amount", entry.amount)
                    .put("type", entry.type.name)
                    .put("category", entry.category)
                    .put("note", entry.note)
                    .put("createdAt", entry.createdAt),
            )
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    fun loadCurrency(): String = preferences.getString(KEY_CURRENCY, "YER") ?: "YER"

    fun saveCurrency(code: String) {
        preferences.edit().putString(KEY_CURRENCY, code).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_CURRENCY = "currency"
    }
}
