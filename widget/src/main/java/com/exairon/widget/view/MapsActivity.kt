package com.exairon.widget.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_chat.send_button
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mGoogleMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var LOCATION_REQUEST_CODE = 400

    private var CURRENT_LOCATION_REQUEST_CODE = 500
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        send_button.setOnClickListener {
            if (selectedLocation != null) {
                StateManager.location = selectedLocation
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            }
        }

        current_location.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation(){
        if(checkPermissions()){
            if(isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
                try {
                    fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task ->
                        val location: Location? = task.result
                        if(location == null) {
                            //location null
                        } else {
                            selectedLocation = LatLng(location.latitude, location.longitude)
                            mGoogleMap.addMarker(MarkerOptions().position(selectedLocation!!).title("Current Location"))
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation!!, 16.0f))
                        }
                    }
                } catch (e: Exception) {

                }

            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION), CURRENT_LOCATION_REQUEST_CODE)
        }
    }

    private fun checkPermissions(): Boolean {
        if(ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun setSelectedLocation() {
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
            setSelectedLocation()
        }

        mGoogleMap.setOnMapClickListener { param1 ->
            val location = LatLng(param1.latitude, param1.longitude)
            mGoogleMap.clear()
            selectedLocation = location
            mGoogleMap.addMarker(MarkerOptions().position(location).title("Selected Location"))
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        }

    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setSelectedLocation()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf("Manifest.permission.ACCESS_FINE_LOCATION"), LOCATION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_REQUEST_CODE) {
            if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION ) {
                setSelectedLocation()
            }
        }
        if(requestCode == CURRENT_LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                getCurrentLocation()
            } else {
                // Denied
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val chatIntent = Intent(this, ChatActivity::class.java)
        startActivity(chatIntent)
    }
}