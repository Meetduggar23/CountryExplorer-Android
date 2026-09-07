package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.CountryAdapter
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.FavoriteManager
import com.example.countries.MainActivity
import com.example.countries.R
import com.example.countries.RecentlyViewedManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
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
        recentlyViewedManager = RecentlyViewedManager(requireContext())

        val statsCountriesCount = view.findViewById<TextView>(R.id.statsCountriesCount)
        val statsRegionsCount = view.findViewById<TextView>(R.id.statsRegionsCount)
        val statsLanguagesCount = view.findViewById<TextView>(R.id.statsLanguagesCount)
        val btnAllCountries = view.findViewById<View>(R.id.btnAllCountries)
        val btnFavorites = view.findViewById<View>(R.id.btnFavorites)
        val btnCompare = view.findViewById<View>(R.id.btnCompare)
        val btnRandom = view.findViewById<View>(R.id.btnRandom)
        val btnQuiz = view.findViewById<View>(R.id.btnQuiz)
        val recentlyViewedList = view.findViewById<RecyclerView>(R.id.recentlyViewedList)
        val emptyRecent = view.findViewById<LinearLayout>(R.id.emptyRecent)
        val favoritesCountText = view.findViewById<TextView>(R.id.favoritesCountText)

        val imageCache = (activity as? MainActivity)?.let {
            mutableMapOf<String, android.graphics.Bitmap>()
        } ?: mutableMapOf()

        adapter = CountryAdapter(emptyList(), imageCache, { country ->
            val intent = android.content.Intent(requireContext(), com.example.countries.CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        })

        recentlyViewedList.layoutManager = LinearLayoutManager(requireContext())
        recentlyViewedList.adapter = adapter

        btnAllCountries.setOnClickListener {
            (activity as MainActivity).loadFragment(AllCountriesFragment())
        }
        btnFavorites.setOnClickListener {
            (activity as MainActivity).loadFragment(FavoritesFragment())
        }
        btnCompare.setOnClickListener {
            (activity as MainActivity).loadFragment(CompareFragment())
        }
        btnRandom.setOnClickListener {
            (activity as MainActivity).loadFragment(RandomCountryFragment())
        }
        btnQuiz.setOnClickListener {
            (activity as MainActivity).loadFragment(QuizFragment())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
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
                    favoritesCountText.text = "$favCount"

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
            }
        }
    }
}
