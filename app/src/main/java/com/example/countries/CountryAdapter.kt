package com.example.countries

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CountryAdapter(
    private var countries: List<Country>,
    private val imageCache: MutableMap<String, Bitmap>,
    private val onClick: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    fun updateData(newCountries: List<Country>) {
        countries = newCountries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(countries[position])
    }

    override fun getItemCount(): Int = countries.size

    inner class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flagImage: ImageView = itemView.findViewById(R.id.flagImage)
        private val countryName: TextView = itemView.findViewById(R.id.countryName)
        private val capitalText: TextView = itemView.findViewById(R.id.capitalText)
        private val regionText: TextView = itemView.findViewById(R.id.regionText)
        private val populationText: TextView = itemView.findViewById(R.id.populationText)
        private val codeBadge: TextView = itemView.findViewById(R.id.codeBadge)
        private val chevronIcon: ImageView = itemView.findViewById(R.id.chevronIcon)

        fun bind(country: Country) {
            countryName.text = country.commonName
            capitalText.text = "Capital: ${country.capital}"
            regionText.text = country.region
            populationText.text = formatPopulation(country.population)
            codeBadge.text = country.cca2

            if (country.flagUrl.isNotEmpty()) {
                flagImage.tag = country.flagUrl
                imageCache[country.flagUrl]?.let {
                    flagImage.setImageBitmap(it)
                } ?: run {
                    flagImage.setImageResource(0)
                    loadFlagImage(country.flagUrl)
                }
            } else {
                flagImage.setImageResource(R.drawable.bg_placeholder_flag)
            }

            itemView.setOnClickListener { onClick(country) }
        }

        private fun loadFlagImage(url: String) {
            val activity = itemView.context as? MainActivity ?: return
            activity.loadImage(url, flagImage)
        }

        private fun formatPopulation(population: Long): String {
            return when {
                population >= 1_000_000_000 -> String.format("%.1fB", population / 1_000_000_000.0)
                population >= 1_000_000 -> String.format("%.1fM", population / 1_000_000.0)
                population >= 1_000 -> String.format("%.1fK", population / 1_000.0)
                else -> population.toString()
            }
        }
    }
}
