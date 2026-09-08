package com.example.countries

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class FlagViewerActivity : AppCompatActivity() {

    private lateinit var flagImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var closeBtn: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flag_viewer)

        flagImage = findViewById(R.id.flagImage)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        closeBtn = findViewById(R.id.closeBtn)

        @Suppress("DEPRECATION")
        val flagUrl = intent.getStringExtra("flagUrl") ?: ""
        @Suppress("DEPRECATION")
        val countryName = intent.getStringExtra("countryName") ?: ""

        title = countryName

        closeBtn.setOnClickListener { finish() }

        setupEdgeToEdge()
        loadFlag(flagUrl)
    }

    private fun setupEdgeToEdge() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun loadFlag(url: String) {
        if (url.isEmpty()) {
            showError()
            return
        }

        progressBar.visibility = View.VISIBLE
        flagImage.visibility = View.GONE
        errorText.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    connection.disconnect()

                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            flagImage.setImageBitmap(bitmap)
                            flagImage.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        } else {
                            showError()
                        }
                    }
                } else {
                    connection.disconnect()
                    withContext(Dispatchers.Main) { showError() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError() }
            }
        }
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        flagImage.visibility = View.GONE
        errorText.visibility = View.VISIBLE
    }
}
