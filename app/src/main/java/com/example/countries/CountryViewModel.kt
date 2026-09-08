package com.example.countries

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class CountriesUiState {
    object Loading : CountriesUiState()
    data class Success(val countries: List<Country>, val totalCount: Int = countries.size) : CountriesUiState()
    data class Error(val message: String, val isAuthError: Boolean = false) : CountriesUiState()
    object Empty : CountriesUiState()
}

class CountryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CountryRepository()

    private val _uiState = MutableStateFlow<CountriesUiState>(CountriesUiState.Loading)
    val uiState: StateFlow<CountriesUiState> = _uiState.asStateFlow()

    private val _allCountries = mutableListOf<Country>()
    val allCountries: List<Country> get() = _allCountries.toList()

    val favoriteManager = FavoriteManager(application)
    val recentlyViewedManager = RecentlyViewedManager(application)

    init {
        loadCountries()
    }

    fun loadCountries() {
        viewModelScope.launch {
            _uiState.value = CountriesUiState.Loading
            try {
                val countries = repository.getCountries()
                _allCountries.clear()
                _allCountries.addAll(countries)
                if (countries.isEmpty()) {
                    _uiState.value = CountriesUiState.Empty
                } else {
                    _uiState.value = CountriesUiState.Success(countries, countries.size)
                }
            } catch (e: ApiException) {
                _uiState.value = CountriesUiState.Error(
                    message = e.message ?: "An error occurred",
                    isAuthError = e.statusCode == 401 || e.statusCode == 403
                )
            } catch (e: Exception) {
                _uiState.value = CountriesUiState.Error(
                    message = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = CountriesUiState.Success(_allCountries, _allCountries.size)
            return
        }
        val filtered = _allCountries.filter { country ->
            country.commonName.contains(query, ignoreCase = true) ||
                country.officialName.contains(query, ignoreCase = true) ||
                country.capital.contains(query, ignoreCase = true) ||
                country.region.contains(query, ignoreCase = true) ||
                country.subregion.contains(query, ignoreCase = true) ||
                country.cca2.contains(query, ignoreCase = true) ||
                country.cca3.contains(query, ignoreCase = true)
        }
        _uiState.value = CountriesUiState.Success(filtered, _allCountries.size)
    }

    fun refresh() {
        loadCountries()
    }

    fun getFilteredCountries(): List<Country> {
        return when (val state = _uiState.value) {
            is CountriesUiState.Success -> state.countries
            else -> _allCountries
        }
    }

    fun getCountriesByContinent(continent: String): List<Country> {
        return _allCountries.filter { it.continents.any { c -> c.equals(continent, ignoreCase = true) } }
    }

    fun getCountriesByRegion(region: String): List<Country> {
        return _allCountries.filter { it.region.equals(region, ignoreCase = true) }
    }

    fun getCountriesByLanguage(language: String): List<Country> {
        return _allCountries.filter {
            it.languages.split(", ").any { lang ->
                lang.equals(language, ignoreCase = true)
            }
        }
    }

    fun getCountriesByCurrency(currency: String): List<Country> {
        return _allCountries.filter {
            it.currencies.contains(currency, ignoreCase = true)
        }
    }

    fun getRankings(type: String, ascending: Boolean = false): List<Country> {
        return when (type.lowercase()) {
            "population" -> {
                val sorted = _allCountries.sortedBy { it.population }
                if (ascending) sorted else sorted.reversed()
            }
            "area" -> {
                val sorted = _allCountries.sortedBy { it.area }
                if (ascending) sorted else sorted.reversed()
            }
            "name" -> {
                val sorted = _allCountries.sortedBy { it.commonName }
                if (ascending) sorted else sorted.reversed()
            }
            else -> _allCountries
        }
    }

    fun getRandomCountry(): Country? {
        if (_allCountries.isEmpty()) return null
        return _allCountries[Random.nextInt(_allCountries.size)]
    }

    fun getNeighbors(country: Country): List<Country> {
        return _allCountries.filter {
            it.cca3 in country.borders
        }
    }
}
