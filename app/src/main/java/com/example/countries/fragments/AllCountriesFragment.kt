package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.Country
import com.example.countries.CountryAdapter
import com.example.countries.CountryDetailActivity
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.FavoriteManager
import com.example.countries.MainActivity
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AllCountriesFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: CountryAdapter
    private var allCountries = listOf<Country>()
    private var filterRegion: String? = null
    private var filterContinent: String? = null
    private var filterLanguage: String? = null
    private var filterCurrency: String? = null

    companion object {
        fun newInstance(filterRegion: String? = null, filterContinent: String? = null,
                        filterLanguage: String? = null, filterCurrency: String? = null): AllCountriesFragment {
            return AllCountriesFragment().apply {
                arguments = Bundle().apply {
                    filterRegion?.let { putString("filterRegion", it) }
                    filterContinent?.let { putString("filterContinent", it) }
                    filterLanguage?.let { putString("filterLanguage", it) }
                    filterCurrency?.let { putString("filterCurrency", it) }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_countries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        favoriteManager = FavoriteManager(requireContext())

        filterRegion = arguments?.getString("filterRegion")
        filterContinent = arguments?.getString("filterContinent")
        filterLanguage = arguments?.getString("filterLanguage")
        filterCurrency = arguments?.getString("filterCurrency")

        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val sortButton = view.findViewById<View>(R.id.sortButton)
        val refreshButton = view.findViewById<View>(R.id.refreshButton)
        val countryCountText = view.findViewById<TextView>(R.id.countryCountText)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)

        val imageCache = mutableMapOf<String, android.graphics.Bitmap>()
        adapter = CountryAdapter(emptyList(), imageCache, { country ->
            val intent = android.content.Intent(requireContext(), CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }, { country ->
            if (favoriteManager.isFavorite(country.cca3)) {
                favoriteManager.removeFavorite(country.cca3)
            } else {
                favoriteManager.addFavorite(country.cca3)
            }
            adapter.notifyDataSetChanged()
        })
        adapter.setFavoriteManager(favoriteManager)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCountries(searchEditText.text.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        sortButton.setOnClickListener { showSortPopup(it) }

        refreshButton.setOnClickListener {
            searchEditText.text.clear()
            viewModel.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
                    allCountries = applyFilters(state.countries)
                    filterCountries(searchEditText.text.toString())
                }
            }
        }
    }

    private fun applyFilters(countries: List<Country>): List<Country> {
        var result = countries
        filterRegion?.let { region ->
            result = result.filter { it.region.equals(region, ignoreCase = true) }
        }
        filterContinent?.let { continent ->
            result = result.filter { country ->
                country.continents.any { it.equals(continent, ignoreCase = true) }
            }
        }
        filterLanguage?.let { language ->
            result = result.filter { country ->
                country.languages.lowercase().contains(language.lowercase())
            }
        }
        filterCurrency?.let { currency ->
            result = result.filter { country ->
                country.currencies.lowercase().contains(currency.lowercase())
            }
        }
        return result
    }

    private fun filterCountries(query: String) {
        val view = view ?: return
        val countryCountText = view.findViewById<TextView>(R.id.countryCountText)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        val filtered = if (query.isBlank()) {
            allCountries
        } else {
            val lower = query.lowercase()
            allCountries.filter {
                it.commonName.lowercase().contains(lower) ||
                    it.capital.lowercase().contains(lower) ||
                    it.region.lowercase().contains(lower) ||
                    it.cca3.lowercase().contains(lower)
            }
        }

        countryCountText.text = "${filtered.size} countries"
        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showSortPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Name (A-Z)")
        popup.menu.add(0, 2, 1, "Name (Z-A)")
        popup.menu.add(0, 3, 2, "Population (Highest)")
        popup.menu.add(0, 4, 3, "Population (Lowest)")
        popup.menu.add(0, 5, 4, "Area (Largest)")
        popup.menu.add(0, 6, 5, "Area (Smallest)")

        popup.setOnMenuItemClickListener { item ->
            allCountries = when (item.itemId) {
                1 -> allCountries.sortedBy { it.commonName }
                2 -> allCountries.sortedByDescending { it.commonName }
                3 -> allCountries.sortedByDescending { it.population }
                4 -> allCountries.sortedBy { it.population }
                5 -> allCountries.sortedByDescending { it.area }
                6 -> allCountries.sortedBy { it.area }
                else -> allCountries
            }
            val searchQuery = view?.findViewById<EditText>(R.id.searchEditText)?.text?.toString() ?: ""
            filterCountries(searchQuery)
            true
        }
        popup.show()
    }
}
