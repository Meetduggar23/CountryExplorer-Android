package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.example.countries.MainActivity
import com.example.countries.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: CountryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyStateText: TextView
    private lateinit var favoritesCount: TextView
    private lateinit var searchEditText: EditText
    private lateinit var scrollUpFab: FloatingActionButton

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

        searchEditText = view.findViewById(R.id.searchEditText)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        favoritesCount = view.findViewById(R.id.favoritesCount)
        scrollUpFab = view.findViewById(R.id.scrollUpFab)
        val clearAllButton = view.findViewById<View>(R.id.clearAllButton)

        val imageCache: MutableMap<String, android.graphics.Bitmap> =
            (activity as? MainActivity)?.imageCache ?: mutableMapOf()
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

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (firstVisible > 3) scrollUpFab.show() else scrollUpFab.hide()
            }
        })

        scrollUpFab.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadFavorites(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        clearAllButton.setOnClickListener {
            val currentFavorites = getFilteredFavorites("")
            if (currentFavorites.isEmpty()) {
                Snackbar.make(view, "No favorites to clear", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Favorites")
                .setMessage("Remove all favorite countries?")
                .setPositiveButton("Clear") { _, _ ->
                    val allFavorites = favoriteManager.getFavoriteCodes()
                    allFavorites.forEach { code -> favoriteManager.removeFavorite(code) }
                    loadFavorites("")
                    Snackbar.make(view, "All favorites cleared", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Success -> {
                        loadFavorites(searchEditText.text.toString())
                    }
                    is CountriesUiState.Error -> {
                        emptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        emptyStateText.text = "Unable to load countries. Pull to refresh."
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites(searchEditText.text.toString())
    }

    private fun getFilteredFavorites(query: String): List<com.example.countries.Country> {
        val state = viewModel.uiState.value
        if (state !is CountriesUiState.Success) return emptyList()

        val favorites = favoriteManager.getFavorites(state.countries)
        return if (query.isBlank()) favorites else {
            val lower = query.lowercase()
            favorites.filter {
                it.commonName.lowercase().contains(lower) ||
                    it.capital.lowercase().contains(lower) ||
                    it.region.lowercase().contains(lower)
            }
        }
    }

    private fun loadFavorites(query: String) {
        val view = view ?: return
        val filtered = getFilteredFavorites(query)

        favoritesCount.text = "${filtered.size} favorite(s)"
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
