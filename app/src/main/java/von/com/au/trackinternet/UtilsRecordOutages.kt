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
import von.com.au.trackinternet.MyDebug.DEB_FUN_START
import von.com.au.trackinternet.MyDebug.DEB_REC_OUT
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
    private lateinit var gBroadcastReceiver: BroadcastReceiver
    private lateinit var gUtilsGeneral: UtilsGeneral       //simple utilities functions

    /* used to track last status, if no change in status from last time does not write a record to log file
     * some phones broadcast wifi changes very close together, but on interrogation the state has not changed
     */
    private var lastStatus: String = mContext!!.getString(R.string.status_network_unknown)

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
        setUpWifiChangeBroadcastRec()
        registerWifiChangeRec()
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
        if (DEB_FUN_START) Log.d(tag, "getColumnNames(: " + mContext?.getString(R.string.debug_started) + "\n")

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
     * Called by onDestroy() in foreground service
     * write a stop header with date time to file
     * unregister listener
     * close stream
     */
    fun stopRecordingOutages() {
        if (DEB_FUN_START) Log.d(tag, "stopRecordingOutages(): " + mContext?.getString(R.string.debug_started))

        val stopHeader: String = gUtilsGeneral.getDateTime() + " " + mContext?.getString(R.string.status_stop_header) + "\n"
        try {
            unRegisterWifiChangeRec()
            gOutputStream.write(stopHeader.toByteArray())
            gOutputStream.close()
        } catch (e: IOException) {
            Log.w(tag, "Error writing to file $e")
        }
    }

    /**
     * setUpWifiChangeBroadcastRec()
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
    private fun setUpWifiChangeBroadcastRec() {
        if (DEB_FUN_START) Log.d(tag, "wifiChangeBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        gBroadcastReceiver = object : BroadcastReceiver() {

            //receiver for changes to internet connections
            @Suppress("DEPRECATION")
            override fun onReceive(c: Context, intent: Intent) {
                if (DEB_FUN_START) Log.d(tag, "BroadcastReceiver(): " + mContext?.getString(R.string.debug_started))

                lateinit var status: String         //used to store status to be written to log file

                val extras: Bundle? = intent.extras     //get network information to write to log file
                val info = extras?.getParcelable<Parcelable>("networkInfo") as NetworkInfo?

                status = when (info!!.state) {
                    NetworkInfo.State.CONNECTED -> {
                        if (DEB_REC_OUT) Log.d(tag, "BroadcastReceiver(): " + mContext?.getString(R.string.log_connected))
                        mContext?.getString(R.string.log_connected) + "\n"
                    }
                    NetworkInfo.State.DISCONNECTED -> {
                        if (DEB_REC_OUT) Log.d(tag, "BroadcastReceiver(): " + mContext?.getString(R.string.log_disconnected))
                        mContext?.getString(R.string.log_disconnected) + "\n"
                    }
                    else -> {
                        if (DEB_REC_OUT) Log.d(tag,
                            "BroadcastReceiver(): " + mContext?.getString(R.string.status_network_unknown))
                        mContext?.getString(R.string.status_network_unknown) + "\n"
                    }
                }
                if (lastStatus == status) return                   //no change in status, do nothing
                lastStatus = status
                //write the record
                gOutputStream.write("${buildLogFileRecord()} $status".toByteArray())
                //check if reached maximum number of records
                if (gLineCount++ > MyConstants.MAX_FILE_RECORDS) {
                    gOutputStream.write("${gUtilsGeneral.getDateTime()} ${mContext?.getString(R.string.error_max_records)} \n"
                        .toByteArray())
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
        val wifiName = getWifiName()
            ?: mContext?.getString(R.string.log_no_wifi)                //if no wifi set name to "No wifi"
        val wifiFrequency: Int = getWifiFrequency() ?: 0                //if no wifi, set frequency to zero
        return "$dateTime $wifiName $wifiFrequency"
    }

    /*
     * registerWifiChangeRec()
     *
     * called by startRecordOutages
     * Register a broadcast receiver for any Wifi network changes
     */
    private fun registerWifiChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "registerWifiChangeRec(): " + mContext?.getString(R.string.debug_started))

        val intentFilter = IntentFilter()
        @Suppress("DEPRECATION")
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        try {
            mContext?.registerReceiver(gBroadcastReceiver, intentFilter)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /*
     * unRegisterWifiChangeRec()
     *
     * called by stopRecordingOutages()
     * unregister a broadcast receiver for any Wifi network changes
     */
    private fun unRegisterWifiChangeRec() {
        if (DEB_FUN_START) Log.d(tag, "unregisterWifiChangeRec(): " + mContext?.getString(R.string.debug_started))

        try {
            mContext?.unregisterReceiver(gBroadcastReceiver)
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
        if (DEB_FUN_START) Log.d(tag,
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