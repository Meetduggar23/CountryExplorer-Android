package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
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
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: CountryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        favoriteManager = FavoriteManager(requireContext())

        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val favoritesCount = view.findViewById<TextView>(R.id.favoritesCount)

        val imageCache = mutableMapOf<String, android.graphics.Bitmap>()
        adapter = CountryAdapter(emptyList(), imageCache, { country ->
            val intent = android.content.Intent(requireContext(), CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }, { country ->
            favoriteManager.removeFavorite(country.cca3)
            loadFavorites(searchEditText.text.toString())
        })
        adapter.setFavoriteManager(favoriteManager)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadFavorites(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
                    loadFavorites(searchEditText.text.toString())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val searchEditText = view?.findViewById<EditText>(R.id.searchEditText)
        loadFavorites(searchEditText?.text?.toString() ?: "")
    }

    private fun loadFavorites(query: String) {
        val view = view ?: return
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val favoritesCount = view.findViewById<TextView>(R.id.favoritesCount)

        val state = viewModel.uiState.value
        if (state !is CountriesUiState.Success) return

        val favorites = favoriteManager.getFavorites(state.countries)
        val filtered = if (query.isBlank()) favorites else {
            val lower = query.lowercase()
            favorites.filter {
                it.commonName.lowercase().contains(lower) ||
                    it.capital.lowercase().contains(lower) ||
                    it.region.lowercase().contains(lower)
            }
        }

        favoritesCount.text = "${favorites.size} favorite(s)"
        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
