@file:Suppress("DEPRECATION")

package von.com.au.trackinternet

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkInfo
import android.net.NetworkInfo.DetailedState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.MAX_FILE_RECORDS
import von.com.au.trackinternet.MyDebug.DEB_FUN_START
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class for recording internet outages
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
     * Start recording outages in the foreground service
     *
     * Called by onStartCommand() in the foreground service
     * Assumes file accessibility has been checked before this call
     * open IO stream for log file in append mode
     * write header to log file
     * set up broadcast receivers for wifi and network changes
     * register listeners to process incoming data
     * return boolean success/failure
     * Initialise global variables for other functions to use
     * @parameter file - file to use for logging
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
     * Generate header for log file
     *
     * Header contains
     *  - phone model
     *  - android version
     *  - date time
     *  - starting message
     *  remove white space from model name
     *  so if imported into spreadsheet not spread across multiple columns if use space for delimiter
     *  @return logfile header
     */
    private fun getLogFileHeader(): String {
        if (DEB_FUN_START) Log.d(tag, "getLogFileHeader(): " + mContext?.getString(R.string.debug_started) + "\n")

        val dateTime = gUtilsGeneral.getDateTime()
        val model: String = (Build.MODEL).replace(regex = "\\s".toRegex(), replacement = "")
        val androidVerHeader = mContext?.getString(R.string.log_android_ver_header) ?: ""
        val androidVer: String = Build.VERSION.RELEASE
        val headerMessage = mContext?.getString(R.string.status_start_header) ?: ""
        val line1 = "$model $androidVerHeader: $androidVer\n"    //title for header
        val line2 = "$dateTime $headerMessage\n"                 //header date and time
        return line1 + line2
    }

    /**
     * Stop recording outages
     *
     * Called by BroadcastRec() if reached maximum number of records or
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
     * Setup network change broadcast receiver
     *
     * called by [startRecordOutages]
     * implemented in override function onReceive
     * for each internet change records if network is connected or disconnected
     * if internet is connected records type of connection, eg mobile or wifi
     * if the type of connection is wifi, records
     * SSID and frequency of wifi network
     * if reached max number of records stops recording and stops the service
     */
    private fun setupNetworkChangeBroadcastRec() {
        if (DEB_FUN_START) Log.d(tag, "setupNetworkChangeBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        gNetworkReceiver = object : BroadcastReceiver() {

            //receiver for changes to internet connections
            @Suppress("DEPRECATION")
            override fun onReceive(c: Context, intent: Intent) {
                if (DEB_FUN_START) Log.d(tag, "NetworkBroadcastReceiver(): " + mContext?.getString(R.string.debug_started))

                val extras: Bundle = intent.extras ?: return     //get network information to write to log file
                //if nothing there, no point proceeding
                val info = extras.getParcelable<Parcelable>("networkInfo") as NetworkInfo? ?: return //if nothing there, no point proceeding

                val status = when (info.state) {      //used to store status to be written to log file

                    NetworkInfo.State.CONNECTED -> mContext?.getString(R.string.log_internet_connected) + " " + getTypeOfNetwork()

                    NetworkInfo.State.DISCONNECTED -> mContext?.getString(R.string.log_internet_disconnected)

                    else -> mContext?.getString(R.string.log_internet_status_unknown)
                }
                if (gNetworkLastStatus == status) return                   //no change in status, do nothing
                gNetworkLastStatus = status

                val record = "${gUtilsGeneral.getDateTime()} $status\n"     //write the record
                try {
                    gOutputStream.write(record.toByteArray())
                    //check if reached maximum number of records
                    if (gLineCount++ > MAX_FILE_RECORDS) {
                        gOutputStream.write(
                            "${gUtilsGeneral.getDateTime()}  ${mContext?.getString(R.string.error_max_records)} \n".toByteArray())   //yes write message to log file
                        stopRecordingOutages()
                        stopOurService()
                    }
                } catch (e: IOException) {
                    Log.w(tag, "Error writing to file $e")
                }
            }
        }
    }

    /**
     * Get type of network for current internet connection
     *
     * normally wifi or mobile data
     * for wifi add details
     * @return string with type of network and wifi details if network type is wifi
     */
    fun getTypeOfNetwork(): String {
        val cm = mContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo = cm.activeNetworkInfo ?: return mContext.getString(R.string.unknown_network)

        return when (activeNetwork.type) {
            TYPE_MOBILE -> mContext.getString(R.string.via_mobile_data)
            TYPE_WIFI -> "${mContext.getString(R.string.via_wifi)}: ${getWifiInformation(true)}"
            TYPE_BLUETOOTH -> mContext.getString(R.string.via_bluetooth)
            else -> mContext.getString(R.string.unknown_network)
        }
    }

    /**
     * Setup a Wifi change broadcast receiver
     *
     * called by [startRecordOutages]
     * implemented in override function onReceive
     * for each wifi change records an entry in the log file
     * if wifi is enabled records SSID and frequency
     * if reached max number of records stops recording and stops the service
     */
    private fun setupWifiChangeBroadcastRec() {
        if (DEB_FUN_START) Log.d(tag, "setupNWifiChangeBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        gWifiReceiver = object : BroadcastReceiver() {

            //receiver for changes to wifi connections
            override fun onReceive(c: Context, intent: Intent) {
                if (DEB_FUN_START) Log.d(tag, "WifiBroadcastReceiver(): " + mContext?.getString(R.string.debug_started))

                val dateTime = gUtilsGeneral.getDateTime()
                //EXTRA_WIFI_STATE is the lookup key for whether Wi-Fi is enabled, disabled, enabling, disabling, or unknown.
                val status: String = when (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {

                    WifiManager.WIFI_STATE_DISABLED -> mContext!!.getString(R.string.log_wifi_disabled)

                    WifiManager.WIFI_STATE_ENABLED -> mContext!!.getString(R.string.log_wifi_enabled)

                    else -> mContext!!.getString(R.string.log_wifi_status_unknown)
                }
                if (gWifiLastStatus == status) return                   //no change in status, do nothing
                gWifiLastStatus = status
                //set flag to say if wifi is enabled
                val connected: Boolean = (status == mContext.getString(R.string.log_wifi_enabled))
                //write the record, if connected will get SSID and frequency
                val record = "${gUtilsGeneral.getDateTime()}  $status ${getWifiInformation(connected)}\n"
                gOutputStream.write(record.toByteArray())

                //check if reached maximum number of records
                if (gLineCount++ > MAX_FILE_RECORDS) {
                    try {
                        gOutputStream.write("$dateTime ${mContext.getString(R.string.error_max_records)} \n".toByteArray())  //yes write message to log file
                        stopRecordingOutages()
                        stopOurService()
                    } catch (e: IOException) {
                        Log.w(tag, "Error writing to file $e")
                    }
                }
            }
        }
    }

    /**
     * get wifi SSID name and frequency for log file
     *
     * if connected get wifi ssid name and frequency
     * if not connected return ""
     * if ssid name not available return ""
     * if frequency not available return frequency of 0
     * @return ssid name and frequency as a string
     */
    fun getWifiInformation(connected: Boolean): String {
        if (DEB_FUN_START) Log.d(tag, "buildLogFileRecord(): " + mContext?.getString(R.string.debug_started) + "\n")

        return when (connected) {
            true -> {
                val wifiName = getWifiName() ?: mContext!!.getString(R.string.log_no_wifi_name)  //if no wifi set name to "No wifi"
                val wifiFrequency: Int = getWifiFrequency() ?: 0                                         //if no wifi, set frequency to zero
                return "$wifiName Freq(MHz): $wifiFrequency"
            }
            false -> ""
        }
    }

    /**
     * Register a broadcast receiver for any network connectivity changes
     *
     * called by [startRecordOutages]
     */
    private fun registerNetworkChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "registerNetworkChangeRec(): " + mContext?.getString(R.string.debug_started))

        val intentFilter = IntentFilter()
        @Suppress("DEPRECATION")
        intentFilter.addAction(CONNECTIVITY_ACTION)
        try {
            mContext?.registerReceiver(gNetworkReceiver, intentFilter)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * Un-register a network change receiver
     *
     * called by [stopRecordingOutages]
     */

    private fun unRegisterNetworkChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "unregisterNetworkChangeRec(): " + mContext?.getString(R.string.debug_started))

        try {
            mContext?.unregisterReceiver(gWifiReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * Register a broadcast receiver for any Wifi network changes
     *
     * called by [startRecordOutages]
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

    /**
     * Unregister a broadcast receiver for any Wifi network changes
     *
     *  called by [stopRecordingOutages]
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
     * Get SSID for current connected wifi,
     * if unable to get SSID returns null
     *
     * @return SSID
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
     * Get frequency for current connected wifi,
     * if unable to get frequency returns null.
     *
     * @return frequency or null
     */
    @SuppressLint("WifiManagerPotentialLeak")
    @Suppress("DEPRECATION")
    private fun getWifiFrequency(): Int? {
        if (DEB_FUN_START) Log.d(tag, "getFrequency(): " + mContext?.getString(R.string.debug_started))

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
     * Stop foreground service
     */
    fun stopOurService() {
        if (DEB_FUN_START) Log.d(tag, "stopOurService(): " + mContext?.getString(R.string.debug_started))

        val serviceIntent = Intent(mContext, ServiceRecOutages::class.java)
        mContext?.stopService(serviceIntent)
    }

    /**
     * Starts service to record outages
     *
     * passes name of log file to record outages
     * @param file file to be used for logging
     */
    fun startOurService(file: File) {
        if (DEB_FUN_START) Log.d(tag, "startOurService(): " + mContext?.getString(R.string.debug_started))

        val serviceIntent = Intent(mContext, ServiceRecOutages::class.java)
        serviceIntent.putExtra(MyConstants.INTENT_EXTRA_FILENAME, file.toString())
        mContext?.startService(serviceIntent)
    }
}