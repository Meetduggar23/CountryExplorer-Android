package com.example.countries

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CountryViewModel
    private lateinit var adapter: CountryAdapter

    private lateinit var appLogo: ImageView
    private lateinit var appTitle: TextView
    private lateinit var appSubtitle: TextView
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageView
    private lateinit var refreshButton: ImageView
    private lateinit var countryCountText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var loadingText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: LinearLayout
    private lateinit var errorIcon: ImageView
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button

    private val imageCache = mutableMapOf<String, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupRefreshButton()
        observeState()
    }

    private fun initViews() {
        appLogo = findViewById(R.id.appLogo)
        appTitle = findViewById(R.id.appTitle)
        appSubtitle = findViewById(R.id.appSubtitle)
        searchContainer = findViewById(R.id.searchContainer)
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)
        refreshButton = findViewById(R.id.refreshButton)
        countryCountText = findViewById(R.id.countryCountText)
        recyclerView = findViewById(R.id.recyclerView)
        loadingContainer = findViewById(R.id.loadingContainer)
        loadingText = findViewById(R.id.loadingText)
        progressBar = findViewById(R.id.progressBar)
        errorContainer = findViewById(R.id.errorContainer)
        errorIcon = findViewById(R.id.errorIcon)
        errorText = findViewById(R.id.errorText)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun setupViewModel() {
        viewModel = androidx.lifecycle.ViewModelProvider(this)[CountryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CountryAdapter(emptyList(), imageCache) { country ->
            val intent = android.content.Intent(this, CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.search(query)
                clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            viewModel.search("")
        }
    }

    private fun setupRefreshButton() {
        refreshButton.setOnClickListener {
            searchEditText.text.clear()
            viewModel.refresh()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Loading -> showLoading()
                    is CountriesUiState.Success -> showSuccess(state)
                    is CountriesUiState.Error -> showError(state.message)
                }
            }
        }
    }

    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        errorContainer.visibility = View.GONE
        searchContainer.visibility = View.GONE
        countryCountText.visibility = View.GONE
        refreshButton.visibility = View.GONE
        appLogo.visibility = View.VISIBLE
        appTitle.visibility = View.VISIBLE
        appSubtitle.visibility = View.VISIBLE
        loadingText.text = "Loading countries..."
    }

    private fun showSuccess(state: CountriesUiState.Success) {
        loadingContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        searchContainer.visibility = View.VISIBLE
        countryCountText.visibility = View.VISIBLE
        refreshButton.visibility = View.VISIBLE
        appLogo.visibility = View.GONE
        appTitle.visibility = View.GONE
        appSubtitle.visibility = View.GONE

        val displayedCount = state.countries.size
        val totalCount = state.totalCount
        countryCountText.text = if (state.searchQuery.isNotBlank()) {
            "$displayedCount of $totalCount countries"
        } else {
            "$totalCount countries"
        }

        adapter.updateData(state.countries)
    }

    private fun showError(message: String) {
        loadingContainer.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        searchContainer.visibility = View.GONE
        countryCountText.visibility = View.GONE
        refreshButton.visibility = View.GONE
        appLogo.visibility = View.VISIBLE
        appTitle.visibility = View.VISIBLE
        appSubtitle.visibility = View.VISIBLE

        errorText.text = message

        retryButton.setOnClickListener {
            viewModel.refresh()
        }
    }

    fun loadImage(url: String, imageView: ImageView) {
        if (url.isBlank()) return
        imageCache[url]?.let {
            imageView.setImageBitmap(it)
            return
        }

        imageView.tag = url
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    try {
                        connection.connect()
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            BitmapFactory.decodeStream(connection.inputStream)
                        } else {
                            null
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                imageCache[url] = bitmap
                if (imageView.tag == url) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
}
