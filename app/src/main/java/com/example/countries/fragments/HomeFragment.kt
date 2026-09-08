package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.CountryAdapter
import com.example.countries.CountryDetailActivity
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.DateUtils
import com.example.countries.FavoriteManager
import com.example.countries.MainActivity
import com.example.countries.PinManager
import com.example.countries.R
import com.example.countries.RecentlyViewedManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var pinManager: PinManager
    private lateinit var recentlyViewedManager: RecentlyViewedManager
    private lateinit var adapter: CountryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        favoriteManager = FavoriteManager(requireContext())
        pinManager = PinManager(requireContext())
        recentlyViewedManager = RecentlyViewedManager(requireContext())

        val statsCountriesCount = view.findViewById<TextView>(R.id.statsCountriesCount)
        val statsRegionsCount = view.findViewById<TextView>(R.id.statsRegionsCount)
        val statsLanguagesCount = view.findViewById<TextView>(R.id.statsLanguagesCount)
        val recentlyViewedList = view.findViewById<RecyclerView>(R.id.recentlyViewedList)
        val emptyRecent = view.findViewById<LinearLayout>(R.id.emptyRecent)
        val favoritesCountText = view.findViewById<TextView>(R.id.favoritesCountText)
        val pinnedCountText = view.findViewById<TextView>(R.id.pinnedCountText)
        val continentCountContainer = view.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.continentCountContainer)
        val lastUpdatedText = view.findViewById<TextView>(R.id.lastUpdatedText)
        val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicator)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val retryHomeButton = view.findViewById<TextView>(R.id.retryHomeButton)
        val scrollView = view.findViewById<android.widget.ScrollView>(R.id.homeScrollView)

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, 16)
            insets
        }

        // Share the activity-level flag cache so returning to Home doesn't
        // re-download every recently-viewed flag
        val imageCache: MutableMap<String, android.graphics.Bitmap> =
            (activity as? MainActivity)?.imageCache ?: mutableMapOf()

        adapter = CountryAdapter(emptyList(), imageCache, { country ->
            val intent = android.content.Intent(requireContext(), CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        })

        recentlyViewedList.layoutManager = LinearLayoutManager(requireContext())
        recentlyViewedList.adapter = adapter

        retryHomeButton.setOnClickListener {
            viewModel.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Loading -> {
                        loadingIndicator?.visibility = View.VISIBLE
                        errorContainer?.visibility = View.GONE
                        statsCountriesCount.text = "--"
                        statsRegionsCount.text = "--"
                        statsLanguagesCount.text = "--"
                    }
                    is CountriesUiState.Success -> {
                        loadingIndicator?.visibility = View.GONE
                        errorContainer?.visibility = View.GONE
                        val countries = state.countries
                        statsCountriesCount.text = "${countries.size}"

                        val regions = countries.map { it.region }.distinct()
                        statsRegionsCount.text = "${regions.size}"

                        val languages = mutableSetOf<String>()
                        countries.forEach { country ->
                            country.languages.split(", ").forEach { lang ->
                                if (lang.isNotBlank() && lang != "Not available") {
                                    languages.add(lang)
                                }
                            }
                        }
                        statsLanguagesCount.text = "${languages.size}"

                        val favCount = favoriteManager.getFavorites(countries).size
                        favoritesCountText.text = "$favCount favorite(s)"

                        val pinnedCount = pinManager.getPinned(countries).size
                        pinnedCountText.text = "$pinnedCount pinned"

                        val continentCounts = getContinentCounts(countries)
                        continentCountContainer.removeAllViews()
                        for ((continent, count) in continentCounts) {
                            val itemView = layoutInflater.inflate(R.layout.item_continent_home, continentCountContainer, false)
                            val nameText = itemView.findViewById<TextView>(R.id.continentName)
                            val countText = itemView.findViewById<TextView>(R.id.continentCount)
                            nameText.text = continent
                            countText.text = "$count"
                            val params = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                                width = 0
                                rowSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                                columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                            }
                            itemView.layoutParams = params
                            continentCountContainer.addView(itemView)
                        }

                        val lastRefreshed = System.currentTimeMillis()
                        lastUpdatedText.text = "Last refreshed: ${DateUtils.formatTimeAgo(lastRefreshed)}"

                        val recent = recentlyViewedManager.getRecentlyViewed(countries)
                        if (recent.isNotEmpty()) {
                            emptyRecent.visibility = View.GONE
                            recentlyViewedList.visibility = View.VISIBLE
                            adapter.updateData(recent)
                        } else {
                            emptyRecent.visibility = View.VISIBLE
                            recentlyViewedList.visibility = View.GONE
                        }
                    }
                    is CountriesUiState.Error -> {
                        loadingIndicator?.visibility = View.GONE
                        errorContainer?.visibility = View.VISIBLE
                        statsCountriesCount.text = "0"
                        statsRegionsCount.text = "0"
                        statsLanguagesCount.text = "0"
                    }
                    is CountriesUiState.Empty -> {
                        loadingIndicator?.visibility = View.GONE
                        errorContainer?.visibility = View.GONE
                        statsCountriesCount.text = "0"
                        statsRegionsCount.text = "0"
                        statsLanguagesCount.text = "0"
                    }
                }
            }
        }
    }

    private fun getContinentCounts(countries: List<com.example.countries.Country>): Map<String, Int> {
        // Group by region (Africa, Americas, Asia, Europe, Oceania, Antarctic).
        // Regions are mutually exclusive, unlike continents after the Americas
        // mapping, so the dashboard counts always sum to the country total.
        val counts = mutableMapOf<String, Int>()
        countries.forEach { country ->
            val key = country.region.ifEmpty { "Not available" }
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts.toSortedMap()
    }
}
