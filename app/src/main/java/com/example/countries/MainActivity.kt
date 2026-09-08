package com.example.countries

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.countries.fragments.*
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var viewModel: CountryViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var bottomNavContainer: LinearLayout
    private lateinit var navIconAllCountries: ImageView
    private lateinit var navIconFavorites: ImageView
    private lateinit var navIconCompare: ImageView
    private lateinit var navIconRandom: ImageView
    private lateinit var navIconQuiz: ImageView
    val imageCache = mutableMapOf<String, Bitmap>()
    private var currentFragmentTag: String = "home"

    // The bottom navigation bar is visible on every page of the app.

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Country Explorer"
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavContainer = findViewById(R.id.bottomNavContainer)

        navIconAllCountries = findViewById(R.id.navIconAllCountries)
        navIconFavorites = findViewById(R.id.navIconFavorites)
        navIconCompare = findViewById(R.id.navIconCompare)
        navIconRandom = findViewById(R.id.navIconRandom)
        navIconQuiz = findViewById(R.id.navIconQuiz)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        toolbar.setNavigationOnClickListener {
            if (currentFragmentTag == "home") {
                drawerLayout.openDrawer(GravityCompat.START)
            } else {
                loadFragment(HomeFragment())
                currentFragmentTag = "home"
                navigationView.setCheckedItem(R.id.nav_home)
                supportActionBar?.title = "Country Explorer"
                updateNavigationIcon()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, insets.bottom)
            windowInsets
        }

        setupBottomNav()

        val themeItem = navigationView.menu.findItem(R.id.nav_theme)
        themeItem?.title = if (ThemeHelper.isDarkTheme(this)) "Dark Theme" else "Light Theme"

        viewModel = ViewModelProvider(this)[CountryViewModel::class.java]

        // Keep toolbar title / nav icon / bottom-nav / search menu in sync
        // when the user presses system back between fragments.
        supportFragmentManager.addOnBackStackChangedListener {
            val top = supportFragmentManager.findFragmentById(R.id.fragmentContainer) ?: return@addOnBackStackChangedListener
            currentFragmentTag = tagFor(top)
            supportActionBar?.title = titleForTag(currentFragmentTag)
            updateNavigationIcon()
            updateBottomNavVisibility()
            updateBottomNavSelection()
            invalidateOptionsMenu()
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            navigationView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun setupBottomNav() {
        bottomNavContainer.findViewById<View>(R.id.navAllCountries).setOnClickListener {
            loadFragment(AllCountriesFragment())
            supportActionBar?.title = "All Countries"
            navigationView.setCheckedItem(R.id.nav_all_countries)
        }

        bottomNavContainer.findViewById<View>(R.id.navFavorites).setOnClickListener {
            loadFragment(FavoritesFragment())
            supportActionBar?.title = "Favorites"
            navigationView.setCheckedItem(R.id.nav_favorites)
        }

        bottomNavContainer.findViewById<View>(R.id.navCompare).setOnClickListener {
            loadFragment(CompareFragment())
            supportActionBar?.title = "Compare"
            navigationView.setCheckedItem(R.id.nav_compare)
        }

        bottomNavContainer.findViewById<View>(R.id.navRandom).setOnClickListener {
            loadFragment(RandomCountryFragment())
            supportActionBar?.title = "Random Country"
            navigationView.setCheckedItem(R.id.nav_random)
        }

        bottomNavContainer.findViewById<View>(R.id.navQuiz).setOnClickListener {
            loadFragment(QuizFragment())
            supportActionBar?.title = "Country Quiz"
            navigationView.setCheckedItem(R.id.nav_quiz)
        }
    }

    private fun updateBottomNavSelection() {
        // Theme-aware colors: dark icon in light theme, light icon in dark
        // theme; selected item is highlighted with the accent color.
        val selectedColor = ContextCompat.getColor(this, R.color.accent_red)
        val unselectedColor = ContextCompat.getColor(this, R.color.bottom_nav_icon)

        navIconAllCountries.setColorFilter(if (currentFragmentTag == "all_countries") selectedColor else unselectedColor)
        navIconFavorites.setColorFilter(if (currentFragmentTag == "favorites") selectedColor else unselectedColor)
        navIconCompare.setColorFilter(if (currentFragmentTag == "compare") selectedColor else unselectedColor)
        navIconRandom.setColorFilter(if (currentFragmentTag == "random") selectedColor else unselectedColor)
        navIconQuiz.setColorFilter(if (currentFragmentTag == "quiz") selectedColor else unselectedColor)
    }

    private fun updateBottomNavVisibility() {
        // Always visible: the bottom bar is shown on every page.
        bottomNavContainer.visibility = View.VISIBLE
    }

    // Search action lives in the toolbar only on the Home page
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        if (currentFragmentTag == "home") {
            menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                openSearch()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSearch() {
        loadFragment(AllCountriesFragment.newInstance(autoFocusSearch = true))
        supportActionBar?.title = "All Countries"
        navigationView.setCheckedItem(R.id.nav_all_countries)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_theme) {
            val newMode = ThemeHelper.toggleTheme(this)
            val title = if (newMode == ThemeHelper.MODE_DARK) "Dark Theme" else "Light Theme"
            item.title = title
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        val fragment: Fragment? = when (item.itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_all_countries -> AllCountriesFragment()
            R.id.nav_favorites -> FavoritesFragment()
            R.id.nav_continents -> ContinentsFragment()
            R.id.nav_regions -> RegionsFragment()
            R.id.nav_languages -> LanguagesFragment()
            R.id.nav_currencies -> CurrenciesFragment()
            R.id.nav_compare -> CompareFragment()
            R.id.nav_random -> RandomCountryFragment()
            R.id.nav_quiz -> QuizFragment()
            R.id.nav_rankings -> RankingsFragment()
            R.id.nav_recently -> RecentlyViewedFragment()
            R.id.nav_help -> HelpFragment()
            R.id.nav_about -> AboutFragment()
            else -> null
        }

        fragment?.let {
            loadFragment(it)
            supportActionBar?.title = item.title
            updateNavigationIcon()
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun updateNavigationIcon() {
        if (currentFragmentTag == "home") {
            toggle.isDrawerIndicatorEnabled = true
            toggle.syncState()
        } else {
            toggle.isDrawerIndicatorEnabled = false
            toolbar.setNavigationIcon(R.drawable.ic_back)
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()

        currentFragmentTag = tagFor(fragment)
        updateNavigationIcon()
        updateBottomNavVisibility()
        updateBottomNavSelection()
        // Show/hide the Home search icon when the page changes
        invalidateOptionsMenu()
    }

    private fun tagFor(fragment: Fragment): String = when (fragment) {
        is HomeFragment -> "home"
        is AllCountriesFragment -> "all_countries"
        is FavoritesFragment -> "favorites"
        is CompareFragment -> "compare"
        is RandomCountryFragment -> "random"
        is QuizFragment -> "quiz"
        is RankingsFragment -> "rankings"
        is RecentlyViewedFragment -> "recently"
        is ContinentsFragment -> "continents"
        is RegionsFragment -> "regions"
        is LanguagesFragment -> "languages"
        is CurrenciesFragment -> "currencies"
        is HelpFragment -> "help"
        is AboutFragment -> "about"
        else -> "other"
    }

    private fun titleForTag(tag: String): String = when (tag) {
        "all_countries" -> "All Countries"
        "favorites" -> "Favorites"
        "compare" -> "Compare"
        "random" -> "Random Country"
        "quiz" -> "Country Quiz"
        "rankings" -> "Rankings"
        "recently" -> "Recently Viewed"
        "continents" -> "Continents"
        "regions" -> "Regions"
        "languages" -> "Languages"
        "currencies" -> "Currencies"
        "help" -> "Help"
        "about" -> "About"
        else -> "Country Explorer"
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
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
                        } else null
                    } finally {
                        connection.disconnect()
                    }
                } catch (_: Exception) { null }
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
