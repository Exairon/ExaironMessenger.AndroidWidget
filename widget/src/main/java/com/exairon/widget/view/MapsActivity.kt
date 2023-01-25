package com.exairon.widget.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_chat.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mGoogleMap: GoogleMap
    private var currentLocation: LatLng? = null
    private var LOCATION_REQUEST_CODE = 400
    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        send_button.setOnClickListener {
            if (currentLocation != null) {
                StateManager.location = currentLocation
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setCurrentLocation() {
        if (latitude !== null && longitude != null) {
            val location = LatLng(latitude!!, longitude!!)
            mGoogleMap.addMarker(MarkerOptions().position(location).title("Selected Location"))
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        }
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mGoogleMap = googleMap;

        if (intent.getStringExtra("latitude") != null) {
            latitude = intent.getStringExtra("latitude")?.toDouble()
            longitude = intent.getStringExtra("longitude")?.toDouble()
            setCurrentLocation()
        }

        mGoogleMap.setOnMapClickListener { param1 ->
            val location = LatLng(param1.latitude, param1.longitude)
            mGoogleMap.clear()
            currentLocation = location
            mGoogleMap.addMarker(MarkerOptions().position(location).title("Selected Location"))
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        }

    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setCurrentLocation()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf("Manifest.permission.ACCESS_FINE_LOCATION"),LOCATION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_REQUEST_CODE) {
            if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION ) {
                setCurrentLocation()
            }
        }
    }
}