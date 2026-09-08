package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.example.countries.RecentlyViewedManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecentlyViewedFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var recentlyViewedManager: RecentlyViewedManager
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: CountryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyStateText: TextView
    private lateinit var scrollUpFab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recently_viewed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]
        recentlyViewedManager = RecentlyViewedManager(requireContext())
        favoriteManager = FavoriteManager(requireContext())

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        scrollUpFab = view.findViewById(R.id.scrollUpFab)
        val clearButton = view.findViewById<TextView>(R.id.clearButton)

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

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) scrollUpFab.show() else if (dy < 0) scrollUpFab.show()
            }
        })

        scrollUpFab.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        clearButton.setOnClickListener {
            val state = viewModel.uiState.value
            if (state !is CountriesUiState.Success) return@setOnClickListener
            val recent = recentlyViewedManager.getRecentlyViewed(state.countries)
            if (recent.isEmpty()) {
                Snackbar.make(view, "No history to clear", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Clear History")
                .setMessage("Clear all recently viewed countries?")
                .setPositiveButton("Clear") { _, _ ->
                    recentlyViewedManager.clear()
                    adapter.updateData(emptyList())
                    emptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    Snackbar.make(view, "Recently viewed cleared", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Success -> {
                        val recent = recentlyViewedManager.getRecentlyViewed(state.countries)
                        if (recent.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            clearButton.visibility = View.GONE
                        } else {
                            emptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            clearButton.visibility = View.VISIBLE
                            adapter.updateData(recent)
                        }
                    }
                    is CountriesUiState.Error -> {
                        emptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    else -> {}
                }
            }
        }
    }
}
