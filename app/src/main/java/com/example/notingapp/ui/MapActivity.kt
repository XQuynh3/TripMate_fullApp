package com.example.notingapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.notingapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.android.gms.location.LocationServices
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var searchInput: EditText
    private lateinit var resultList: ListView
    private lateinit var confirmBtn: Button
    private lateinit var currentLocationBtn: Button

    private var selectedPoint: GeoPoint? = null
    private var selectedName: String = "Selected location"

    private val LOCATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        searchInput = findViewById(R.id.searchLocation)
        resultList = findViewById(R.id.resultList)
        confirmBtn = findViewById(R.id.confirmBtn)
        currentLocationBtn = findViewById(R.id.currentLocationBtn)

        map.setMultiTouchControls(true)

        val startPoint = GeoPoint(10.762622, 106.660172)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        setupSearch()
        setupMapClick()
        setupConfirm()
        setupCurrentLocation()
    }

    // 🔍 SEARCH MULTI RESULT
    private fun setupSearch() {

        searchInput.setOnEditorActionListener { _, _, _ ->

            val query = searchInput.text.toString()

            if (query.isNotEmpty()) {

                val geocoder = Geocoder(this, Locale.getDefault())

                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 5)

                val names = results?.map {
                    it.getAddressLine(0)
                } ?: emptyList()

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    names
                )

                resultList.adapter = adapter

                resultList.setOnItemClickListener { _, _, position, _ ->

                    val addr = results!![position]

                    val point = GeoPoint(addr.latitude, addr.longitude)

                    selectedPoint = point
                    selectedName = addr.getAddressLine(0)

                    showMarker(point, selectedName)
                }
            }
            true
        }
    }

    // 🗺 CLICK MAP
    private fun setupMapClick() {

        map.setOnTouchListener { _, event ->

            if (event.action == android.view.MotionEvent.ACTION_UP) {

                val geoPoint = map.projection.fromPixels(
                    event.x.toInt(),
                    event.y.toInt()
                ) as GeoPoint

                selectedPoint = geoPoint
                selectedName = getLocationName(
                    geoPoint.latitude,
                    geoPoint.longitude
                )

                showMarker(geoPoint, selectedName)
            }
            false
        }
    }

    // 📍 SHOW MARKER
    private fun showMarker(point: GeoPoint, title: String) {

        map.overlays.clear()

        val marker = Marker(map)
        marker.position = point
        marker.title = title

        map.overlays.add(marker)
        map.controller.animateTo(point)
    }

    // ✔ CONFIRM BUTTON
    private fun setupConfirm() {

        confirmBtn.setOnClickListener {

            if (selectedPoint == null) {
                Toast.makeText(this, "Chọn vị trí trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = Intent()
            result.putExtra("lat", selectedPoint!!.latitude)
            result.putExtra("lng", selectedPoint!!.longitude)
            result.putExtra("locationName", selectedName)

            setResult(RESULT_OK, result)
            finish()
        }
    }

    // 📍 CURRENT LOCATION (FIX PERMISSION)
    private fun setupCurrentLocation() {

        val client = LocationServices.getFusedLocationProviderClient(this)

        currentLocationBtn.setOnClickListener {

            if (!hasLocationPermission()) {
                requestLocationPermission()
                return@setOnClickListener
            }

            client.lastLocation.addOnSuccessListener { location ->

                if (location != null) {

                    val point = GeoPoint(location.latitude, location.longitude)

                    selectedPoint = point
                    selectedName = "My location"

                    showMarker(point, selectedName)
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔥 PERMISSION CHECK
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 🔥 REQUEST PERMISSION
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }

    // 🔥 HANDLE RESULT
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocationName(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Selected location"
            }
        } catch (_: Exception) {
            "Selected location"
        }
    }
}