package com.example.countries

class CountryRepository {

    private var cachedCountries: List<Country>? = null

    fun getCachedCountries(): List<Country>? = cachedCountries

    suspend fun fetchCountries(): Result<List<Country>> {
        val result = ApiClient.fetchCountries()
        result.onSuccess { countries ->
            cachedCountries = countries
        }
        return result
    }

    fun searchCountries(query: String): List<Country> {
        val countries = cachedCountries ?: return emptyList()
        if (query.isBlank()) return countries

        val lowerQuery = query.trim().lowercase()
        return countries.filter { country ->
            country.commonName.lowercase().contains(lowerQuery) ||
                country.capital.lowercase().contains(lowerQuery) ||
                country.region.lowercase().contains(lowerQuery) ||
                country.cca3.lowercase().contains(lowerQuery)
        }
    }
}
