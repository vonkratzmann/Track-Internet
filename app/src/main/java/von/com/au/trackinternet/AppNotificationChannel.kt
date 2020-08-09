package von.com.au.trackinternet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import mysites.com.au.checkinternetconnection.R

/**
 * Class for notification channels
 *
 * In manifest this class wraps whole app
 * and its onCreate() will be called as soon as we start the application
 */

//Even though warning from Lint, its actually used in AndroidManifest.xml
class AppNotificationChannel : Application() {
    private val tag = javaClass.simpleName          //used for debugging in Logcat

    override fun onCreate() {
        super.onCreate()
        if (MyDebug.DEB_FUN_START) Log.d(tag, "onCreate(): " + getString(R.string.debug_started))

        //foreground service requires a notification channel for Android Oreo and above
        createNotificationChannel()
    }

    /**
     * createNotificationChannel()
     *
     * As using a foreground service
     * a notification channel is required for Android Oreo
     * where all notifications must be assigned to a channel
     * if not Oreo ignore
     */
    private fun createNotificationChannel() {
        if (MyDebug.DEB_FUN_START) Log.d(tag,
            "createNotificationChannel(): " + getString(R.string.debug_started))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MyConstants.NOT_CHANNEL_ID,
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