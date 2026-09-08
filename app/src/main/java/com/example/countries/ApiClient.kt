package com.example.countries

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ApiClient {

    companion object {
        private const val TAG = "ApiClient"
        // NOTE: REST Countries v1-v4 are fully deprecated and return errors.
        // REST Countries v5 requires a paid API key (500 req/month free tier).
        // The demo key "rc_live_demo" only returns 1 sample country (Canada).
        // We use countries.dev instead — a free, no-key alternative that provides
        // the same country data with the same field names as the old v3.1 API.
        private const val BASE_URL = "https://countries.dev/countries"
        private const val PAGE_SIZE = 250
    }

    suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
        val allCountries = mutableListOf<Country>()
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val url = URL("$BASE_URL?limit=$PAGE_SIZE&offset=$offset")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode
                val responseBody = readStream(connection.inputStream)
                Log.d(TAG, "Page: offset=$offset, responseCode=$responseCode")

                when {
                    responseCode == HttpURLConnection.HTTP_OK -> {
                        val parsed = parseResponse(responseBody)
                        if (parsed.isEmpty()) {
                            hasMore = false
                        } else {
                            allCountries.addAll(parsed)
                            Log.d(TAG, "Page: offset=$offset, received=${parsed.size}")
                            offset += PAGE_SIZE
                            if (parsed.size < PAGE_SIZE) {
                                hasMore = false
                            }
                        }
                    }
                    responseCode == 429 -> {
                        throw ApiException(
                            "Too many requests. Please try again later.",
                            responseCode
                        )
                    }
                    responseCode == HttpURLConnection.HTTP_NOT_FOUND -> {
                        throw ApiException(
                            "Country data endpoint not found.",
                            responseCode
                        )
                    }
                    responseCode >= 500 -> {
                        throw ApiException(
                            "Country service is temporarily unavailable.",
                            responseCode
                        )
                    }
                    else -> {
                        val errorMsg = extractErrorMessage(responseBody)
                        throw ApiException(
                            errorMsg ?: "Invalid request (HTTP $responseCode)",
                            responseCode
                        )
                    }
                }
            } catch (e: ApiException) {
                throw e
            } catch (e: java.net.UnknownHostException) {
                throw ApiException("Unable to connect. Check your internet connection.", -1)
            } catch (e: java.net.SocketTimeoutException) {
                throw ApiException("Connection timed out. Check your internet connection.", -1)
            } catch (e: java.io.IOException) {
                throw ApiException("Unable to connect. Check your internet connection.", -1)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                throw ApiException(
                    e.message ?: "An unexpected error occurred.",
                    -1
                )
            } finally {
                connection.disconnect()
            }
        }

        Log.d(TAG, "TOTAL COUNTRIES LOADED = ${allCountries.size}")
        allCountries
    }

    private fun readStream(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseResponse(responseBody: String): List<Country> {
        val trimmed = responseBody.trim()

        if (trimmed.isEmpty()) {
            throw ApiException("No country data was received.", -1)
        }

        return if (trimmed.startsWith("[")) {
            parseJsonArray(JSONArray(trimmed))
        } else if (trimmed.startsWith("{")) {
            parseJsonObject(JSONObject(trimmed))
        } else {
            throw ApiException("Received invalid country data.", -1)
        }
    }

    private fun parseJsonObject(jsonObject: JSONObject): List<Country> {
        if (jsonObject.has("errors")) {
            val errors = jsonObject.optJSONArray("errors")
            val message = if (errors != null && errors.length() > 0) {
                errors.getJSONObject(0).optString("message", "API error")
            } else {
                "API returned an error"
            }
            throw ApiException(message, -1)
        }
        return emptyList()
    }

    private fun parseJsonArray(jsonArray: JSONArray): List<Country> {
        val countries = mutableListOf<Country>()
        for (i in 0 until jsonArray.length()) {
            try {
                val json = jsonArray.getJSONObject(i)
                countries.add(parseCountry(json))
            } catch (e: Exception) {
                Log.w(TAG, "Skipping malformed country entry at index $i: ${e.message}")
            }
        }
        return countries
    }

    private fun parseCountry(json: JSONObject): Country {
        val commonName = json.optString("name", "Not available")
        val nativeName = json.optString("nativeName", "")
        val officialName = if (nativeName.isNotEmpty()) nativeName else commonName

        val capital = json.optString("capital", "Not available")

        val region = json.optString("region", "Not available")
        val subregion = json.optString("subregion", "Not available")

        val population = json.optLong("population", 0)
        val area = json.optDouble("area", 0.0)

        val cca2 = json.optString("alpha2Code", "")
        val cca3 = json.optString("alpha3Code", "")

        val currencies = parseCurrenciesArray(json.optJSONArray("currencies"))
        val languages = parseLanguagesArray(json.optJSONArray("languages"))

        val timezones = mutableListOf<String>()
        val timezonesArray = json.optJSONArray("timezones")
        if (timezonesArray != null) {
            for (i in 0 until timezonesArray.length()) {
                timezones.add(timezonesArray.getString(i))
            }
        }

        // The API reports "Americas" as the region, but continents are
        // North America / South America. Map it so the Continents page and
        // quiz continent questions include the American countries.
        val continents = mutableListOf<String>()
        when (region) {
            "Americas" -> {
                continents.add("North America")
                continents.add("South America")
            }
            "", "Not available" -> { /* leave empty */ }
            else -> continents.add(region)
        }

        val borders = mutableListOf<String>()
        val bordersArray = json.optJSONArray("borders")
        if (bordersArray != null) {
            for (i in 0 until bordersArray.length()) {
                borders.add(bordersArray.getString(i))
            }
        }

        val flagUrl = if (cca2.isNotEmpty()) {
            "https://flags.restcountries.com/v5/w320/${cca2.lowercase()}.png"
        } else {
            val flags = json.optJSONObject("flags")
            flags?.optString("png") ?: ""
        }

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

    private fun parseCurrenciesArray(jsonArray: JSONArray?): String {
        if (jsonArray == null) return "Not available"
        val currencies = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            try {
                val currency = jsonArray.getJSONObject(i)
                val name = currency.optString("name", "")
                val symbol = currency.optString("symbol", "")
                if (name.isNotEmpty()) {
                    currencies.add(if (symbol.isNotEmpty()) "$name ($symbol)" else name)
                }
            } catch (e: Exception) {
                // skip malformed currency
            }
        }
        return if (currencies.isNotEmpty()) currencies.joinToString(", ") else "Not available"
    }

    private fun parseLanguagesArray(jsonArray: JSONArray?): String {
        if (jsonArray == null) return "Not available"
        val languages = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            try {
                val langObj = jsonArray.getJSONObject(i)
                val name = langObj.optString("name", "")
                if (name.isNotEmpty()) languages.add(name)
            } catch (e: Exception) {
                // skip malformed language
            }
        }
        return if (languages.isNotEmpty()) languages.joinToString(", ") else "Not available"
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            if (json.has("errors")) {
                val errors = json.getJSONArray("errors")
                if (errors.length() > 0) {
                    errors.getJSONObject(0).optString("message")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

class ApiException(message: String, val statusCode: Int) : Exception(message)
