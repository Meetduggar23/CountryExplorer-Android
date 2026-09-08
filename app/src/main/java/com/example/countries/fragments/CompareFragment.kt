package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.countries.Country
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.MainActivity
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CompareFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private var allCountries = listOf<Country>()
    private lateinit var spinner1: Spinner
    private lateinit var spinner2: Spinner
    private lateinit var swapButton: View
    private lateinit var compareButton: View
    private lateinit var compareContainer: View
    private lateinit var compareEmptyPrompt: View
    private lateinit var comparePromptText: TextView
    private lateinit var flag1: ImageView
    private lateinit var flag2: ImageView
    private lateinit var name1: TextView
    private lateinit var name2: TextView
    private lateinit var capital1: TextView
    private lateinit var capital2: TextView
    private lateinit var population1: TextView
    private lateinit var population2: TextView
    private lateinit var area1: TextView
    private lateinit var area2: TextView
    private lateinit var region1: TextView
    private lateinit var region2: TextView

    private var selectionInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_compare, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        spinner1 = view.findViewById(R.id.countrySpinner1)
        spinner2 = view.findViewById(R.id.countrySpinner2)
        swapButton = view.findViewById(R.id.swapButton)
        compareButton = view.findViewById(R.id.compareButton)
        compareContainer = view.findViewById(R.id.compareContainer)
        compareEmptyPrompt = view.findViewById(R.id.compareEmptyPrompt)
        comparePromptText = view.findViewById(R.id.comparePromptText)
        flag1 = view.findViewById(R.id.flag1)
        flag2 = view.findViewById(R.id.flag2)
        name1 = view.findViewById(R.id.name1)
        name2 = view.findViewById(R.id.name2)
        capital1 = view.findViewById(R.id.capital1)
        capital2 = view.findViewById(R.id.capital2)
        population1 = view.findViewById(R.id.population1)
        population2 = view.findViewById(R.id.population2)
        area1 = view.findViewById(R.id.area1)
        area2 = view.findViewById(R.id.area2)
        region1 = view.findViewById(R.id.region1)
        region2 = view.findViewById(R.id.region2)

        swapButton.setOnClickListener {
            val pos1 = spinner1.selectedItemPosition
            val pos2 = spinner2.selectedItemPosition
            if (pos1 >= 0 && pos2 >= 0) {
                spinner1.setSelection(pos2)
                spinner2.setSelection(pos1)
            }
        }

        compareButton.setOnClickListener { renderComparison() }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (!selectionInitialized) return
                renderComparison()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner1.onItemSelectedListener = listener
        spinner2.onItemSelectedListener = listener

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Success -> {
                        allCountries = state.countries.sortedBy { it.commonName }
                        setupSpinners()
                    }
                    is CountriesUiState.Error -> {
                        compareContainer.visibility = View.GONE
                        compareEmptyPrompt.visibility = View.VISIBLE
                        comparePromptText.text = "Unable to load country data"
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupSpinners() {
        val names = allCountries.map { it.commonName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = adapter
        spinner2.adapter = adapter
        if (allCountries.size > 1 && !selectionInitialized) {
            spinner2.setSelection(1)
        }
        selectionInitialized = true
        renderComparison()
    }

    private fun renderComparison() {
        if (allCountries.isEmpty()) return
        val pos1 = spinner1.selectedItemPosition
        val pos2 = spinner2.selectedItemPosition
        if (pos1 < 0 || pos2 < 0) return

        val country1 = allCountries[pos1]
        val country2 = allCountries[pos2]

        compareEmptyPrompt.visibility = View.GONE
        compareContainer.visibility = View.VISIBLE

        bindCountryCard(flag1, name1, capital1, population1, area1, region1, country1)
        bindCountryCard(flag2, name2, capital2, population2, area2, region2, country2)
    }

    private fun bindCountryCard(
        flag: ImageView,
        name: TextView,
        capital: TextView,
        population: TextView,
        area: TextView,
        region: TextView,
        country: Country
    ) {
        name.text = country.commonName
        capital.text = country.capital
        population.text = formatNumber(country.population)
        area.text = String.format("%,.2f km\u00B2", country.area)
        region.text = country.region

        if (country.flagUrl.isNotEmpty()) {
            (activity as? MainActivity)?.loadImage(country.flagUrl, flag)
        }
    }

    private fun formatNumber(num: Long): String {
        return String.format("%,d", num)
    }
}
