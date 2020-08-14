package von.com.au.trackinternet

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.ONGOING_NOTIFICATION_ID
import java.io.File

/**
 * Foreground service
 *
 * Once started runs independently of the app
 * Records changes in wifi connection status in a log file
 * Records changes to the internet connectivity to the sme log file
 * Uses two broadcast receivers to detect changes to wifi and internet
 * Majority of the code is in the class [UtilsRecordOutages]
 */

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ServiceRecOutages : Service() {
    private val tag = javaClass.simpleName          //used for debugging in Logcat

    private lateinit var utilsRecOut: UtilsRecordOutages

    /*
     * called first time service is created
     *
    * instantiate class [UtilsRecordOutages] to record outages
    */
    override fun onCreate() {
        super.onCreate()
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onCreate(): " + getString(R.string.debug_started))

        utilsRecOut = UtilsRecordOutages(this)
    }

    /**
     * Called every time service is started
     *
     * Extracts log file name from the intent
     * sets up notification channel
     * builds a notification
     * adds pending intent to start app when user clicks on the notification
     * starts recording
     * runs service in foreground
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onStartCommand(): " + getString(R.string.debug_started))

        //get name of the file to record outages in
        val fileName: String = intent.getStringExtra(MyConstants.INTENT_EXTRA_FILENAME)
        //start recording
        utilsRecOut.startRecordOutages(File(fileName))

        //build pending intent to start main activity
        val notifyIntent: Intent = Intent(this, MainActivity::class.java).apply {
            //   todo() sort out flag as clashes with parameters    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val resultPendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //build notification and add pending intent
        val notification: Notification = NotificationCompat.Builder(
            this,
            MyConstants.NOT_CHANNEL_ID
        )
            .setContentTitle(getString(R.string.not_name))
            .setContentText(getString(R.string.not_description_text))
            .setSmallIcon(R.drawable.wifi_on)
            .setContentIntent(resultPendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    /**
     * Called when service is about to stop
     *
     * calls [stopRecordingOutages]() which
     * writes a stop header to log file
     * unregisters the broadcast receivers
     * closes log file
     */
    override fun onDestroy() {
        super.onDestroy()
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onDestroy(): " + getString(R.string.debug_started))

        utilsRecOut.stopRecordingOutages()
    }

    /**
     * Not used. As this is a started service, mandatory override required
     */
    override fun onBind(intent: Intent): IBinder? {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onBind(): " + getString(R.string.debug_started))
        return null
    }
}