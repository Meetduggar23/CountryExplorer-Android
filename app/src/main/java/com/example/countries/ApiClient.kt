package com.example.countries

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient {

    private val baseUrl = "https://restcountries.com/v3.1/all"

    suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
        try {
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()
                parseCountries(JSONArray(response))
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch countries: ${e.message}")
        }
    }

    private fun parseCountries(jsonArray: JSONArray): List<Country> {
        val countries = mutableListOf<Country>()
        for (i in 0 until jsonArray.length()) {
            try {
                val json = jsonArray.getJSONObject(i)
                countries.add(parseCountry(json))
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }
        return countries
    }

    private fun parseCountry(json: JSONObject): Country {
        val name = json.getJSONObject("name")
        val commonName = name.optString("common", "")
        val officialName = name.optString("official", "")

        val capitalArray = json.optJSONArray("capital")
        val capital = if (capitalArray != null && capitalArray.length() > 0) {
            capitalArray.getString(0)
        } else {
            "N/A"
        }

        val region = json.optString("region", "")
        val subregion = json.optString("subregion", "")

        val population = json.optLong("population", 0)
        val area = json.optDouble("area", 0.0)

        val cca2 = json.optString("cca2", "")
        val cca3 = json.optString("cca3", "")

        val currencies = parseCurrencies(json.optJSONObject("currencies"))
        val languages = parseLanguages(json.optJSONObject("languages"))

        val timezones = mutableListOf<String>()
        val timezonesArray = json.optJSONArray("timezones")
        if (timezonesArray != null) {
            for (i in 0 until timezonesArray.length()) {
                timezones.add(timezonesArray.getString(i))
            }
        }

        val continents = mutableListOf<String>()
        val continentsArray = json.optJSONArray("continents")
        if (continentsArray != null) {
            for (i in 0 until continentsArray.length()) {
                continents.add(continentsArray.getString(i))
            }
        }

        val borders = mutableListOf<String>()
        val bordersArray = json.optJSONArray("borders")
        if (bordersArray != null) {
            for (i in 0 until bordersArray.length()) {
                borders.add(bordersArray.getString(i))
            }
        }

        val flags = json.optJSONObject("flags")
        val flagUrl = flags?.optString("png", "") ?: ""

        return Country(
            commonName = commonName,
            officialName = officialName,
            capital = capital,
            region = region,
            subregion = subregion,
            population = population,
            area = area,
            cca2 = cca2,
            cca3 = cca3,
            currencies = currencies,
            languages = languages,
            timezones = timezones,
            continents = continents,
            borders = borders,
            flagUrl = flagUrl
        )
    }

    private fun parseCurrencies(json: JSONObject?): String {
        if (json == null) return "N/A"
        val currencies = mutableListOf<String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val currency = json.getJSONObject(key)
            val name = currency.optString("name", "")
            val symbol = currency.optString("symbol", "")
            currencies.add("$name ($symbol)")
        }
        return if (currencies.isNotEmpty()) currencies.joinToString(", ") else "N/A"
    }

    private fun parseLanguages(json: JSONObject?): String {
        if (json == null) return "N/A"
        val languages = mutableListOf<String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            languages.add(json.getString(keys.next()))
        }
        return if (languages.isNotEmpty()) languages.joinToString(", ") else "N/A"
    }
}