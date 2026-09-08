package com.example.countries

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
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

    private val bottomNavFragments = setOf("home", "all_countries", "favorites", "compare", "random", "quiz")

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

        setupBottomNav()

        val themeItem = navigationView.menu.findItem(R.id.nav_theme)
        themeItem?.title = if (ThemeHelper.isDarkTheme(this)) "Dark Theme" else "Light Theme"

        viewModel = ViewModelProvider(this)[CountryViewModel::class.java]

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
        val selectedColor = resources.getColor(R.color.accent_red, null)
        val unselectedColor = resources.getColor(R.color.text_muted, null)

        navIconAllCountries.setColorFilter(if (currentFragmentTag == "all_countries") selectedColor else unselectedColor)
        navIconFavorites.setColorFilter(if (currentFragmentTag == "favorites") selectedColor else unselectedColor)
        navIconCompare.setColorFilter(if (currentFragmentTag == "compare") selectedColor else unselectedColor)
        navIconRandom.setColorFilter(if (currentFragmentTag == "random") selectedColor else unselectedColor)
        navIconQuiz.setColorFilter(if (currentFragmentTag == "quiz") selectedColor else unselectedColor)
    }

    private fun updateBottomNavVisibility() {
        bottomNavContainer.visibility = if (currentFragmentTag in bottomNavFragments) View.VISIBLE else View.GONE
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
            currentFragmentTag = item.itemId.toString()
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

        val tag = when (fragment) {
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
        currentFragmentTag = tag
        updateNavigationIcon()
        updateBottomNavVisibility()
        updateBottomNavSelection()
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
