package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.MainActivity
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContinentsFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_continents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        val continentData = mutableListOf<Pair<String, Int>>()
        val adapter = ContinentAdapter(continentData) { continent ->
            val fragment = AllCountriesFragment.newInstance(filterContinent = continent)
            (activity as MainActivity).loadFragment(fragment)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is CountriesUiState.Success -> {
                            val allContinents = listOf("Africa", "Asia", "Europe", "North America", "South America", "Oceania", "Antarctica")
                            continentData.clear()
                            allContinents.forEach { continent ->
                                val count = state.countries.count { country ->
                                    country.continents.any { it.equals(continent, ignoreCase = true) }
                                }
                                if (count > 0) {
                                    continentData.add(Pair(continent, count))
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                        is CountriesUiState.Error -> {
                            continentData.clear()
                            adapter.notifyDataSetChanged()
                        }
                        else -> {}
                    }
                }
            }
    }

    inner class ContinentAdapter(
        private val data: List<Pair<String, Int>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ContinentAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.continentIcon)
            val name: TextView = itemView.findViewById(R.id.continentName)
            val count: TextView = itemView.findViewById(R.id.countryCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_continent, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (name, count) = data[position]
            holder.name.text = name
            holder.count.text = "$count countries"

            val iconRes = when (name.lowercase()) {
                "africa" -> R.drawable.ic_globe
                "asia" -> R.drawable.ic_globe
                "europe" -> R.drawable.ic_globe
                "north america" -> R.drawable.ic_location
                "south america" -> R.drawable.ic_location
                "oceania" -> R.drawable.ic_globe
                "antarctica" -> R.drawable.ic_globe
                else -> R.drawable.ic_globe
            }
            holder.icon.setImageResource(iconRes)

            holder.itemView.setOnClickListener { onClick(name) }
        }

        override fun getItemCount(): Int = data.size
    }
}
