package com.example.countries

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pinned_countries", Context.MODE_PRIVATE)

    fun addPin(cca3: String) {
        val codes = getPinnedCodesList()
        if (!codes.contains(cca3)) {
            codes.add(cca3)
            saveCodes(codes)
        }
    }

    fun removePin(cca3: String) {
        val codes = getPinnedCodesList()
        codes.remove(cca3)
        saveCodes(codes)
    }

    fun isPinned(cca3: String): Boolean {
        return getPinnedCodesList().contains(cca3)
    }

    fun getPinnedCodes(): Set<String> {
        return getPinnedCodesList().toSet()
    }

    fun getPinned(allCountries: List<Country>): List<Country> {
        val codes = getPinnedCodesList()
        val countryMap = allCountries.associateBy { it.cca3 }
        return codes.mapNotNull { countryMap[it] }
    }

    private fun getPinnedCodesList(): MutableList<String> {
        val json = prefs.getString("pinned_codes", null) ?: return mutableListOf()
        val array = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun saveCodes(codes: List<String>) {
        val array = JSONArray()
        codes.forEach { array.put(it) }
        prefs.edit().putString("pinned_codes", array.toString()).apply()
    }
}
