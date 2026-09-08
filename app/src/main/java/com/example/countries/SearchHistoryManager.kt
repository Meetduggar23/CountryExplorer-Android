package com.example.countries

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_ENTRIES = 15
        private const val KEY_QUERIES = "queries"
    }

    fun addQuery(query: String) {
        if (query.isBlank()) return
        val queries = getQueryList()
        if (queries.isNotEmpty() && queries.first() == query) return
        queries.add(0, query)
        if (queries.size > MAX_ENTRIES) {
            queries.subList(MAX_ENTRIES, queries.size).clear()
        }
        saveQueries(queries)
    }

    fun getQueries(): List<String> {
        return getQueryList()
    }

    fun clear() {
        prefs.edit().remove(KEY_QUERIES).apply()
    }

    private fun getQueryList(): MutableList<String> {
        val json = prefs.getString(KEY_QUERIES, null) ?: return mutableListOf()
        val array = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun saveQueries(queries: List<String>) {
        val array = JSONArray()
        queries.forEach { array.put(it) }
        prefs.edit().putString(KEY_QUERIES, array.toString()).apply()
    }
}
