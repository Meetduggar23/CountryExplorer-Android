package com.example.countries

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class CountryDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var flagImageLarge: ImageView
    private lateinit var countryNameText: TextView
    private lateinit var officialNameText: TextView
    private lateinit var loadingDetail: ProgressBar
    private lateinit var capitalValue: TextView
    private lateinit var regionValue: TextView
    private lateinit var subregionValue: TextView
    private lateinit var populationValue: TextView
    private lateinit var areaValue: TextView
    private lateinit var code2Value: TextView
    private lateinit var code3Value: TextView
    private lateinit var currenciesValue: TextView
    private lateinit var languagesValue: TextView
    private lateinit var timezonesValue: TextView
    private lateinit var continentsValue: TextView
    private lateinit var bordersValue: TextView
    private lateinit var favButton: ImageView
    private lateinit var shareButton: ImageView
    private lateinit var copyButton: ImageView
    private lateinit var neighborsContainer: LinearLayout
    private lateinit var neighborsList: LinearLayout

    private lateinit var favoriteManager: FavoriteManager
    private lateinit var recentlyViewedManager: RecentlyViewedManager
    private var currentCountry: Country? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_detail)

        favoriteManager = FavoriteManager(this)
        recentlyViewedManager = RecentlyViewedManager(this)

        initViews()
        setupButtons()

        val country = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("country", Country::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("country") as? Country
        }

        if (country != null) {
            currentCountry = country
            recentlyViewedManager.addCountry(country.cca3)
            displayCountry(country)
            loadNeighbors(country)
        } else {
            finish()
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        flagImageLarge = findViewById(R.id.flagImageLarge)
        countryNameText = findViewById(R.id.countryNameText)
        officialNameText = findViewById(R.id.officialNameText)
        loadingDetail = findViewById(R.id.loadingDetail)
        capitalValue = findViewById(R.id.capitalValue)
        regionValue = findViewById(R.id.regionValue)
        subregionValue = findViewById(R.id.subregionValue)
        populationValue = findViewById(R.id.populationValue)
        areaValue = findViewById(R.id.areaValue)
        code2Value = findViewById(R.id.code2Value)
        code3Value = findViewById(R.id.code3Value)
        currenciesValue = findViewById(R.id.currenciesValue)
        languagesValue = findViewById(R.id.languagesValue)
        timezonesValue = findViewById(R.id.timezonesValue)
        continentsValue = findViewById(R.id.continentsValue)
        bordersValue = findViewById(R.id.bordersValue)
        favButton = findViewById(R.id.favButton)
        shareButton = findViewById(R.id.shareButton)
        copyButton = findViewById(R.id.copyButton)
        neighborsContainer = findViewById(R.id.neighborsContainer)
        neighborsList = findViewById(R.id.neighborsList)
    }

    private fun setupButtons() {
        backButton.setOnClickListener { finish() }

        favButton.setOnClickListener {
            currentCountry?.let { country ->
                if (favoriteManager.isFavorite(country.cca3)) {
                    favoriteManager.removeFavorite(country.cca3)
                    favButton.setImageResource(R.drawable.ic_star)
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                } else {
                    favoriteManager.addFavorite(country.cca3)
                    favButton.setImageResource(R.drawable.ic_star_filled)
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                }
            }
        }

        shareButton.setOnClickListener {
            currentCountry?.let { country ->
                val shareText = buildString {
                    appendLine("${country.commonName}")
                    appendLine("Official: ${country.officialName}")
                    appendLine("Capital: ${country.capital}")
                    appendLine("Population: ${formatPopulation(country.population)}")
                    appendLine("Region: ${country.region}")
                    appendLine("Currency: ${country.currencies}")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Check out ${country.commonName}")
                }
                startActivity(Intent.createChooser(intent, "Share Country"))
            }
        }

        copyButton.setOnClickListener {
            currentCountry?.let { country ->
                val copyText = buildString {
                    appendLine("Country: ${country.commonName}")
                    appendLine("Official: ${country.officialName}")
                    appendLine("Capital: ${country.capital}")
                    appendLine("Population: ${formatPopulation(country.population)}")
                    appendLine("Area: ${formatArea(country.area)}")
                    appendLine("Region: ${country.region}")
                    appendLine("Subregion: ${country.subregion}")
                    appendLine("Languages: ${country.languages}")
                    appendLine("Currency: ${country.currencies}")
                }
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Country Info", copyText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Country information copied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayCountry(country: Country) {
        countryNameText.text = country.commonName
        officialNameText.text = country.officialName
        capitalValue.text = country.capital
        regionValue.text = country.region
        subregionValue.text = country.subregion
        populationValue.text = formatPopulation(country.population)
        areaValue.text = formatArea(country.area)
        code2Value.text = country.cca2
        code3Value.text = country.cca3
        currenciesValue.text = country.currencies
        languagesValue.text = country.languages
        timezonesValue.text = country.timezones.joinToString(", ").ifEmpty { "Not available" }
        continentsValue.text = country.continents.joinToString(", ").ifEmpty { "Not available" }
        bordersValue.text = country.borders.joinToString(", ").ifEmpty { "No land borders" }

        favButton.setImageResource(
            if (favoriteManager.isFavorite(country.cca3)) R.drawable.ic_star_filled
            else R.drawable.ic_star
        )

        if (country.flagUrl.isNotEmpty()) {
            loadingDetail.visibility = View.VISIBLE
            flagImageLarge.tag = country.flagUrl
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val connection = URL(country.flagUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        try {
                            connection.connect()
                            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                                BitmapFactory.decodeStream(connection.inputStream)
                            } else null
                        } finally {
                            connection.disconnect()
                        }
                    } catch (_: Exception) { null }
                }
                loadingDetail.visibility = View.GONE
                if (bitmap != null && flagImageLarge.tag == country.flagUrl) {
                    flagImageLarge.setImageBitmap(bitmap)
                } else {
                    flagImageLarge.setImageResource(R.drawable.bg_placeholder_flag)
                }
            }
        } else {
            flagImageLarge.setImageResource(R.drawable.bg_placeholder_flag)
        }
    }

    private fun loadNeighbors(country: Country) {
        if (country.borders.isEmpty()) {
            neighborsContainer.visibility = View.GONE
            return
        }

        val viewModel = ViewModelProvider(this)[CountryViewModel::class.java]
        val neighbors = viewModel.getNeighbors(country)

        if (neighbors.isEmpty()) {
            neighborsContainer.visibility = View.GONE
            return
        }

        neighborsContainer.visibility = View.VISIBLE
        neighborsList.removeAllViews()

        for (neighbor in neighbors) {
            val itemView = layoutInflater.inflate(R.layout.item_neighbor, neighborsList, false)
            val nameText = itemView.findViewById<TextView>(R.id.neighborName)
            val codeText = itemView.findViewById<TextView>(R.id.neighborCode)
            nameText.text = neighbor.commonName
            codeText.text = neighbor.cca2
            itemView.setOnClickListener {
                val intent = Intent(this, CountryDetailActivity::class.java)
                intent.putExtra("country", neighbor)
                startActivity(intent)
            }
            neighborsList.addView(itemView)
        }
    }

    private fun formatPopulation(population: Long): String {
        return when {
            population >= 1_000_000_000 -> String.format("%.1f billion", population / 1_000_000_000.0)
            population >= 1_000_000 -> String.format("%.1f million", population / 1_000_000.0)
            population >= 1_000 -> String.format("%,d", population)
            else -> population.toString()
        }
    }

    private fun formatArea(area: Double): String {
        return if (area > 0) String.format("%,.2f km\u00B2", area) else "Not available"
    }
}
