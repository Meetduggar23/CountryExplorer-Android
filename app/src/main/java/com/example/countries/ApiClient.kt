package com.example.countries

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiClient {

    private const val BASE_URL = "https://restcountries.com/v3.1/all"
    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 15000

    fun fetchCountries(): Result<List<Country>> {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        return try {
            val url = URL(BASE_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(
                    Exception("HTTP error: $responseCode - ${connection.responseMessage ?: "Unknown error"}")
                )
            }

            reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            val countries = parseCountries(response.toString())
            if (countries.isEmpty()) {
                Result.failure(Exception("No countries found in response"))
            } else {
                Result.success(countries)
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please check your internet and try again."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network settings."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch countries: ${e.message}"))
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun parseCountries(json: String): List<Country> {
        val countries = mutableListOf<Country>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                countries.add(parseCountryObject(obj))
            } catch (_: Exception) {
                // skip malformed country objects
            }
        }

        countries.sortBy { it.commonName.lowercase() }
        return countries
    }

    private fun parseCountryObject(obj: JSONObject): Country {
        val name = obj.optJSONObject("name") ?: JSONObject()
        val commonName = name.optString("common", "Unknown")
        val officialName = name.optString("official", "Unknown")

        val capitalArray = obj.optJSONArray("capital")
        val capital = if (capitalArray != null && capitalArray.length() > 0) {
            capitalArray.optString(0, "Not available")
        } else {
            "Not available"
        }

        val region = obj.optString("region", "Unknown")
        val subregion = obj.optString("subregion", "Not available")
        val population = obj.optLong("population", 0)
        val area = obj.optDouble("area", 0.0)
        val cca2 = obj.optString("cca2", "Unknown")
        val cca3 = obj.optString("cca3", "Unknown")

        val currenciesObj = obj.optJSONObject("currencies")
        val currencies = if (currenciesObj != null && currenciesObj.length() > 0) {
            val names = mutableListOf<String>()
            for (key in currenciesObj.keys()) {
                val curr = currenciesObj.optJSONObject(key)
                val name = curr?.optString("name", "") ?: ""
                val symbol = curr?.optString("symbol", "") ?: ""
                if (name.isNotEmpty()) {
                    names.add(if (symbol.isNotEmpty()) "$name ($symbol)" else name)
                }
            }
            names.joinToString(", ").ifEmpty { "Not available" }
        } else {
            "Not available"
        }

        val languagesObj = obj.optJSONObject("languages")
        val languages = if (languagesObj != null && languagesObj.length() > 0) {
            val names = mutableListOf<String>()
            for (key in languagesObj.keys()) {
                val langName = languagesObj.optString(key, "")
                if (langName.isNotEmpty()) names.add(langName)
            }
            names.joinToString(", ").ifEmpty { "Not available" }
        } else {
            "Not available"
        }

        val timezonesArray = obj.optJSONArray("timezones")
        val timezones = mutableListOf<String>()
        if (timezonesArray != null) {
            for (j in 0 until timezonesArray.length()) {
                timezones.add(timezonesArray.optString(j, ""))
            }
        }

        val continentsArray = obj.optJSONArray("continents")
        val continents = mutableListOf<String>()
        if (continentsArray != null) {
            for (j in 0 until continentsArray.length()) {
                continents.add(continentsArray.optString(j, ""))
            }
        }

        val bordersArray = obj.optJSONArray("borders")
        val borders = mutableListOf<String>()
        if (bordersArray != null) {
            for (j in 0 until bordersArray.length()) {
                borders.add(bordersArray.optString(j, ""))
            }
        }

        val flagsObj = obj.optJSONObject("flags")
        val flagUrl = flagsObj?.optString("png", "") ?: ""

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
}
