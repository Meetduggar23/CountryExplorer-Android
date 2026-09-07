package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class CurrenciesFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_currencies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)

        val currencyData = mutableListOf<Triple<String, String, Int>>()
        val adapter = CurrencyAdapter(currencyData) { currencyName ->
            val fragment = AllCountriesFragment.newInstance(filterCurrency = currencyName)
            (activity as MainActivity).loadFragment(fragment)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state is CountriesUiState.Success) {
                    val currencyMap = mutableMapOf<String, MutableSet<String>>()
                    state.countries.forEach { country ->
                        country.currencies.split(", ").forEach { curr ->
                            if (curr.isNotBlank() && curr != "Not available") {
                                currencyMap.getOrPut(curr) { mutableSetOf() }.add(country.commonName)
                            }
                        }
                    }
                    currencyData.clear()
                    currencyMap.entries.sortedBy { it.key }.forEach { (curr, countries) ->
                        currencyData.add(Triple(curr, "", countries.size))
                    }
                    adapter.notifyDataSetChanged()

                    if (currencyData.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    inner class CurrencyAdapter(
        private val data: List<Triple<String, String, Int>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.currencyName)
            val code: TextView = itemView.findViewById(R.id.currencyCode)
            val count: TextView = itemView.findViewById(R.id.countryCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_currency, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (name, code, count) = data[position]
            holder.name.text = name
            holder.code.text = code.ifEmpty { "" }
            holder.count.text = "$count countries"
            holder.itemView.setOnClickListener { onClick(name) }
        }

        override fun getItemCount(): Int = data.size
    }
}
