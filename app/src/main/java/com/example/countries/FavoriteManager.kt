package com.example.countries

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class FavoriteManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("country_favorites", Context.MODE_PRIVATE)

    fun addFavorite(cca3: String) {
        val codes = getFavoriteCodes().toMutableSet()
        codes.add(cca3)
        saveCodes(codes)
    }

    fun removeFavorite(cca3: String) {
        val codes = getFavoriteCodes().toMutableSet()
        codes.remove(cca3)
        saveCodes(codes)
    }

    fun isFavorite(cca3: String): Boolean {
        return getFavoriteCodes().contains(cca3)
    }

    fun getFavoriteCodes(): Set<String> {
        val jsonString = sharedPreferences.getString("favorite_codes", null) ?: return emptySet()
        val jsonArray = JSONArray(jsonString)
        val codes = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            codes.add(jsonArray.getString(i))
        }
        return codes
    }

    fun getFavorites(allCountries: List<Country>): List<Country> {
        val favoriteCodes = getFavoriteCodes()
        return allCountries.filter { it.cca3 in favoriteCodes }
    }

    private fun saveCodes(codes: Set<String>) {
        val jsonArray = JSONArray()
        codes.forEach { jsonArray.put(it) }
        sharedPreferences.edit().putString("favorite_codes", jsonArray.toString()).apply()
    }
}