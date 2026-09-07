package com.example.countries

class CountryRepository {

    private val apiClient = ApiClient()

    suspend fun getCountries(): List<Country> {
        return apiClient.getCountries()
    }
}