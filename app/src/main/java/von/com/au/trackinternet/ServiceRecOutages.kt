package von.com.au.trackinternet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.NOT_CHANNEL_ID
import von.com.au.trackinternet.MyConstants.ONGOING_NOTIFICATION_ID
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
     * sets up notification channel
     * and a notification
     * starts recording
     * runs service in foreground
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onStartCommand(): " + getString(R.string.debug_started))

        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(
            this,
            NOT_CHANNEL_ID
        )
            .setContentTitle(getString(R.string.not_name))
            .setContentText(getString(R.string.not_description_text))
            .setSmallIcon(R.drawable.wifi_on)
            .build()

        //get name of the file to record outages in
        val fileName: String = intent.getStringExtra(MyConstants.INTENT_EXTRA_FILENAME)
        //start recording
        utilsRecOut.startRecordOutages(File(fileName))

        startForeground(ONGOING_NOTIFICATION_ID, notification)
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

    /**
     *
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOT_CHANNEL_ID,
                getString(R.string.not_name),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = getString(R.string.not_description_text)

            /* require a notification handler, so register the channel with system
         * importance or notification behaviours cannot be change after this
         */
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}