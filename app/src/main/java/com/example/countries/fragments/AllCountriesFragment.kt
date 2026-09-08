package com.example.countries.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.CountryAdapter
import com.example.countries.CountryDetailActivity
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.FavoriteManager
import com.example.countries.PinManager
import com.example.countries.R
import com.example.countries.SearchHistoryManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AllCountriesFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var pinManager: PinManager
    private lateinit var searchHistoryManager: SearchHistoryManager
    private lateinit var adapter: CountryAdapter
    private val imageCache = mutableMapOf<String, android.graphics.Bitmap>()

    private var currentSearchQuery = ""
    private var selectedLetter: String? = null
    private var currentSortId = 1
    private var filterRegion: String? = null
    private var filterContinent: String? = null
    private var filterLanguage: String? = null
    private var filterCurrency: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageView
    private lateinit var countryCountText: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var resetSortButton: TextView
    private lateinit var resetFiltersButtonEmpty: TextView
    private lateinit var scrollUpFab: FloatingActionButton
    private lateinit var sortButton: View
    private lateinit var azFilterContainer: LinearLayout
    private lateinit var searchHistoryContainer: LinearLayout

    private val sortOptions = listOf(
        1 to "Name (A-Z)",
        2 to "Name (Z-A)",
        3 to "Population (Highest)",
        4 to "Population (Lowest)",
        5 to "Area (Largest)",
        6 to "Area (Smallest)"
    )

    companion object {
        fun newInstance(
            filterRegion: String? = null,
            filterContinent: String? = null,
            filterLanguage: String? = null,
            filterCurrency: String? = null
        ): AllCountriesFragment {
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_countries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        favoriteManager = FavoriteManager(requireContext())
        pinManager = PinManager(requireContext())
        searchHistoryManager = SearchHistoryManager(requireContext())

        filterRegion = arguments?.getString("filterRegion")
        filterContinent = arguments?.getString("filterContinent")
        filterLanguage = arguments?.getString("filterLanguage")
        filterCurrency = arguments?.getString("filterCurrency")

        initViews(view)
        setupAdapter()
        setupSearch()
        setupAZFilter()
        setupSortButton()
        setupScrollToTop()
        setupResetFilters()
        observeViewModel()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        clearSearchButton = view.findViewById(R.id.clearSearchButton)
        countryCountText = view.findViewById(R.id.countryCountText)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        resetSortButton = view.findViewById(R.id.resetSortButton)
        resetFiltersButtonEmpty = view.findViewById(R.id.resetFiltersButtonEmpty)
        scrollUpFab = view.findViewById(R.id.scrollUpFab)
        sortButton = view.findViewById(R.id.sortButton)
        azFilterContainer = view.findViewById(R.id.azFilterContainer)
        searchHistoryContainer = view.findViewById(R.id.searchHistoryContainer)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupAdapter() {
        adapter = CountryAdapter(
            emptyList(),
            imageCache,
            onClick = { country ->
                pinManager.addPin(country.cca3)
                viewModel.recentlyViewedManager.addCountry(country.cca3)
                val intent = Intent(requireContext(), CountryDetailActivity::class.java)
                intent.putExtra("country", country)
                startActivity(intent)
            },
            onFavoriteToggle = { country ->
                if (favoriteManager.isFavorite(country.cca3)) {
                    favoriteManager.removeFavorite(country.cca3)
                    Snackbar.make(recyclerView, "${country.commonName} removed from favorites", Snackbar.LENGTH_SHORT).show()
                } else {
                    favoriteManager.addFavorite(country.cca3)
                    Snackbar.make(recyclerView, "${country.commonName} added to favorites", Snackbar.LENGTH_SHORT).show()
                }
                refreshCurrentList()
            }
        )
        adapter.setFavoriteManager(favoriteManager)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            currentSearchQuery = ""
            clearSearchButton.visibility = View.GONE
            hideSearchHistory()
            refreshCurrentList()
        }

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                clearSearchButton.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                if (currentSearchQuery.isEmpty()) {
                    hideSearchHistory()
                }
                refreshCurrentList()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchHistoryManager.addQuery(query)
                }
                hideSearchHistory()
                true
            } else false
        }

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && searchEditText.text.isEmpty()) {
                refreshSearchHistory()
            } else if (!hasFocus) {
                hideSearchHistory()
            }
        }
    }

    private fun refreshSearchHistory() {
        val queries = searchHistoryManager.getQueries()
        searchHistoryContainer.removeAllViews()

        if (queries.isEmpty()) {
            hideSearchHistory()
            return
        }

        searchHistoryContainer.visibility = View.VISIBLE

        for (query in queries.take(5)) {
            val historyItem = TextView(requireContext()).apply {
                text = query
                setTextColor(ContextCompat.getColor(context, R.color.text_light_gray))
                textSize = 13f
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                setOnClickListener {
                    searchEditText.setText(query)
                    searchEditText.setSelection(query.length)
                    hideSearchHistory()
                }
            }
            searchHistoryContainer.addView(historyItem)
        }
    }

    private fun hideSearchHistory() {
        searchHistoryContainer.visibility = View.GONE
    }

    private fun setupAZFilter() {
        azFilterContainer.removeAllViews()

        val allLetters = mutableListOf("All") + ('A'..'Z').map { it.toString() }

        for (letter in allLetters) {
            val letterView = TextView(requireContext()).apply {
                text = letter
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                minWidth = dpToPx(32)
                setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))

                val isSelected = (letter == "All" && selectedLetter == null) || letter == selectedLetter

                if (isSelected) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.accent_red))
                    setTextColor(ContextCompat.getColor(context, R.color.text_white))
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }

                setOnClickListener {
                    if (letter == "All") {
                        selectedLetter = null
                    } else {
                        selectedLetter = if (selectedLetter == letter) null else letter
                    }
                    updateAZFilterSelection()
                    refreshCurrentList()
                }
            }

            val marginParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(4)
            }
            letterView.layoutParams = marginParams

            azFilterContainer.addView(letterView)
        }
    }

    private fun updateAZFilterSelection() {
        for (i in 0 until azFilterContainer.childCount) {
            val child = azFilterContainer.getChildAt(i) as? TextView ?: continue
            val letter = child.text.toString()

            val isSelected = (letter == "All" && selectedLetter == null) || letter == selectedLetter

            if (isSelected) {
                child.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))
            } else {
                child.setBackgroundColor(Color.TRANSPARENT)
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
        }
    }

    private fun setupSortButton() {
        sortButton.setOnClickListener { showSortPopup(it) }
    }

    private fun showSortPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)

        sortOptions.forEach { (id, label) ->
            popup.menu.add(0, id, sortOptions.indexOfFirst { it.first == id }, label)
        }

        for (i in 0 until popup.menu.size()) {
            @Suppress("DEPRECATION")
            if (popup.menu.getItem(i)?.itemId == currentSortId) {
                popup.menu.getItem(i)?.isChecked = true
                break
            }
        }
        @Suppress("DEPRECATION")
        popup.menu.setGroupCheckable(0, true, true)

        popup.setOnMenuItemClickListener { item ->
            currentSortId = item.itemId
            val sortLabel = sortOptions.firstOrNull { it.first == item.itemId }?.second
            if (sortLabel != null) {
                (sortButton as? TextView)?.text = sortLabel
            }
            resetSortButton.visibility = if (currentSortId != 1) View.VISIBLE else View.GONE
            refreshCurrentList()
            true
        }
        popup.show()
    }

    private fun setupScrollToTop() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
                scrollUpFab.visibility = if (firstVisible > 3) View.VISIBLE else View.GONE
            }
        })

        scrollUpFab.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private fun setupResetFilters() {
        val resetAction = {
            searchEditText.text.clear()
            currentSearchQuery = ""
            selectedLetter = null
            currentSortId = 1
            clearSearchButton.visibility = View.GONE
            (sortButton as? TextView)?.text = sortOptions.first().second
            resetSortButton.visibility = View.GONE
            updateAZFilterSelection()
            refreshCurrentList()
            Snackbar.make(recyclerView, "Filters reset", Snackbar.LENGTH_SHORT).show()
        }

        resetSortButton.setOnClickListener { resetAction() }
        resetFiltersButtonEmpty.setOnClickListener { resetAction() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Loading -> {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                        emptyStateText.text = "Loading countries..."
                    }
                    is CountriesUiState.Success -> {
                        refreshCurrentList()
                    }
                    is CountriesUiState.Error -> {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                        emptyStateText.text = "Error: ${state.message}"
                    }
                }
            }
        }
    }

    private fun refreshCurrentList() {
        val state = viewModel.uiState.value
        if (state !is CountriesUiState.Success) return

        var result = state.countries.toMutableList()

        filterRegion?.let { region ->
            result = result.filter { it.region.equals(region, ignoreCase = true) }.toMutableList()
        }
        filterContinent?.let { continent ->
            result = result.filter { country ->
                country.continents.any { it.equals(continent, ignoreCase = true) }
            }.toMutableList()
        }
        filterLanguage?.let { language ->
            result = result.filter { country ->
                country.languages.lowercase().contains(language.lowercase())
            }.toMutableList()
        }
        filterCurrency?.let { currency ->
            result = result.filter { country ->
                country.currencies.lowercase().contains(currency.lowercase())
            }.toMutableList()
        }

        selectedLetter?.let { letter ->
            result = result.filter {
                it.commonName.uppercase().startsWith(letter)
            }.toMutableList()
        }

        if (currentSearchQuery.isNotBlank()) {
            val lower = currentSearchQuery.lowercase()
            result = result.filter {
                it.commonName.lowercase().contains(lower) ||
                    it.capital.lowercase().contains(lower) ||
                    it.region.lowercase().contains(lower) ||
                    it.subregion.lowercase().contains(lower) ||
                    it.cca2.lowercase().contains(lower) ||
                    it.cca3.lowercase().contains(lower) ||
                    it.languages.lowercase().contains(lower)
            }.toMutableList()
        }

        val pinnedCodes = pinManager.getPinnedCodes()
        val pinned = result.filter { it.cca3 in pinnedCodes }
        val unpinned = result.filter { it.cca3 !in pinnedCodes }

        val sortedUnpinned = when (currentSortId) {
            1 -> unpinned.sortedBy { it.commonName }
            2 -> unpinned.sortedByDescending { it.commonName }
            3 -> unpinned.sortedByDescending { it.population }
            4 -> unpinned.sortedBy { it.population }
            5 -> unpinned.sortedByDescending { it.area }
            6 -> unpinned.sortedBy { it.area }
            else -> unpinned.sortedBy { it.commonName }
        }

        val sortedPinned = when (currentSortId) {
            1 -> pinned.sortedBy { it.commonName }
            2 -> pinned.sortedByDescending { it.commonName }
            3 -> pinned.sortedByDescending { it.population }
            4 -> pinned.sortedBy { it.population }
            5 -> pinned.sortedByDescending { it.area }
            6 -> pinned.sortedBy { it.area }
            else -> pinned.sortedBy { it.commonName }
        }

        val finalList = sortedPinned + sortedUnpinned

        adapter.updateData(finalList)
        countryCountText.text = "${finalList.size} countries"

        if (finalList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyStateText.text = when {
                currentSearchQuery.isNotBlank() -> "No results for \"$currentSearchQuery\""
                selectedLetter != null -> "No countries starting with $selectedLetter"
                else -> "No countries found"
            }
            resetFiltersButtonEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            resetFiltersButtonEmpty.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentList()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
