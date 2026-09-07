package com.example.countries

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class RecentlyViewedManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("recently_viewed", Context.MODE_PRIVATE)

    private val maxItems = 10

    fun addCountry(cca3: String) {
        val codes = getCodes().toMutableList()
        codes.remove(cca3)
        codes.add(0, cca3)
        if (codes.size > maxItems) {
            saveCodes(codes.take(maxItems))
        } else {
            saveCodes(codes)
        }
    }

    fun getRecentlyViewed(allCountries: List<Country>): List<Country> {
        val codes = getCodes()
        val countryMap = allCountries.associateBy { it.cca3 }
        return codes.mapNotNull { countryMap[it] }
    }

    fun clear() {
        sharedPreferences.edit().remove("viewed_codes").apply()
    }

    private fun getCodes(): List<String> {
        val jsonString = sharedPreferences.getString("viewed_codes", null) ?: return emptyList()
        val jsonArray = JSONArray(jsonString)
        val codes = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            codes.add(jsonArray.getString(i))
        }
        return codes
    }

    private fun saveCodes(codes: List<String>) {
        val jsonArray = JSONArray()
        codes.forEach { jsonArray.put(it) }
        sharedPreferences.edit().putString("viewed_codes", jsonArray.toString()).apply()
    }
}