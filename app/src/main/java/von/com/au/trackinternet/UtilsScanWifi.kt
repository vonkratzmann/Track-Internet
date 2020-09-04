package von.com.au.trackinternet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import mysites.com.au.checkinternetconnection.R
import java.util.*

/**
 * Class for scanning wifi networks
 * number of utility functions
 */

class UtilsScanWifi(val mContext: Context?) {
    private val tag = javaClass.simpleName          //used for debugging in Logcat
    private lateinit var wifiManager: WifiManager
    private lateinit var wiFiScanReceiver: BroadcastReceiver
    private var gArrayList = ArrayList<String>()  //create empty list
    private lateinit var gAdapter: ArrayAdapter<String>
    private lateinit var gListView: ListView

    /**
     * Scan Wifi Networks
     *
     * clear ArrayList ready for new results
     * setup wifi manager
     * set up broadcast receiver
     * register listener as registering and unregistering done for each call to scan
     * scans done infrequently as they have to be initiated by the user
     * start the scan
     * if scan successful, [mWiFiScanReceiver()] uses the callback method [onReceive()]  to retrieve the results
     * [onReceive()] needs to unregister the listener on successful scan
     * if scan unsuccessful, unregister listener, call scanFailure()
     * @param view where to display the results
     *@return status of scan
     */
    @Suppress("DEPRECATION")
    fun scanWifiNetworks(view: View) {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "scanWifiNetworks(): " + mContext?.getString(R.string.debug_started))

        gArrayList.clear()
        gListView = view.findViewById(R.id.listview)
        gAdapter = ArrayAdapter(mContext!!, android.R.layout.simple_list_item_1, gArrayList)
        gListView.adapter = gAdapter

        Toast.makeText(mContext, mContext.getString(R.string.status_scanning), Toast.LENGTH_SHORT).show()

        //wifi manager must be on applicationContext
        wifiManager = mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        //set up receiver to be called from listener and register listener
        setUpWfiScanBroadcastRec()
        registerWifiScanRec()
        //start scanning
        val status: Boolean = wifiManager.startScan()

        if (!status) {
            unregisterWifiScanRec()
            scanFailure()
        }
    }

    /**
     * Setup a wifi scan broadcast receiver
     *
     * Set up broadcast receiver for WifiScan
     * check for success or failure and
     * call appropriate functions
     * once finished unregister the receiver
     */
    private fun setUpWfiScanBroadcastRec() {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "wifiBroadcastRec(): " + mContext?.getString(R.string.debug_started))

        wiFiScanReceiver = object : BroadcastReceiver() {       //set up the receiver
            override fun onReceive(c: Context, intent: Intent) {
                if (MyDebug.DEB_FUN_START) Log.d(tag, "wifi onRec(): " + mContext?.getString(R.string.debug_started))

                if ((intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)))
                    scanSuccess()
                else
                    scanFailure()
                //now have results unregister receiver
                unregisterWifiScanRec()
            }
        }
    }

    /**
     * Process scan success
     *
     * Tell user successful
     * get results & put into array
     * convert SSID to uppercase before sort
     * otherwise will present uppercase sorted first, then lowercase sorted
     * then sort loaded array
     * notify list adapter data changed
     */
    private fun scanSuccess() {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "scanSuccess(): " + mContext?.getString(R.string.debug_started))

        Toast.makeText(mContext, mContext?.getString(R.string.status_scan_success), Toast.LENGTH_SHORT).show()
        val mResults: List<ScanResult> = wifiManager.scanResults
        var size: Int = mResults.size

        try {
            while (size > 0) {
                size--
                val ssid = mResults[size].SSID
                val bssid = mResults[size].BSSID
                val capabilities = mResults[size].capabilities
                val level = mResults[size].level
                val frequency = mResults[size].frequency
                gArrayList.add(
                    "ssid: " + ssid.toUpperCase(Locale.getDefault()) + " bssid: " + bssid + " capabilities: "
                            + capabilities + " level: " + level + " frequency: " + frequency
                )
            }
            gArrayList.sort()
            gAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.w("WifScanner", "Exception: $e")
        }
    }

    /**
     * Process scan failure
     *
     * Tell user unsuccessful
     */
    fun scanFailure() {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "scanFailure(): " + mContext?.getString(R.string.debug_started))

        Toast.makeText(mContext, "Scan Failure", Toast.LENGTH_SHORT).show()
    }

    /**
     * Register a wifi scan receiver
     *
     * Set up intent for scan and then register receive
     */
    private fun registerWifiScanRec() {
        if (MyDebug.DEB_FUN_START) Log.d(tag, "registerWifiScanRec(): " + mContext?.getString(R.string.debug_started))

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        mContext?.registerReceiver(wiFiScanReceiver, intentFilter)
    }

    /**
     * Unregister a wifi scan receiver
     *
     * Unregister receiver
     */
    private fun unregisterWifiScanRec() {
        if (MyDebug.DEB_FUN_START) Log.d(tag,
            "unregisterWifiScanRec(): " + mContext?.getString(R.string.debug_started))

        try {
            mContext?.unregisterReceiver(wiFiScanReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}