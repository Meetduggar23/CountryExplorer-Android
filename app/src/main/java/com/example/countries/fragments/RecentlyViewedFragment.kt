package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.example.countries.RecentlyViewedManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecentlyViewedFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var recentlyViewedManager: RecentlyViewedManager
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: CountryAdapter

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

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val clearButton = view.findViewById<Button>(R.id.clearButton)

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

        clearButton.setOnClickListener {
            recentlyViewedManager.clear()
            adapter.updateData(emptyList())
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            clearButton.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
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
            }
        }
    }
}
