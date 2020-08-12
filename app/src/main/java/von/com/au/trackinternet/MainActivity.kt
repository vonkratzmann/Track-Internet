package von.com.au.trackinternet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.activity_main.*
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.DELAY_KILL
import von.com.au.trackinternet.MyDebug.DEB_FUN_START

/**
 *
 * Aim is to detect and record wifi internet outages
 *
 * Outages are written to a user specified log file on external storage
 *
 * On startup app will terminate if:
 *  - external storage is not available
 *  - wifi disabled
 *
 * It assumes the wifi internet outages are intermittent
 * for each outage a date, time, SSID name, and frequency are written to the file
 * checks file does not exceeded specified number of records in MyConstants
 *
 * User can start and stop recording
 * Each time the recording is started, if file exists
 * user is asked if they want to delete the existing file
 * It is up to the user to manage the files
 *
 * If the file name is changed, the new name will be used the next time recording is started
 *
 * The log file can be emailed to a user
 *
 * Notes:
 *  - The recording runs in the foreground, on the same thread as the main activity
 *  - It will only stop when stopped by the user or one of the quit options in the menu
 *  - The debugging code can be removed from runtime by adjusting the values in MyDebug class to false
 *  - needs access coarse location permission
 */

/**
 * MainActivity:
 * - sets up notification channel required for foreground service
 * - the class for the notification channel is invoked in the manifest
 * - inflates menu on the action bar
 * -check have the coarse location permission which is need for wifi scan
 * - FragmentMain is started automatically by the Navigation Component,
 * - as it is flagged as the start destination
 *
 * FragmentMain
 * - handles button pushes
 * - if record outages button pressed, calls recordOutages()
 * - does a number of checks,
 * - checks if log file exists, prompts user if they want to delete it
 * - then calls startOurService() in UtilsRecordOutages class
 *
 * - startOurService()
 * - sets up the intent
 * - adds name of log file to intent
 * - calls startService() for the OS to start the service
 *
 * When the service starts,  @override onStart() is called
 * - onStart() sets up the notification
 * - calls startRecordOutages()
 * - starts the service in foreground
 *
 * startRecordOutages()
 * - writes a header record to the log file
 * - sets up the broadcast receiver "gBroadcastReceiver", by a call to setUpWifiChangeBroadcastRec()
 * - registers the receiver by a call to registerWifiChangeRec()
 *
 * gBroadcastReceiver
 * - which will be called when there changes to the internet connection status
 * - writes one line record of changes to internet connectivity status to the log file
 *
 */


@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {
    private val tag = javaClass.simpleName                   //used for debugging in Logcat
    private lateinit var gUtilsGeneral: UtilsGeneral                //hold simple utility functions
    private lateinit var gUtilsRecordOutages: UtilsRecordOutages    //holds record outages functions
    private lateinit var appBarConfiguration: AppBarConfiguration   //add navigation support to the action bar

    /**
     *  onCreate()
     *
     *  set up navigation to work with action bar
     *  so title in action bar changes when a fragment is loaded
     *  instantiate utility classes
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (DEB_FUN_START) Log.d(tag, "onCreate(): " + getString(R.string.debug_started))
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        gUtilsGeneral = UtilsGeneral(this)
        gUtilsRecordOutages = UtilsRecordOutages(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp()
                || super.onSupportNavigateUp()
    }

    /**
     * onStart()
     *
     * checks for coarse location permission
     */
    override fun onStart() {
        super.onStart()
        if (DEB_FUN_START) Log.d(tag, "onStart(): " + getString(R.string.debug_started))

        checkPermAccessLocation()
    }

    /**
     * onCreateOptionsMenu()
     *
     * inflate menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (DEB_FUN_START) Log.d(tag, "onCreateOptionsMenu(): " + getString(R.string.debug_started))

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * onOptionsItemSelected(item: MenuItem)
     *
     * Handle action bar (menu) item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (DEB_FUN_START) Log.d(tag, "onOptionsItemSelected(): " + getString(R.string.debug_started))

        return when (item.itemId) {
            R.id.menu_quit_stop_recording -> {
                gUtilsRecordOutages.stopOurService()
                //stopOurService() stops foreground service
                //the foreground service onDestroy() will then call stopRecordingOutages
                //which unregisters broadcast receivers and closes file output stream
                killApp(getString(R.string.app_stopping))
                true

            }
            R.id.menu_quit_keep_recording -> {
                killApp(getString(R.string.app_stopping))
                true
            }
            R.id.menu_help -> {
                //Navigate to FragmentDisplayOutages
                //As not in fragment have to use R.id.nav_host_fragment in content_main.xml to find nav controller
                //uses global navigation action to load help fragment,
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_FragmentHelp)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * checkPermAccessLocation()
     *
     * Check we have permissions to access coarse location
     * this is required for Wifi Scan
     * without permission the scan will not return any results
     * ignores tick box do not show this again as app wifi scan will not work without it
     */
    private fun checkPermAccessLocation() {
        if (DEB_FUN_START) Log.d(tag, "checkPermAccessLocation(): " + getString(R.string.debug_started))

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {            //Permission is not granted, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), MyConstants.PERM_REQUEST_CODE
            )
            // The callback method gets the result of the request.
        }
    }

    /**
     * onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray
     *
     * this callback process request from access coarse location
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (DEB_FUN_START) Log.d(tag, "onRequestPermissionsResult(): " + getString(R.string.debug_started))

        when (requestCode) {
            MyConstants.PERM_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo!
                    killApp(getString(R.string.error_no_permission))
                }
            }
        }
    }

    /**
     * killApp(message: String)
     *
     * Displays app name then the message
     * Delay before killing the app
     * so user can see the message
     *
     * Note had this in class UtilsGeneral but had issue with getting finish() to work
     * so moved it here
     */
    fun killApp(message: String) {
        if (DEB_FUN_START) Log.d(tag, "killApp(): " + getString(R.string.debug_started))

        val appName: String? = getString(R.string.app_name)
        Toast.makeText(this, "$appName  $message", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({
            finish()         //run kill after the delay
        }, DELAY_KILL)
    }
}