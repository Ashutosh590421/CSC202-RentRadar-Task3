
package com.example.rentradar

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PropertyDetailFragment : Fragment() {

    private lateinit var database: PropertyDatabase

    private lateinit var titleInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var dateButton: Button
    private lateinit var typeSpinner: Spinner
    private lateinit var rentInput: EditText
    private lateinit var bedroomsInput: EditText
    private lateinit var statusSpinner: Spinner
    private lateinit var notesInput: EditText

    private lateinit var gpsText: TextView
    private lateinit var mapButton: Button
    private lateinit var photoView: ImageView

    private var property = Property()
    private var existingId: String? = null
    private var pendingPhotoPath: String? = null
    private var saving = false

    private val dateFormat = SimpleDateFormat(
        "dd MMM yyyy", Locale.getDefault()
    )

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val path = pendingPhotoPath
            if (success && path != null) {
                property.photoPath = path
                updatePhoto()
                showMessage("Photo captured successfully")
            } else {
                path?.let { File(it).delete() }
                showMessage("Photo was not captured")
            }
            pendingPhotoPath = null
        }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                getCurrentLocation()
            } else {
                showMessage("Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingId = arguments?.getString("property_id")
        pendingPhotoPath =
            savedInstanceState?.getString("pending_photo")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_photo", pendingPhotoPath)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val context = requireContext()
        database = PropertyDatabase.getDatabase(context)

        val scroll = ScrollView(context)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 48)
        }

        fun heading(label: String) {
            layout.addView(TextView(context).apply {
                text = label
                textSize = 16f
                setTextColor(0xFF222222.toInt())
                setPadding(0, 18, 0, 6)
            })
        }

        fun input(hintText: String): EditText {
            return EditText(context).apply {
                hint = hintText
                textSize = 16f
                setSingleLine(true)
                layout.addView(this)
            }
        }

        layout.addView(TextView(context).apply {
            text = if (existingId == null)
                "New Property" else "Property Details"
            textSize = 24f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, 8, 0, 16)
        })

        heading("Title")
        titleInput = input("e.g. 2BR Apartment, Brisbane")

        heading("Address")
        addressInput = input("Enter property address")

        heading("Inspection Date")
        dateButton = Button(context).apply {
            text = dateFormat.format(Date(property.inspectionDate))
            setOnClickListener { openDatePicker() }
        }
        layout.addView(dateButton)

        heading("Property Details")

        heading("Property Type")
        val types = arrayOf(
            "Apartment", "House", "Townhouse", "Sharehouse"
        )
        typeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                types
            )
        }
        layout.addView(typeSpinner)

        heading("Weekly Rent (AUD)")
        rentInput = input("e.g. 420").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        heading("Bedrooms (0-9)")
        bedroomsInput = input("e.g. 2").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        heading("Application Status")
        val statuses = arrayOf(
            "Inspected", "Applied", "Approved", "Missed out"
        )
        statusSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                statuses
            )
        }
        layout.addView(statusSpinner)

        heading("Notes")
        notesInput = EditText(context).apply {
            hint = "Notes about the property inspection"
            minLines = 4
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        layout.addView(notesInput)

        // GPS

        heading("GPS Location")
        gpsText = TextView(context).apply {
            text = "GPS location not recorded"
            textSize = 15f
            setPadding(8, 12, 8, 12)
        }
        layout.addView(gpsText)

        layout.addView(Button(context).apply {
            text = "Get GPS"
            setOnClickListener { checkLocationPermission() }
        })

        mapButton = Button(context).apply {
            text = "Show Map"
            isEnabled = false
            setOnClickListener { openGoogleMaps() }
        }
        layout.addView(mapButton)

        // PHOTO

        heading("Image")

        photoView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFFECECEC.toInt())
            setOnClickListener { enlargePhoto() }
        }
        layout.addView(photoView)

        layout.addView(Button(context).apply {
            text = "Take Photo"
            setOnClickListener { takePhoto() }
        })

        // SHARE

        layout.addView(Button(context).apply {
            text = "Share Property"
            setOnClickListener { shareProperty() }
        })

        // SAVE

        layout.addView(Button(context).apply {
            text = "Save Property"
            setOnClickListener { saveProperty() }
        })

        if (existingId != null) {
            layout.addView(Button(context).apply {
                text = "Delete Property"
                setOnClickListener { confirmDelete() }
            })
        }

        scroll.addView(layout)
        updateGpsDisplay()
        updatePhoto()

        return scroll
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (titleInput.text.toString().isBlank()) {
                        parentFragmentManager.popBackStack()
                    } else {
                        saveProperty()
                    }
                }
            }
        )

        existingId?.let { id ->
            viewLifecycleOwner.lifecycleScope.launch {
                val loaded = database.propertyDao()
                    .getPropertyById(id)

                if (loaded != null) {
                    property = loaded
                    populateFields()
                } else {
                    showMessage("Property not found")
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun populateFields() {
        titleInput.setText(property.title)
        addressInput.setText(property.address)
        dateButton.text =
            dateFormat.format(Date(property.inspectionDate))
        rentInput.setText(property.weeklyRent.toString())
        bedroomsInput.setText(property.bedrooms.toString())
        notesInput.setText(property.notes)

        val types = listOf(
            "Apartment", "House", "Townhouse", "Sharehouse"
        )
        typeSpinner.setSelection(
            types.indexOf(property.propertyType).coerceAtLeast(0)
        )

        val statuses = listOf(
            "Inspected", "Applied", "Approved", "Missed out"
        )
        statusSpinner.setSelection(
            statuses.indexOf(property.applicationStatus)
                .coerceAtLeast(0)
        )

        updateGpsDisplay()
        updatePhoto()
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = property.inspectionDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                property.inspectionDate = calendar.timeInMillis
                dateButton.text = dateFormat.format(
                    Date(property.inspectionDate)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun readForm(): Boolean {
        val title = titleInput.text.toString().trim()

        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            titleInput.requestFocus()
            return false
        }

        property.title = title
        property.address = addressInput.text.toString().trim()
        property.propertyType = typeSpinner.selectedItem.toString()
        property.applicationStatus =
            statusSpinner.selectedItem.toString()
        property.notes = notesInput.text.toString()

        property.weeklyRent =
            rentInput.text.toString().toIntOrNull()
                ?.coerceAtLeast(0) ?: 0

        property.bedrooms =
            bedroomsInput.text.toString().toIntOrNull()
                ?.coerceIn(0, 9) ?: 0

        return true
    }

    private fun saveProperty() {
        if (saving || !readForm()) return
        saving = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                database.propertyDao().insert(property)
                showMessage("Property saved")
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                saving = false
                showMessage("Save failed: ${e.message}")
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Property")
            .setMessage("Delete this rental property?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        database.propertyDao().delete(property)
                        property.photoPath?.let {
                            File(it).delete()
                        }
                        showMessage("Property deleted")
                        parentFragmentManager.popBackStack()
                    } catch (e: Exception) {
                        showMessage("Delete failed: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // CAMERA

    private fun takePhoto() {
        try {
            val directory = File(
                requireContext().filesDir, "images"
            )
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val photoFile = File.createTempFile(
                "rent_radar_", ".jpg", directory
            )

            pendingPhotoPath = photoFile.absolutePath

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            showMessage("Camera error: ${e.message}")
        }
    }

    private fun updatePhoto() {
        if (!::photoView.isInitialized) return

        val path = property.photoPath

        if (path != null && File(path).exists()) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            val largest = maxOf(
                options.outWidth, options.outHeight
            )

            val sample = when {
                largest > 4000 -> 8
                largest > 2000 -> 4
                largest > 1000 -> 2
                else -> 1
            }

            val bitmap = BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                }
            )
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    private fun enlargePhoto() {
        val path = property.photoPath

        if (path == null || !File(path).exists()) {
            showMessage("No photo available")
            return
        }

        PhotoDialogFragment.newInstance(path).show(
            parentFragmentManager, "photo_dialog"
        )
    }

    // SHARING

    private fun shareProperty() {
        if (!readForm()) return

        val summary = """
            Rent Radar Property Summary

            Title: ${property.title}
            Address: ${property.address}
            Inspection Date: ${
            dateFormat.format(Date(property.inspectionDate))
        }
            Weekly Rent: AUD $${property.weeklyRent}
            Application Status: ${property.applicationStatus}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary)
        }

        startActivity(
            Intent.createChooser(intent, "Share Property")
        )
    }

    // LOCATION

    private fun checkLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val manager = requireContext().getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val provider = when {
            manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) -> LocationManager.GPS_PROVIDER

            manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            ) -> LocationManager.NETWORK_PROVIDER

            else -> {
                showMessage("Enable device location")
                return
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(
                    provider,
                    null,
                    ContextCompat.getMainExecutor(
                        requireContext()
                    )
                ) { location ->
                    if (isAdded) updateLocation(location)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(
                    provider,
                    object : android.location.LocationListener {
                        override fun onLocationChanged(
                            location: Location
                        ) {
                            if (isAdded) {
                                updateLocation(location)
                            }
                        }
                    },
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            showMessage("GPS error: ${e.message}")
        }
    }

    private fun updateLocation(location: Location?) {
        if (location == null) {
            showMessage("Location unavailable")
            return
        }

        property.latitude = location.latitude
        property.longitude = location.longitude
        updateGpsDisplay()

        if (readForm()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    database.propertyDao().insert(property)
                    showMessage("GPS location saved")
                } catch (e: Exception) {
                    showMessage("Unable to save GPS location")
                }
            }
        } else {
            showMessage("GPS captured; save the property")
        }
    }

    private fun updateGpsDisplay() {
        if (!::gpsText.isInitialized) return

        val latitude = property.latitude
        val longitude = property.longitude

        if (latitude != null && longitude != null) {
            gpsText.text = String.format(
                Locale.US,
                "Latitude: %.6f\nLongitude: %.6f",
                latitude, longitude
            )
            mapButton.isEnabled = true
        } else {
            gpsText.text = "GPS location not recorded"
            mapButton.isEnabled = false
        }
    }

    private fun openGoogleMaps() {
        val latitude = property.latitude
        val longitude = property.longitude

        if (latitude == null || longitude == null) {
            showMessage("GPS location not recorded")
            return
        }

        val url = String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.6f%%2C%.6f",
            latitude, longitude
        )

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )

        intent.setPackage("com.android.chrome")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            intent.setPackage(null)
            try {
                startActivity(intent)
            } catch (error: Exception) {
                showMessage("Unable to open Google Maps")
            }
        }
    }

    private fun showMessage(message: String) {
        if (isAdded) {
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
