package cbedoy.wthamf

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.patloew.rxlocation.RxLocation
import com.google.android.gms.location.LocationRequest



/**
 * wthamf
 *
 * Created by bedoy on 8/10/18.
 */
class RXService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("RXService", "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        NotifierController.init(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        NotifierController.destroy()
    }

}