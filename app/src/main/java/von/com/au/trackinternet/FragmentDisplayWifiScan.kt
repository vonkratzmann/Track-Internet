package von.com.au.trackinternet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mysites.com.au.checkinternetconnection.R

/**
 * Scans wifi networks and display results on the device
 */
class FragmentDisplayWifiScan : Fragment() {
    //had to change name to tag3 as compiler had clash with FragmentMain tag
    private val tag3 = javaClass.simpleName
    private lateinit var utilsGeneral: UtilsGeneral  //hold general functions to handle files and other things
    private lateinit var utilsScanWifi: UtilsScanWifi  //hold general functions to handle files and other things

    /**
     *  Instantiate helper classes
     *
     *  @param savedInstanceState  reference to the Bundle object
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreate(): " + getString(R.string.debug_started))

        utilsGeneral = UtilsGeneral(this.activity?.applicationContext)
        utilsScanWifi = UtilsScanWifi(this.activity?.applicationContext)
    }

    /**
     * Inflate layout for this fragment
     *
     * @param inflater layoutInflater
     * @param container ViewGroup
     * @param savedInstanceState reference to the Bundle object
     * @return returns the view hierarchy associated with the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreateView(): " + getString(R.string.debug_started))

        return inflater.inflate(R.layout.fragment_display_wifi_scan, container, false)
    }

    /**
     * Calls scan for [scanWifiNetwork]() which does all the work
     *
     * @param view view where to display the results
     * @param savedInstanceState reference to the Bundle object
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onViewCreated(): " + getString(R.string.debug_started))

        utilsScanWifi.scanWifiNetworks(view)
    }
}