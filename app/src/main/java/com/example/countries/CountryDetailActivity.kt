package com.example.countries

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var pinButton: ImageView
    private lateinit var overflowMenuBtn: ImageView
    private lateinit var summaryPopulation: TextView
    private lateinit var summaryArea: TextView
    private lateinit var summaryRegion: TextView
    private lateinit var summaryCapital: TextView
    private lateinit var summaryCurrency: TextView
    private lateinit var summaryContainer: LinearLayout
    private lateinit var neighborsContainer: LinearLayout
    private lateinit var neighborsList: LinearLayout
    private lateinit var scrollView: android.widget.ScrollView

    private lateinit var favoriteManager: FavoriteManager
    private lateinit var pinManager: PinManager
    private lateinit var recentlyViewedManager: RecentlyViewedManager
    private lateinit var viewModel: CountryViewModel
    private var currentCountry: Country? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_detail)

        favoriteManager = FavoriteManager(this)
        pinManager = PinManager(this)
        recentlyViewedManager = RecentlyViewedManager(this)
        viewModel = ViewModelProvider(this)[CountryViewModel::class.java]

        initViews()
        setupButtons()
        applyWindowInsets()

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

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom + 24)
            insets
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
        pinButton = findViewById(R.id.pinButton)
        overflowMenuBtn = findViewById(R.id.overflowMenuBtn)
        summaryPopulation = findViewById(R.id.summaryPopulation)
        summaryArea = findViewById(R.id.summaryArea)
        summaryRegion = findViewById(R.id.summaryRegion)
        summaryCapital = findViewById(R.id.summaryCapital)
        summaryCurrency = findViewById(R.id.summaryCurrency)
        summaryContainer = findViewById(R.id.summaryContainer)
        neighborsContainer = findViewById(R.id.neighborsContainer)
        neighborsList = findViewById(R.id.neighborsList)
        scrollView = findViewById(R.id.scrollView)
    }

    private fun setupButtons() {
        backButton.setOnClickListener { finish() }

        flagImageLarge.setOnClickListener {
            currentCountry?.let { country ->
                if (country.flagUrl.isNotEmpty()) {
                    val intent = Intent(this, FlagViewerActivity::class.java)
                    intent.putExtra("flagUrl", country.flagUrl)
                    intent.putExtra("countryName", country.commonName)
                    startActivity(intent)
                } else {
                    Snackbar.make(flagImageLarge, "No flag available", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        overflowMenuBtn.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        pinButton.setOnClickListener {
            currentCountry?.let { country ->
                if (pinManager.isPinned(country.cca3)) {
                    pinManager.removePin(country.cca3)
                    pinButton.setImageResource(R.drawable.ic_pin)
                    Snackbar.make(pinButton, "Country unpinned", Snackbar.LENGTH_SHORT).show()
                } else {
                    pinManager.addPin(country.cca3)
                    pinButton.setImageResource(R.drawable.ic_pin_filled)
                    Snackbar.make(pinButton, "Country pinned", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        favButton.setOnClickListener {
            currentCountry?.let { country ->
                if (favoriteManager.isFavorite(country.cca3)) {
                    favoriteManager.removeFavorite(country.cca3)
                    favButton.setImageResource(R.drawable.ic_star)
                    Snackbar.make(favButton, "Removed from favorites", Snackbar.LENGTH_SHORT).show()
                } else {
                    favoriteManager.addFavorite(country.cca3)
                    favButton.setImageResource(R.drawable.ic_star_filled)
                    Snackbar.make(favButton, "Added to favorites", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        shareButton.setOnClickListener {
            currentCountry?.let { country ->
                val shareText = buildString {
                    appendLine(country.commonName)
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
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.detail_overflow_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_copy_name -> {
                    copyToClipboard("Country Name", currentCountry?.commonName ?: "")
                    true
                }
                R.id.action_copy_code -> {
                    copyToClipboard("Country Code", currentCountry?.cca2 ?: "")
                    true
                }
                R.id.action_copy_all -> {
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
                        copyToClipboard("Country Info", copyText)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        val message = when (label) {
            "Country Name" -> "Country name copied"
            "Country Code" -> "Country code copied"
            else -> "Country information copied"
        }
        Snackbar.make(overflowMenuBtn, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun displayCountry(country: Country) {
        countryNameText.text = country.commonName
        officialNameText.text = country.officialName
        capitalValue.text = country.capital.ifEmpty { "Not available" }
        regionValue.text = country.region.ifEmpty { "Not available" }
        subregionValue.text = country.subregion.ifEmpty { "Not available" }
        populationValue.text = formatPopulation(country.population)
        areaValue.text = formatArea(country.area)
        code2Value.text = country.cca2.ifEmpty { "Not available" }
        code3Value.text = country.cca3.ifEmpty { "Not available" }
        currenciesValue.text = country.currencies.ifEmpty { "Not available" }
        languagesValue.text = country.languages.ifEmpty { "Not available" }
        timezonesValue.text = country.timezones.joinToString(", ").ifEmpty { "Not available" }
        continentsValue.text = country.continents.joinToString(", ").ifEmpty { "Not available" }
        bordersValue.text = country.borders.joinToString(", ").ifEmpty { "No land borders" }

        summaryPopulation.text = formatPopulation(country.population)
        summaryArea.text = formatArea(country.area)
        summaryRegion.text = country.region.ifEmpty { "Not available" }
        summaryCapital.text = country.capital.ifEmpty { "Not available" }
        summaryCurrency.text = country.currencies.ifEmpty { "Not available" }

        favButton.setImageResource(
            if (favoriteManager.isFavorite(country.cca3)) R.drawable.ic_star_filled
            else R.drawable.ic_star
        )

        pinButton.setImageResource(
            if (pinManager.isPinned(country.cca3)) R.drawable.ic_pin_filled
            else R.drawable.ic_pin
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
