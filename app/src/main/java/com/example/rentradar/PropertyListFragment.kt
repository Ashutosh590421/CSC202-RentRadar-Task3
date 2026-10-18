
package com.example.rentradar

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PropertyListFragment : Fragment() {

    private lateinit var database: PropertyDatabase
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private var properties = listOf<Property>()
    private var sortJob: Job? = null
    private var sortMode = 0

    private val dateFormat = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val context = requireContext()

        database = PropertyDatabase.getDatabase(context)

        val preferences = context.getSharedPreferences(
            "rent_radar_preferences",
            0
        )

        sortMode = preferences.getInt("sort_mode", 0)

        // Main vertical layout

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Toolbar

        val toolbar = Toolbar(context).apply {
            title = "Rent Radar"
            setBackgroundColor(0xFF1565C0.toInt())
            setTitleTextColor(0xFFFFFFFF.toInt())
        }

        val newItem = toolbar.menu.add("New")
        newItem.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_ALWAYS
        )

        val sortItem = toolbar.menu.add("Sort")
        sortItem.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_ALWAYS
        )

        val helpItem = toolbar.menu.add("Help")
        helpItem.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_ALWAYS
        )

        toolbar.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {

                "New" -> {
                    openPropertyDetail(null)
                    true
                }

                "Sort" -> {
                    showSortDialog()
                    true
                }

                "Help" -> {
                    openHelp()
                    true
                }

                else -> false
            }
        }

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Property ListView

        listView = ListView(context)

        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        )

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->

            if (position in properties.indices) {
                val selectedProperty = properties[position]
                openPropertyDetail(selectedProperty.id)
            }
        }

        // Empty list message

        val emptyMessage = TextView(context).apply {
            text = "No properties recorded yet.\n" +
                    "Tap New to add a property."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(24, 48, 24, 48)
        }

        root.addView(
            emptyMessage,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        listView.emptyView = emptyMessage

        return root
    }

    override fun onStart() {
        super.onStart()
        loadProperties()
    }

    override fun onStop() {
        sortJob?.cancel()
        sortJob = null
        super.onStop()
    }

    // Open New or Existing Property

    private fun openPropertyDetail(propertyId: String?) {

        val fragment = PropertyDetailFragment()

        if (propertyId != null) {
            fragment.arguments = Bundle().apply {
                putString("property_id", propertyId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    // Load Properties from Room

    private fun loadProperties() {

        sortJob?.cancel()

        val dao = database.propertyDao()

        sortJob = viewLifecycleOwner.lifecycleScope.launch {

            val propertyFlow = if (sortMode == 0) {
                dao.getPropertiesByNewest()
            } else {
                dao.getPropertiesByLowestRent()
            }

            propertyFlow.collect { results ->

                properties = results

                val rows = results.map { property ->

                    val formattedDate = dateFormat.format(
                        Date(property.inspectionDate)
                    )

                    "${property.title}\n" +
                            "$formattedDate  |  " +
                            "$${property.weeklyRent}/week"
                }

                adapter.clear()
                adapter.addAll(rows)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // Sorting Dialog

    private fun showSortDialog() {

        val options = arrayOf(
            "Inspection date (newest first)",
            "Weekly rent (lowest first)"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Sort Properties")
            .setSingleChoiceItems(
                options,
                sortMode
            ) { dialog, selected ->

                sortMode = selected

                requireContext()
                    .getSharedPreferences(
                        "rent_radar_preferences",
                        0
                    )
                    .edit()
                    .putInt("sort_mode", sortMode)
                    .apply()

                loadProperties()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Open Help WebView

    private fun openHelp() {

        val intent = Intent(
            requireContext(),
            HelpActivity::class.java
        )

        startActivity(intent)
    }
}
