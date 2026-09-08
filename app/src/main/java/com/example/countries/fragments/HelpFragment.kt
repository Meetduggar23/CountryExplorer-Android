package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.countries.R

class HelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val helpContainer = view.findViewById<LinearLayout>(R.id.helpContainer)

        val sections = listOf(
            "Searching Countries" to "Use the search bar at the top to find countries by name. Results update as you type.",
            "A-Z Filter" to "Tap any letter to jump to countries starting with that letter for quick navigation.",
            "Sorting Countries" to "Use the sort options to arrange countries by name, population, area, or other criteria.",
            "Favorites" to "Tap the star icon on any country to add it to your favorites. Tap again to remove. Use the clear button to remove all favorites at once.",
            "Pinning Countries" to "Tap the pin icon to pin important countries to the top of your list. Pinned countries appear first regardless of sorting.",
            "Comparing Countries" to "Select two countries to compare their details side by side, including population, area, capital, and more.",
            "Random Country" to "Tap the random button to discover a random country with all its details.",
            "Country Quiz" to "Test your knowledge with the country quiz. Answer questions about flags, capitals, and geography.",
            "Recently Viewed" to "Access your recently viewed countries from the history section to quickly revisit them.",
            "Sharing / Copying" to "Share country information with friends or copy details to your clipboard using the share or copy buttons.",
            "Refreshing Data" to "Pull down on the list to refresh country data and get the latest information from the API."
        )

        sections.forEach { (title, description) ->
            val sectionView = createSection(title, description)
            helpContainer.addView(sectionView)
        }
    }

    private fun createSection(title: String, description: String): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            setTextColor(resources.getColor(R.color.text_white, null))
            textSize = 16f
            setPadding(0, 0, 0, dpToPx(4))
        }

        val descView = TextView(requireContext()).apply {
            text = description
            setTextColor(resources.getColor(R.color.text_light_gray, null))
            textSize = 14f
        }

        layout.addView(titleView)
        layout.addView(descView)
        return layout
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
