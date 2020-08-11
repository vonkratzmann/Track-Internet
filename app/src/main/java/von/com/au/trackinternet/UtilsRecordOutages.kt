@file:Suppress("DEPRECATION")

package von.com.au.trackinternet

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.NetworkInfo.DetailedState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.LOG_HEADER_SPACING
import von.com.au.trackinternet.MyConstants.MAX_FILE_RECORDS
import von.com.au.trackinternet.MyDebug.DEB_FUN_START
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class for recording internet outages on wifi
 * number of utility functions
 */

class UtilsRecordOutages(val mContext: Context?) {
    private val tag = javaClass.simpleName          //used for debugging in Logcat
    private lateinit var gOutputStream: FileOutputStream    //stream used to write to log file
    private var gLineCount = 0                              //track number of records written
    private lateinit var gWifiReceiver: BroadcastReceiver
    private lateinit var gNetworkReceiver: BroadcastReceiver
    private lateinit var gUtilsGeneral: UtilsGeneral       //simple utilities functions

    /* used to track last status, if no change in status from last time does not write a record to log file
     * some phones broadcast wifi changes very close together, but on interrogation the state has not changed
     */
    private var gNetworkLastStatus: String? = mContext?.getString(R.string.log_internet_status_unknown)
    private var gWifiLastStatus: String? = mContext?.getString(R.string.log_wifi_status_unknown)

    /**
     * startRecordOutages(mFilename: String)
     *
     * Called by onStartCommand() in the foreground service
     * Assumes file accessibility has been checked before this call
     * open IO stream for log file in append mode
     * write header to log file
     * set up broadcast receiver
     * register listener to process incoming data
     * return boolean success/failure
     * Initialise global variables for other functions to use
     *
     * See [MyConstants] for width of columns and layout
     */
    fun startRecordOutages(file: File): Boolean {
        if (DEB_FUN_START) Log.d(tag, "startRecordOutages: " + mContext?.getString(R.string.debug_started) + "\n")
        //instantiate class for file utilities
        gUtilsGeneral = UtilsGeneral(mContext)

        try {
            gOutputStream = FileOutputStream(file, true)    //append to the file
            val b: ByteArray = getLogFileHeader().toByteArray()     //write the header to the log file
            gOutputStream.write(b)
        } catch (e: IOException) {
            //Unable to create file
            Log.w(tag, "Error writing to file: $file  $e")
            return false
        }
        setupWifiChangeBroadcastRec()
        setupNetworkChangeBroadcastRec()
        registerWifiChangeRec()
        registerNetworkChangeRec()

        return true
    }

    /**
     * getLogFileHeader()
     * Header contains
     *  - phone model
     *  - android version
     *  - date time
     *  - starting message
     *   - column headings
     *  remove white space from model name so if imported into spreadsheet not spread across multiple columns
     */
    private fun getLogFileHeader(): String {
        if (DEB_FUN_START) Log.d(tag, "getLogFileHeader(): " + mContext?.getString(R.string.debug_started) + "\n")

        val dateTime = gUtilsGeneral.getDateTime()
        val model: String = (Build.MODEL).replace(regex = "\\s".toRegex(), replacement = "")
        val androidVerHeader = mContext?.getString(R.string.log_android_ver_header)
        val androidVer: String = Build.VERSION.RELEASE
        val headerMessage = mContext?.getString(R.string.status_start_header)
        val line1 = "$model $androidVerHeader: $androidVer\n"    //title for header
        val line2 = "$dateTime $headerMessage\n"                 //header date and time
        val line3 = getColumnNames()                     //column names for log file
        return line1 + line2 + line3
    }

    /**
     * getColumnNames()
     *
     * generate a string with column names for log file
     */
    private fun getColumnNames(): String {
        if (DEB_FUN_START) Log.d(tag, "getColumnNames(): " + mContext?.getString(R.string.debug_started) + "\n")

        return (mContext?.getString(R.string.log_date_header)?.padEnd(LOG_HEADER_SPACING)
                + mContext?.getString(R.string.log_time_header)?.padEnd(LOG_HEADER_SPACING)
                + mContext?.getString(R.string.log_ssid_header)?.padEnd(LOG_HEADER_SPACING)
                + mContext?.getString(R.string.log_freq_header)?.padEnd(LOG_HEADER_SPACING)
                + mContext?.getString(R.string.log_status_header)?.padEnd(LOG_HEADER_SPACING)
                + "\n")
    }

    /**
     * stopRecordingOutages()
     *
     * Called by BroadCastRec() if reached maximum number of records or
     * Called by onDestroy() in foreground service or
     * called by onOptionsItemSelected()in main activity
     * when a menu item is selected to quit the app and stop recording
     *
     * write a stop header with date time to file
     * unregister listeners
     * close stream
     */
    fun stopRecordingOutages() {
        if (DEB_FUN_START) Log.d(tag, "stopRecordingOutages(): " + mContext?.getString(R.string.debug_started))

        val stopHeader: String = gUtilsGeneral.getDateTime() + " " + mContext?.getString(R.string.status_stop_header) + "\n"
        try {
            unRegisterNetworkChangeRec()
            unRegisterWifiChangeRec()
            gOutputStream.write(stopHeader.toByteArray())
            gOutputStream.close()
        } catch (e: IOException) {
            Log.w(tag, "Error writing to file $e")
        }
    }

    /**
     * setupNetworkChangeBroadcastRec()
     *
     * called by startRecordOutages()
     * Set up broadcast receiver for Wifi changes
     * override function onReceive
     * for each wifi change records:
     *  - date/time stamped
     *  - ssid of wifi network connected
     *  - frequency of wifi network
     *  - internet connectivity status
     *  if reached max number of records
     *   - stop recording
     */
    private fun setupNetworkChangeBroadcastRec() {
        if (DEB_FUN_START) Log.d(tag, "setupNetworkChangeBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        gNetworkReceiver = object : BroadcastReceiver() {

            //receiver for changes to internet connections
            @Suppress("DEPRECATION")
            override fun onReceive(c: Context, intent: Intent) {
                if (DEB_FUN_START) Log.d(tag, "NetworkBroadcastReceiver(): " + mContext?.getString(R.string.debug_started))

                val extras: Bundle? = intent.extras     //get network information to write to log file
                val info = extras?.getParcelable<Parcelable>("networkInfo") as NetworkInfo?

                val status = when (info!!.state) {      //used to store status to be written to log file

                    NetworkInfo.State.CONNECTED -> mContext?.getString(R.string.log_internet_connected)

                    NetworkInfo.State.DISCONNECTED -> mContext?.getString(R.string.log_internet_disconnected)

                    else -> mContext?.getString(R.string.log_internet_status_unknown)
                }
                if (gNetworkLastStatus == status) return                   //no change in status, do nothing
                gNetworkLastStatus = status

                //write the record
                gOutputStream.write("${buildLogFileRecord()} $status\n".toByteArray())

                //check if reached maximum number of records
                if (gLineCount++ > MAX_FILE_RECORDS) {
                    gOutputStream.write(
                        "${gUtilsGeneral.getDateTime()} ${mContext?.getString(R.string.error_max_records)} \n"
                            .toByteArray())
                    stopRecordingOutages()
                    //todo stop foreground service
                }
            }
        }
    }

    /**
     * setupWifiChangeBroadcastRec()
     *
     * Set up broadcast receiver for Wifi changes
     * override function onReceive
     * for each wifi change records an entry in the log file
     */
    private fun setupWifiChangeBroadcastRec() {
        if (DEB_FUN_START) Log.d(tag, "setupNWifiChangeBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        gWifiReceiver = object : BroadcastReceiver() {

            //receiver for changes to wifi connections
            override fun onReceive(c: Context, intent: Intent) {
                if (DEB_FUN_START) Log.d(tag, "WifiBroadcastReceiver(): " + mContext?.getString(R.string.debug_started))

                val dateTime = gUtilsGeneral.getDateTime()
                //EXTRA_WIFI_STATE is the lookup key for whether Wi-Fi is enabled, disabled, enabling, disabling, or unknown.
                val status: String? = when (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {

                    WifiManager.WIFI_STATE_DISABLED -> mContext?.getString(R.string.log_wifi_disabled)

                    WifiManager.WIFI_STATE_ENABLED -> mContext?.getString(R.string.log_wifi_enabled)

                    else -> mContext?.getString(R.string.log_wifi_status_unknown)
                }
                if (gWifiLastStatus == status) return                   //no change in status, do nothing
                gWifiLastStatus = status

                //write the record
                gOutputStream.write("${buildLogFileRecord()} $status\n".toByteArray())

                //check if reached maximum number of records
                if (gLineCount++ > MAX_FILE_RECORDS) {
                    gOutputStream.write("$dateTime ${mContext?.getString(R.string.error_max_records)} \n".toByteArray())
                    stopRecordingOutages()
                    //todo stop foreground service
                }
            }
        }
    }

    /**
     * buildLogFileRecord()
     *
     * gather information to write to log file
     */
    fun buildLogFileRecord(): String {
        if (DEB_FUN_START) Log.d(tag, "buildLogFileRecord(): " + mContext?.getString(R.string.debug_started) + "\n")

        val dateTime = gUtilsGeneral.getDateTime()
        val wifiName = getWifiName() ?: mContext?.getString(R.string.log_no_wifi_name)                //if no wifi set name to "No wifi"
        val wifiFrequency: Int = getWifiFrequency() ?: 0                //if no wifi, set frequency to zero
        return "$dateTime $wifiName $wifiFrequency"
    }

    /*
     * registerNetworkChangeRec()
     *
     * called by startRecordOutages
     * Register a broadcast receiver for any Wifi network changes
     */
    private fun registerNetworkChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "registerNetworkChangeRec(): " + mContext?.getString(R.string.debug_started))

        val intentFilter = IntentFilter()
        @Suppress("DEPRECATION")
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        try {
            mContext?.registerReceiver(gNetworkReceiver, intentFilter)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /*
     * unRegisterNetworkChangeRec()
     *
     * called by stopRecordingOutages()
     * unregister a broadcast receiver for any Wifi network changes
     */

    private fun unRegisterNetworkChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "unregisterNetworkChangeRec(): " + mContext?.getString(R.string.debug_started))

        try {
            mContext?.unregisterReceiver(gWifiReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /*
       * registerWifiChangeRec()
       *
       * Register a broadcast receiver for any Wifi network changes
       */
    @Suppress("DEPRECATION")
    private fun registerWifiChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "registerWifiChangeRec(): " + mContext?.getString(R.string.debug_started))

        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)

        //registerReceiver belongs to the Activity class, in fragment have to add requireActivity()
        try {
            mContext?.registerReceiver(gWifiReceiver, intentFilter)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }


    }

    /*
     * unRegisterWifiChangeRec()
     *
     * unregister a broadcast receiver for any Wifi network changes
     */
    private fun unRegisterWifiChangeRec() {
        if (DEB_FUN_START) Log.d(
            tag,
            "unregisterWifiChangeRec(): " + mContext?.getString(R.string.debug_started))
        try {
            //unregisterReceiver belongs to the Activity class, in fragment have to add requireActivity()
            mContext?.unregisterReceiver(gNetworkReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }


    /**
     * getWifiName(context: Context)
     *
     * get SSID for current connected wifi
     */
    @SuppressLint("WifiManagerPotentialLeak")
    @Suppress("DEPRECATION")
    private fun getWifiName(): String? {
        if (DEB_FUN_START) Log.d(tag, "getWifiName(): " + mContext?.getString(R.string.debug_started))

        //use getApplicationContext as WifiService will memory leak
        val manager = mContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (manager.isWifiEnabled) {
            val wifiInfo = manager.connectionInfo
            if (wifiInfo != null) {
                val state = WifiInfo.getDetailedStateOf(wifiInfo.supplicantState)
                if (state == DetailedState.CONNECTED || state == DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.ssid
                }
            }
        }
        return null
    }

    /**
     * getWifiFrequency(context: Context)
     *
     * get frequency for current connected wifi
     */
    @SuppressLint("WifiManagerPotentialLeak")
    @Suppress("DEPRECATION")
    private fun getWifiFrequency(): Int? {
        if (DEB_FUN_START) Log.d(
            tag,
            "getFrequency(): " + mContext?.getString(R.string.debug_started))

        //use getApplicationContext as WifiService will memory leak
        val manager = mContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (manager.isWifiEnabled) {
            val wifiInfo = manager.connectionInfo
            if (wifiInfo != null) {
                val state = WifiInfo.getDetailedStateOf(wifiInfo.supplicantState)
                if (state == DetailedState.CONNECTED || state == DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.frequency
                }
            }
        }
        return null
    }

    /**
     * stopService()
     *
     */
    fun stopOurService() {
        if (DEB_FUN_START) Log.d(tag, "stopService(): " + mContext?.getString(R.string.debug_started))

        val serviceIntent = Intent(mContext, ServiceRecOutages::class.java)
        mContext?.stopService(serviceIntent)
    }

    /**
     * startOurService(file: File)
     *
     * starts service to record outages and
     * passes name of log file to record outages
     */
    fun startOurService(file: File) {
        if (DEB_FUN_START) Log.d(tag, "startOurService(): " + mContext?.getString(R.string.debug_started))

        val serviceIntent = Intent(mContext, ServiceRecOutages::class.java)
        serviceIntent.putExtra(MyConstants.INTENT_EXTRA_FILENAME, file.toString())
        mContext?.startService(serviceIntent)
    }
}