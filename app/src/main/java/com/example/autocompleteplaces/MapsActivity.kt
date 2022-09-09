package com.example.autocompleteplaces

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.autocompleteplaces.BuildConfig.MAPS_API_KEY

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.autocompleteplaces.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.Response
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import com.android.volley.Response as Response1

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var TAG = "Info"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, MAPS_API_KEY)
        }

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                mMap.addMarker(MarkerOptions().position(place.latLng).title(place.name))
                val cameraPosition = CameraPosition.Builder()
                    .target(place.latLng)
                    .zoom(15f)
                    .bearing(0f)
                    .build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                Log.i(TAG, "Nome: ${place.name}, ID: ${place.id}, latlng: ${place.latLng}")
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }
        })

        var waypoints = arrayOf(LatLng(43.722517,13.2056245),LatLng(43.6753223,13.2184074),
                                LatLng(43.7641394,13.1405653))

        val btnHome : Button = findViewById(R.id.button1);
        btnHome.setOnClickListener {
            setDirections(LatLng(43.8424173,13.0146632),waypoints)
        }


    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(5f)

        setUpMap()

        }



    private fun setUpMap() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        moveOnLocalPosition()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED)
            moveOnLocalPosition()
        else super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    private fun moveOnLocalPosition () {
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLong = LatLng(location.latitude, location.longitude)
                mMap.animateCamera((CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f)))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setDirections(destination: LatLng, waypoint: Array<LatLng> ) {
        // Direction between two points
        val latLngOrigin = LatLng(lastLocation.latitude, lastLocation.longitude) //current location
        val latLngDestination = destination // Fano
        for (i in 0 until waypoint.size){
            mMap!!.addMarker(MarkerOptions().position(waypoint[i]).title("Point" + i))
        }

        //val latLngWaypoint = waypoint // Mondolfo

        mMap.addMarker(MarkerOptions()
                            .position(latLngOrigin)
                            .title("Posizione Attuale")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        mMap.addMarker(MarkerOptions()
                             .position(latLngDestination)
                             .title("Destination"))
        /*
        mMap!!.addMarker(MarkerOptions().position(latLngWaypoint).title("Point1"))
        mMap!!.addMarker(MarkerOptions().position(latLngWaypoint).title("Point1"))
        mMap!!.addMarker(MarkerOptions().position(latLngWaypoint).title("Point1"))
        */

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOrigin, 11f))
        val path: MutableList<List<LatLng>> = ArrayList()
        //val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=43.7149517,13.2179488&destination=43.8424173,13.0146632&key=$MAPS_API_KEY"
        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin="+ lastLocation.latitude + "," + lastLocation.longitude +
                             "&destination="+ destination.latitude + "," + destination.longitude +"&waypoints=via%3A"+ waypoint[0].latitude + "," +
                               waypoint[0].longitude + "|via%3A" + waypoint[1].latitude + "," + waypoint[1].longitude + "|via%3A" + waypoint[2].latitude +
                              "," + waypoint[2].longitude + "&key=$MAPS_API_KEY"
        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response1.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            Log.i(TAG, "An error occurred: $steps")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                mMap!!.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
            }
        }, Response1.ErrorListener {
                _ ->
        }){}
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(directionsRequest)
       }

    /*
    ergegeg
     */


}