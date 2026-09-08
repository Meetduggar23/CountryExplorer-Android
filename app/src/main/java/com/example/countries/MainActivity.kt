package com.example.countries

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
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
    val imageCache = mutableMapOf<String, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Country Explorer"

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        viewModel = ViewModelProvider(this)[CountryViewModel::class.java]

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            navigationView.setCheckedItem(R.id.nav_home)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
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
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
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
