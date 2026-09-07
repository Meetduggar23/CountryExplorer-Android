package com.example.countries

import java.io.Serializable

data class Country(
    val commonName: String,
    val officialName: String,
    val capital: String,
    val region: String,
    val subregion: String,
    val population: Long,
    val area: Double,
    val cca2: String,
    val cca3: String,
    val currencies: String,
    val languages: String,
    val timezones: List<String>,
    val continents: List<String>,
    val borders: List<String>,
    val flagUrl: String
) : Serializable {
    fun getCountriesUsingLanguage(allCountries: List<Country>): List<Country> {
        return allCountries.filter { it.languages.contains(this.languages.split(", ").firstOrNull() ?: "") }
    }
}