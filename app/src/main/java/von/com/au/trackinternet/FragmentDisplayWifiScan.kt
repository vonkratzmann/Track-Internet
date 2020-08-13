package von.com.au.trackinternet

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mysites.com.au.checkinternetconnection.R

/**
 * Fragment subclass to display results of a scan for wifi networks
 */
class FragmentDisplayWifiScan : Fragment() {
    //had to change name to tag3 as compiler had clash with FragmentMain tag
    private val tag3 = javaClass.simpleName
    private lateinit var sharedPref: SharedPreferences
    private lateinit var utilsGeneral: UtilsGeneral  //hold general functions to handle files and other things
    private lateinit var utilsScanWifi: UtilsScanWifi  //hold general functions to handle files and other things

    /** onCreate()
     *
     * sets up default shared preference file
     *  - used to pass file name and operations request to class FragmentDisplayOutages
     *  instantiate classes
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreate(): " + getString(R.string.debug_started))

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!
        //create classes
        utilsGeneral = UtilsGeneral(this.activity?.applicationContext)
        utilsScanWifi = UtilsScanWifi(this.activity?.applicationContext)
    }

    /**
     * onCreateView()
     *
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_display_wifi_scan, container, false)
    }

    /**
     * onViewCreated()
     *
     * scan for wifi networks
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onViewCreated(): " + getString(R.string.debug_started))

        utilsScanWifi.scanWifiNetworks(view)
    }

    /**
     * onStart()
     *
     *
     */
    override fun onStart() {
        super.onStart()
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onStart(): " + getString(R.string.debug_started))
    }
}