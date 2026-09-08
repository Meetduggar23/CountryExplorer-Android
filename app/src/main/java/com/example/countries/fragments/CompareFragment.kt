package com.example.countries.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.countries.Country
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CompareFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private var allCountries = listOf<Country>()
    private lateinit var spinner1: Spinner
    private lateinit var spinner2: Spinner
    private lateinit var compareContainer: LinearLayout
    private lateinit var btnSwap: View
    private val imageCache = mutableMapOf<String, Bitmap>()

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
        compareContainer = view.findViewById(R.id.compareContainer)
        btnSwap = view.findViewById(R.id.swapButton)

        btnSwap.setOnClickListener {
            val pos1 = spinner1.selectedItemPosition
            val pos2 = spinner2.selectedItemPosition
            spinner1.setSelection(pos2)
            spinner2.setSelection(pos1)
        }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                updateComparison()
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
                        compareContainer.removeAllViews()
                        val tv = TextView(requireContext()).apply {
                            text = "Unable to load country data"
                            textSize = 14f
                            setTextColor(resources.getColor(R.color.text_muted, null))
                            gravity = android.view.Gravity.CENTER
                            setPadding(0, 32, 0, 0)
                        }
                        compareContainer.addView(tv)
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
        if (allCountries.size > 1) {
            spinner2.setSelection(1)
        }
    }

    private fun updateComparison() {
        compareContainer.removeAllViews()
        if (allCountries.isEmpty()) return
        if (spinner1.selectedItemPosition < 0 || spinner2.selectedItemPosition < 0) return

        val country1 = allCountries[spinner1.selectedItemPosition]
        val country2 = allCountries[spinner2.selectedItemPosition]

        fun addRow(label: String, val1: String, val2: String) {
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp4, 0, dp4)
                dividerDrawable = null
            }
            val tv1 = TextView(requireContext()).apply {
                text = val1
                textSize = 13f
                setTextColor(resources.getColor(R.color.text_white, null))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val tvLabel = TextView(requireContext()).apply {
                text = label
                textSize = 11f
                setTextColor(resources.getColor(R.color.accent_yellow, null))
                gravity = android.view.Gravity.CENTER
                setPadding(dp8, 0, dp8, 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                maxLines = 1
            }
            val tv2 = TextView(requireContext()).apply {
                text = val2
                textSize = 13f
                setTextColor(resources.getColor(R.color.text_white, null))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            row.addView(tv1)
            row.addView(tvLabel)
            row.addView(tv2)
            compareContainer.addView(row)
        }

        addRow("Name", country1.commonName, country2.commonName)
        addRow("Capital", country1.capital, country2.capital)
        addRow("Region", country1.region, country2.region)
        addRow("Population", formatNumber(country1.population), formatNumber(country2.population))
        addRow("Area", String.format("%,.2f km²", country1.area), String.format("%,.2f km²", country2.area))
        addRow("Languages", "${country1.languages.split(", ").size}", "${country2.languages.split(", ").size}")
        addRow("Borders", "${country1.borders.size}", "${country2.borders.size}")
        addRow("Currencies", country1.currencies, country2.currencies)
    }

    private fun formatNumber(num: Long): String {
        return String.format("%,d", num)
    }
}
