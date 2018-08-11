package cbedoy.wthamf

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationRequest
import com.patloew.rxlocation.RxLocation

/**
 * wthamf
 *
 * Created by bedoy on 8/10/18.
 */
object NotifierController{

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0

    @SuppressLint("MissingPermission")
    fun init(context: Context){
        val rxLocation = RxLocation(context)
        PubNubController.init(context)
        PubNubController.registerUser("cbedoy")
        val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)

        rxLocation.location().updates(locationRequest)
                .flatMap { location -> rxLocation.geocoding().fromLocation(location).toObservable() }
                .subscribe({
                    /* do something */
                    if (it.latitude != lastLatitude && it.longitude != lastLongitude) {
                        val latitude = it.latitude
                        val longitude = it.longitude

                        val map = HashMap<String, Any>()
                        map["latitude"] = latitude
                        map["longitude"] = longitude

                        PubNubController.publishMessageToChannel(map, "general")

                        lastLatitude = it.latitude
                        lastLongitude = it.longitude
                    }
                })

    }

    fun destroy() {

    }
}