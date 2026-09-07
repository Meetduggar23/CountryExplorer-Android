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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countries.Country
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RankingsFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RankingAdapter
    private var allCountries = listOf<Country>()
    private var currentMetric = "population"
    private var currentOrder = "highest"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rankings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        val btnPopulation = view.findViewById<Button>(R.id.btnPopulation)
        val btnArea = view.findViewById<Button>(R.id.btnArea)
        val btnHighest = view.findViewById<Button>(R.id.btnHighest)
        val btnLowest = view.findViewById<Button>(R.id.btnLowest)
        recyclerView = view.findViewById(R.id.rankingsList)

        adapter = RankingAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fun updateMainButtons() {
            if (currentMetric == "population") {
                btnPopulation.backgroundTintList = resources.getColorStateList(R.color.accent_red, null)
                btnPopulation.setTextColor(resources.getColor(R.color.text_white, null))
                btnArea.setBackgroundColor(0)
                btnArea.setBackgroundResource(R.drawable.bg_search)
                btnArea.setTextColor(resources.getColor(R.color.text_light_gray, null))
            } else {
                btnArea.backgroundTintList = resources.getColorStateList(R.color.accent_red, null)
                btnArea.setTextColor(resources.getColor(R.color.text_white, null))
                btnPopulation.setBackgroundColor(0)
                btnPopulation.setBackgroundResource(R.drawable.bg_search)
                btnPopulation.setTextColor(resources.getColor(R.color.text_light_gray, null))
            }
        }

        fun updateSubButtons() {
            if (currentOrder == "highest") {
                btnHighest.backgroundTintList = resources.getColorStateList(R.color.accent_yellow, null)
                btnHighest.setTextColor(resources.getColor(R.color.primary_dark_navy, null))
                btnLowest.setBackgroundColor(0)
                btnLowest.setBackgroundResource(R.drawable.bg_search)
                btnLowest.setTextColor(resources.getColor(R.color.text_light_gray, null))
            } else {
                btnLowest.backgroundTintList = resources.getColorStateList(R.color.accent_yellow, null)
                btnLowest.setTextColor(resources.getColor(R.color.primary_dark_navy, null))
                btnHighest.setBackgroundColor(0)
                btnHighest.setBackgroundResource(R.drawable.bg_search)
                btnHighest.setTextColor(resources.getColor(R.color.text_light_gray, null))
            }
        }

        btnPopulation.setOnClickListener {
            currentMetric = "population"
            updateMainButtons()
            updateRankings()
        }

        btnArea.setOnClickListener {
            currentMetric = "area"
            updateMainButtons()
            updateRankings()
        }

        btnHighest.setOnClickListener {
            currentOrder = "highest"
            updateSubButtons()
            updateRankings()
        }

        btnLowest.setOnClickListener {
            currentOrder = "lowest"
            updateSubButtons()
            updateRankings()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
                    allCountries = state.countries
                    updateRankings()
                }
            }
        }
    }

    private fun updateRankings() {
        val sorted = when (currentMetric) {
            "population" -> {
                if (currentOrder == "highest") allCountries.sortedByDescending { it.population }
                else allCountries.sortedBy { it.population }
            }
            "area" -> {
                if (currentOrder == "highest") allCountries.sortedByDescending { it.area }
                else allCountries.sortedBy { it.area }
            }
            else -> allCountries
        }
        adapter.updateData(sorted, currentMetric)
    }

    inner class RankingAdapter : RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

        private var countries = listOf<Country>()
        private var metric = "population"

        fun updateData(newCountries: List<Country>, newMetric: String) {
            countries = newCountries
            metric = newMetric
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rankText: TextView = itemView.findViewById(R.id.rankNumber)
            val flagImage: ImageView = itemView.findViewById(R.id.flagImage)
            val countryName: TextView = itemView.findViewById(R.id.countryName)
            val valueText: TextView = itemView.findViewById(R.id.valueText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ranking, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val country = countries[position]
            holder.rankText.text = "#${position + 1}"
            holder.countryName.text = country.commonName

            val value = when (metric) {
                "population" -> formatNumber(country.population)
                "area" -> String.format("%,.2f km\u00B2", country.area)
                else -> ""
            }
            holder.valueText.text = value

            if (country.flagUrl.isNotEmpty()) {
                holder.flagImage.tag = country.flagUrl
                val activity = holder.itemView.context as? com.example.countries.MainActivity
                activity?.loadImage(country.flagUrl, holder.flagImage)
            }
        }

        override fun getItemCount(): Int = countries.size

        private fun formatNumber(num: Long): String {
            return String.format("%,d", num)
        }
    }
}
