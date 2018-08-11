package cbedoy.wthamf

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions
import com.mapbox.mapboxsdk.plugins.locationlayer.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.plugins.locationlayer.OnLocationLayerClickListener
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode


class MainActivity : AppCompatActivity(), LocationEngineListener, OnLocationLayerClickListener, OnCameraTrackingChangedListener {
    override fun onLocationLayerClick() {

    }

    override fun onCameraTrackingChanged(currentMode: Int) {

    }

    override fun onCameraTrackingDismissed() {

    }

    override fun onLocationChanged(location: Location?) {
        Timber.d("onLocationChanged")

        if (location != null) {
            val latLng = LatLng(location.latitude, location.longitude)

            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16.0)

            mapboxMap?.animateCamera(cameraUpdate)
        }
        locationEngineProvider?.removeLocationEngineListener(this)
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngineProvider?.requestLocationUpdates()
    }

    private var locationEngineProvider: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null
    private var mapboxMap: MapboxMap? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Mapbox.getInstance(this, BuildConfig.MAPBOX_KEY)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {

            mapboxMap = it

            locationEngineProvider = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
            locationEngineProvider?.priority = LocationEnginePriority.HIGH_ACCURACY
            locationEngineProvider?.fastestInterval = 1000
            locationEngineProvider?.addLocationEngineListener(this)
            locationEngineProvider?.activate()

            val options = LocationLayerOptions.builder(this).build()
            locationLayerPlugin = LocationLayerPlugin(mapView, it, locationEngineProvider, options)
            locationLayerPlugin?.addOnLocationClickListener(this)
            locationLayerPlugin?.addOnCameraTrackingChangedListener(this)
            locationLayerPlugin?.cameraMode = CameraMode.NONE

            if (checkLocationPermission()){
                locationEngineProvider?.requestLocationUpdates()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private val MY_PERMISSIONS_REQUEST_LOCATION: Int = 6743

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                        .setTitle("Location")
                        .setMessage("To user our app should give us your access to know location ")
                        .setPositiveButton("", { _, _ ->
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(this@MainActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    MY_PERMISSIONS_REQUEST_LOCATION)
                        })
                        .create()
                        .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_LOCATION)
            }
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    val intent = Intent(this, RXService::class.java)
                    startService(intent)

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
        }
    }
}
