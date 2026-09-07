package com.example.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CountriesUiState {
    data object Loading : CountriesUiState()
    data class Success(
        val countries: List<Country>,
        val totalCount: Int,
        val searchQuery: String = ""
    ) : CountriesUiState()

    data class Error(val message: String) : CountriesUiState()
}

class CountryViewModel : ViewModel() {

    private val repository = CountryRepository()

    private val _uiState = MutableStateFlow<CountriesUiState>(CountriesUiState.Loading)
    val uiState: StateFlow<CountriesUiState> = _uiState

    private var isFetching = false

    init {
        loadCountries()
    }

    fun loadCountries() {
        if (isFetching) return
        isFetching = true

        val cached = repository.getCachedCountries()
        if (cached != null && cached.isNotEmpty()) {
            _uiState.value = CountriesUiState.Success(
                countries = cached,
                totalCount = cached.size
            )
            isFetching = false
            return
        }

        _uiState.value = CountriesUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchCountries()
            result.onSuccess { countries ->
                _uiState.value = CountriesUiState.Success(
                    countries = countries,
                    totalCount = countries.size
                )
            }.onFailure { error ->
                _uiState.value = CountriesUiState.Error(
                    error.message ?: "Unknown error occurred"
                )
            }
            isFetching = false
        }
    }

    fun search(query: String) {
        val current = _uiState.value
        if (current !is CountriesUiState.Success) return

        val filtered = repository.searchCountries(query)
        _uiState.value = current.copy(
            countries = filtered,
            searchQuery = query
        )
    }

    fun refresh() {
        isFetching = false
        _uiState.value = CountriesUiState.Loading
        loadCountries()
    }
}
