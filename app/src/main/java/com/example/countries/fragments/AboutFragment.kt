package com.example.countries.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.countries.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aboutLogo = view.findViewById<ImageView>(R.id.aboutLogo)
        val appName = view.findViewById<TextView>(R.id.appName)
        val versionText = view.findViewById<TextView>(R.id.versionText)

        aboutLogo.setImageResource(R.drawable.ic_app_icon)
        appName.text = "Country Explorer"

        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            versionText.text = "Version ${packageInfo.versionName} (Build $versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            versionText.text = "Version 1.0"
        }
    }
}
