package com.example.data

import android.content.Context
import org.json.JSONObject

object RadioCatalog {
    data class RadioItem(val name: String, val url: String)
    data class RadioCategory(val name: String, val type: String, val items: List<RadioItem>)

    fun load(context: Context): List<RadioCategory> {
        return try {
            val raw = context.assets.open("radio_catalog.json").bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val cats = root.optJSONArray("categories") ?: return emptyList()
            (0 until cats.length()).mapNotNull { i ->
                val obj = cats.optJSONObject(i) ?: return@mapNotNull null
                val arr = obj.optJSONArray("items")
                RadioCategory(
                    name = obj.optString("name"),
                    type = obj.optString("type"),
                    items = if (arr == null) emptyList() else (0 until arr.length()).mapNotNull { j ->
                        val it = arr.optJSONObject(j) ?: return@mapNotNull null
                        RadioItem(name = it.optString("name"), url = it.optString("url"))
                    }
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
