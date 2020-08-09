package von.com.au.trackinternet

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import mysites.com.au.checkinternetconnection.R
import java.io.File

/**
 * Class for foreground service
 */

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ServiceRecOutages : Service() {
    private val tag = javaClass.simpleName          //used for debugging in Logcat

    private lateinit var utilsRecOut: UtilsRecordOutages

    /*
    * onCreate()
    *
    * called first time service is created
    * instantiate class of utilities to record outages
    */
    override fun onCreate() {
        super.onCreate()
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onCreate(): " + getString(R.string.debug_started))

        utilsRecOut = UtilsRecordOutages(this)
    }

    /**
     * onStartCommand()
     *
     * called every time service is started
     * sets up notification
     * starts recording
     * runs service in foreground
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onStartCommand(): " + getString(R.string.debug_started))

        val notification: Notification = NotificationCompat.Builder(this,
            MyConstants.NOT_CHANNEL_ID
        )
            .setContentTitle(getString(R.string.not_name))
            .setContentText(getString(R.string.not_description_text))
            .setSmallIcon(R.drawable.ic_android)
            .build()

        //get name of the file to record outages in
        val fileName: String = intent.getStringExtra(MyConstants.INTENT_EXTRA_FILENAME)
        //start recording
        utilsRecOut.startRecordOutages(File(fileName))

        startForeground(1, notification)
        return START_STICKY
    }

    /**
     * onDestroy()
     *
     * called when service is about to stop
     * write a stop header to log file
     * close file
     */
    override fun onDestroy() {
        super.onDestroy()
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onDestroy(): " + getString(R.string.debug_started))

        utilsRecOut.stopRecordingOutages()
    }

    /**
     * onBind(intent: Intent)
     *
     * not needed as this is a started service, but mandatory for class
     */
    override fun onBind(intent: Intent): IBinder? {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onBind(): " + getString(R.string.debug_started))
        return null
    }
}