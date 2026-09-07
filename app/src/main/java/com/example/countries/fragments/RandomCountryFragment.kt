package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.countries.Country
import com.example.countries.CountryDetailActivity
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.FavoriteManager
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RandomCountryFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private var allCountries = listOf<Country>()
    private var currentCountry: Country? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_random_country, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        favoriteManager = FavoriteManager(requireContext())

        val flagImage = view.findViewById<ImageView>(R.id.flagImageView)
        val countryName = view.findViewById<TextView>(R.id.countryNameText)
        val capitalText = view.findViewById<TextView>(R.id.capitalText)
        val regionText = view.findViewById<TextView>(R.id.regionBadge)
        val populationText = view.findViewById<TextView>(R.id.populationText)
        val btnRandom = view.findViewById<Button>(R.id.btnRandom)
        val btnViewDetails = view.findViewById<Button>(R.id.btnDetails)
        val btnFavorite = view.findViewById<Button>(R.id.btnFavAdd)

        btnRandom.setOnClickListener {
            if (allCountries.isNotEmpty()) {
                currentCountry = allCountries.random()
                displayCountry(view)
            }
        }

        btnViewDetails.setOnClickListener {
            currentCountry?.let { country ->
                val intent = android.content.Intent(requireContext(), CountryDetailActivity::class.java)
                intent.putExtra("country", country)
                startActivity(intent)
            }
        }

        btnFavorite.setOnClickListener {
            currentCountry?.let { country ->
                if (favoriteManager.isFavorite(country.cca3)) {
                    favoriteManager.removeFavorite(country.cca3)
                    btnFavorite.text = "Add to Favorites"
                } else {
                    favoriteManager.addFavorite(country.cca3)
                    btnFavorite.text = "Remove from Favorites"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
                    allCountries = state.countries
                    if (currentCountry == null && allCountries.isNotEmpty()) {
                        currentCountry = allCountries.random()
                        displayCountry(view)
                    }
                }
            }
        }
    }

    private fun displayCountry(view: View) {
        val country = currentCountry ?: return
        view.findViewById<TextView>(R.id.countryNameText)?.text = country.commonName
        view.findViewById<TextView>(R.id.capitalText)?.text = "Capital: ${country.capital}"
        view.findViewById<TextView>(R.id.regionBadge)?.text = "Region: ${country.region}"
        view.findViewById<TextView>(R.id.populationText)?.text = "Population: ${formatNumber(country.population)}"

        val btnFavorite = view.findViewById<Button>(R.id.btnFavAdd)
        btnFavorite.text = if (favoriteManager.isFavorite(country.cca3)) {
            "Remove from Favorites"
        } else {
            "Add to Favorites"
        }
    }

    private fun formatNumber(num: Long): String {
        return String.format("%,d", num)
    }
}
