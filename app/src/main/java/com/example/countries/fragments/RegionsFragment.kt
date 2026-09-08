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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.MainActivity
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegionsFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_regions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        val regionData = mutableListOf<Pair<String, Int>>()
        val adapter = RegionAdapter(regionData) { region ->
            val fragment = AllCountriesFragment.newInstance(filterRegion = region)
            (activity as MainActivity).loadFragment(fragment)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is CountriesUiState.Success -> {
                            val regions = listOf("Africa", "Americas", "Asia", "Europe", "Oceania")
                            regionData.clear()
                            regions.forEach { region ->
                                val count = state.countries.count {
                                    it.region.equals(region, ignoreCase = true)
                                }
                                regionData.add(Pair(region, count))
                            }
                            adapter.notifyDataSetChanged()
                        }
                        is CountriesUiState.Error -> {
                            regionData.clear()
                            adapter.notifyDataSetChanged()
                        }
                        else -> {}
                    }
                }
            }
    }

    inner class RegionAdapter(
        private val data: List<Pair<String, Int>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<RegionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.regionIcon)
            val name: TextView = itemView.findViewById(R.id.regionName)
            val count: TextView = itemView.findViewById(R.id.countryCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_region, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (name, count) = data[position]
            holder.name.text = name
            holder.count.text = "$count countries"
            holder.icon.setImageResource(R.drawable.ic_globe)
            holder.itemView.setOnClickListener { onClick(name) }
        }

        override fun getItemCount(): Int = data.size
    }
}
