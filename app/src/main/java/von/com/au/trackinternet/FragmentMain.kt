package von.com.au.trackinternet

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.SharedPreferences
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mysites.com.au.checkinternetconnection.*
import von.com.au.trackinternet.MyDebug.DEB_FRAG_MAIN
import von.com.au.trackinternet.MyDebug.DEB_FUN_START
import java.io.File

/**
 * Fragment subclass as the default destination in the navigation.
 */
class FragmentMain : Fragment() {
    //had to change name to tag1 as compiler had clash with FragmentDisplayOutages tag
    private val tag1 = javaClass.simpleName             //used for debugging in Logcat
    private lateinit var gSharedPref: SharedPreferences
    private lateinit var gScanWifi: UtilsScanWifi               //class for scanning wifi
    private lateinit var gUtilsGeneral: UtilsGeneral            //class for general utilities
    private lateinit var gUtilsRecordOutages: UtilsRecordOutages     //class for recording outages


    /** onCreate()
     *
     * sets up default shared preference file
     *  - used to pass file name and operations request to class FragmentDisplayOutages
     *  instantiate classes
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gSharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!
        //create classes
        gUtilsRecordOutages = UtilsRecordOutages(this.activity?.applicationContext)
        gScanWifi = UtilsScanWifi(this.activity?.applicationContext)
        gUtilsGeneral = UtilsGeneral(this.activity?.applicationContext)
    }

    /**
     * onCreateView()
     *
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (DEB_FUN_START) Log.d(tag1, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    /**
     * onViewCreated(view: View, savedInstanceState: Bundle?)
     *
     * instantiate classes for [UtilsRecordOutages]  [UtilsScanWifi] [UtilsGeneral]
     * set up button onClick listeners
     * get saved filename and email address from shared preferences,
     * update edittext views
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (DEB_FUN_START) Log.d(tag1, "onViewCreated(): " + getString(R.string.debug_started))

        //find the buttons and setup click listeners

        /* button to email log file to supplied email address */
        view.findViewById<Button>(R.id.button_email_outages).setOnClickListener {
            checkSendEmail(view)
        }

        /* button to start recording outages */
        view.findViewById<Button>(R.id.button_record_outages).setOnClickListener {
            recordOutages(view)
        }

        /* button to stop recording outages */
        view.findViewById<Button>(R.id.button_stop_recording).setOnClickListener {
            Toast.makeText(context, getString(R.string.status_stop_header), Toast.LENGTH_LONG).show()
            gUtilsRecordOutages.stopOurService()
        }

        /* button to display outages recorded in file */
        view.findViewById<Button>(R.id.button_display_outages).setOnClickListener {
            displayOutages(view)
        }

        /* button to scan and then display wifi network */
        view.findViewById<Button>(R.id.button_scan_wifi).setOnClickListener {
            scanWifi()
        }

        //get saved filename and email address from shared preferences, update edittext views
        var editText: EditText = view.findViewById(R.id.edittext_filename)
        editText.setText(getLogFileNameFromSharedPref())
        //add listener after edittext updated
        addTextChangeListenerLogFile(editText)

        editText = view.findViewById(R.id.edittext_emailaddr)
        editText.setText(getEmailAddrFromSharedPref())
        //add listener after edittext updated
        addTextChangeListenerEmailAddr(editText)
    }

    /**
     * onStart()
     *
     * does a number of checks:
     *  - checks external storage available
     *  - as recorded outages file stored on external storage
     *  - if not terminates app
     * checks if wifi enabled
     *  - if not enabled terminates the application
     *  - if not given terminates app
     */
    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (DEB_FUN_START) Log.d(tag1, "onStart(): " + getString(R.string.debug_started))

        //check external storage available, if not kill app
        if (!gUtilsGeneral.checkExtStorageForRW()) {
            MainActivity().killApp(getString(R.string.error_storage_not_accessible))
        }

        //check if wifi enabled, if not kill app
        val wifiManager: WifiManager = context?.applicationContext
            ?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifi: Boolean = wifiManager.isWifiEnabled
        if (DEB_FRAG_MAIN) Log.d(tag, "WifiStatus: $wifi")

        if (!wifi) {
            if (MyDebug.DEB_ACT_MAIN) Log.d(tag, getString(R.string.error_wifi_disabled))
            MainActivity().killApp(getString(R.string.error_wifi_disabled))
        }
    }

    /**
     * onPause()
     * do not unregister broadcast receivers here as they are used by foreground service
     * In foreground service onDestroy() calls stopRecording()
     * which unregisters the broadcast receivers
     */
    override fun onPause() {
        super.onPause()
        if (DEB_FUN_START) Log.d(tag1, "onPause(): " + getString(R.string.debug_started))
    }

    /**
     * getLogFileNameFromSharedPref()
     */
    private fun getLogFileNameFromSharedPref(): String? {
        if ((DEB_FUN_START)) Log.d(tag1, "getLogFileNameFromSharedPref(): " + getString(R.string.debug_started))

        val defaultValue = getString(R.string.pref_key_log_file_default)
        val myFileName: String? = gSharedPref.getString(getString(R.string.pref_key_log_file), defaultValue)
        if (DEB_FRAG_MAIN) Log.d(tag1, "Log file name from preferences: $myFileName")

        return myFileName
    }

    /**
     * getEmailAddrFromSharedPref()
     */
    private fun getEmailAddrFromSharedPref(): String? {
        if (DEB_FUN_START) Log.d(tag1, "getEmailAddrFromSharedPref() : " + getString(R.string.debug_started))

        val defaultValue = resources.getString(R.string.pref_key_email_default)
        val myEmailAddress: String? = gSharedPref.getString(getString(R.string.pref_key_email), defaultValue)
        if (DEB_FRAG_MAIN) Log.d(tag1, "Email address from preferences: $myEmailAddress")

        return myEmailAddress
    }

    /**
     * recordOutages()
     * get filename from view
     * check a name has been entered,
     *   if not message user and return
     * check if file exists
     * if so prompt user to delete the file before proceeding
     * starts the service which will record the outages
     * Note: service can be also started in the dialog box
     */
    private fun recordOutages(view: View) {
        if (DEB_FUN_START) Log.d(tag1, "recordOutages(): " + getString(R.string.debug_started))

        val fileName: String = gUtilsGeneral.getFileName(view)

        if (fileName.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_no_filename_entered), Toast.LENGTH_SHORT).show()
            return
        }
        //check if file exists, yes show dialog box to see if it is to be deleted
        val file: File = gUtilsGeneral.getFilePathName(view)
        if (file.exists())
            checkIfDeleteFileDialog(file)     //service also started in dialog box
        else
            gUtilsRecordOutages.startOurService(file)     //file does not exist, so need for dialog, just start the service
    }

    /**
     * checkIfDeleteFileDialog(file: File)
     *
     * show alert dialog
     */
    private fun checkIfDeleteFileDialog(file: File) {
        if (DEB_FUN_START) Log.d(tag1, "checkIfDeleteFile(): " + getString(R.string.debug_started))

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(requireContext())

        // Set a title for alert dialog
        builder.setTitle(getString(R.string.dialog_file_exists))

        // Set a message for alert dialog
        builder.setMessage(getString(R.string.log_delete_file))
        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    //delete file before we start
                    file.delete()
                    gUtilsRecordOutages.startOurService(file)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    gUtilsRecordOutages.startOurService(file)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    return@OnClickListener
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton(getString(R.string.yes), dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton(getString(R.string.no), dialogClickListener)

        // Set the alert dialog cancel button
        builder.setNeutralButton(getString(R.string.cancel), dialogClickListener)

        // Initialize the AlertDialog using builder object
        val dialog: AlertDialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    /**
     * displayOutages()
     *
     * get filename
     * check a name has been entered,
     *   if not message user and return
     * check we can create the file from path and filename
     * *   if not message user and return
     * pass file name to shared preferences
     *   to be used by next fragment
     * navigate to FragmentDisplayOutages to process the request
     */
    private fun displayOutages(view: View) {
        if (DEB_FUN_START) Log.d(tag1, "displayOutages(): " + getString(R.string.debug_started))

        val fileName: String = gUtilsGeneral.getFileName(view)

        if (fileName.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_no_filename_entered), Toast.LENGTH_SHORT).show()
            return
        }
        if (gUtilsGeneral.checkFileAccessible(fileName)) {
            //add filename to shared preferences
            with(gSharedPref.edit()) {
                this?.putString(getString(R.string.pref_key_log_file), fileName)
                commit()
            }
            //Navigate to FragmentDisplayOutages
            findNavController().navigate(R.id.action_Fragment_Main_to_Display_Outages)
        }
    }

    /**
     * scanWifi(view: View)
     *
     */
    private fun scanWifi() {
        if (DEB_FUN_START) Log.d(tag1, "scanWifi(): " + getString(R.string.debug_started))

        //Navigate to FragmentDisplayWifiScan
        findNavController().navigate(R.id.action_Fragment_Main_to_Display_Wifi_Scan)
    }

    /**
     * checkSendEmail(view: View)
     *
     * check valid email address
     * if not tell user and exit
     * otherwise send email with
     *  - email address
     *  - subject
     *  - name of file to attach
     */
    private fun checkSendEmail(view: View) {
        if (DEB_FUN_START) Log.d(tag1, "checkSendEmail(): " + getString(R.string.debug_started))

        val viewEditEmailAddr = view.findViewById<EditText>(R.id.edittext_emailaddr)
        val emailAddress: String = viewEditEmailAddr.text.toString()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(context, getString(R.string.error_invalid_email_address), Toast.LENGTH_SHORT).show()
            return
        }
        sendEmail(
            address = emailAddress,
            subject = getString(R.string.email_subject),
            file = gUtilsGeneral.getFilePathName(view))
    }

    /**
     * sendEmail(addresses: String, subject: String, file: File)
     *
     * Sends an email with provided address and subject
     * Attaches the file
     * Client has to select email client if multiple clients
     * Assumes it is a valid email address
     * toDo() change so user does not have to select email client
     */
    @Suppress("DEPRECATION")
    private fun sendEmail(address: String, subject: String, file: File) {
        if (DEB_FUN_START) Log.d(tag1, "sendEmail: " + getString(R.string.debug_started))

        if (DEB_FRAG_MAIN) Log.d(tag1, "Email attachment: $file")

        /* In Android N, "file:// not allowed to attach to intent, so code 'val path = Uri.fromFile(fileLocation)'
         * triggers a FileUriExposedException.
         * to share content of a private file  use a FileProvider.
         */
        val pathURI = FileProvider.getUriForFile(
            requireContext(),
            requireContext().applicationContext.packageName.toString() + ".provider",
            file)

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            data = Uri.parse("mailto:")             // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(EXTRA_STREAM, pathURI)
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
            type = "text/plain"
        }
        startActivity(emailIntent)
    }

    /**
     *  addTextChangeListenerLogFile(editText)
     *
     *  any changes to file name are saved in shared preferences
     */
    private fun addTextChangeListenerLogFile(editText: EditText) {
        if (DEB_FUN_START) Log.d(tag1, "addTextChangeListenerLogFile(): " + getString(R.string.debug_started))

        editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(p0: Editable?) {
                if (DEB_FUN_START) Log.d(tag1, "Filename afterTextChanged(): " + getString(R.string.debug_started))

                //save filename in shared preferences
                val fileName: String = editText.text.toString()
                with(gSharedPref.edit()) {
                    this?.putString(getString(R.string.pref_key_log_file), fileName)
                    commit()
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    /**
     *  addTextChangeListenerEmailAddr(editText)
     *
     *  any changes to email address are saved in shared preferences
     */
    private fun addTextChangeListenerEmailAddr(editText: EditText) {
        if (DEB_FUN_START) Log.d(tag1, "addTextChangeListenerEmailAddr(): " + getString(R.string.debug_started))

        editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(p0: Editable?) {
                if (DEB_FUN_START) Log.d(tag1, "Email afterTextChanged(): " + getString(R.string.debug_started))

                //save email address in shared preferences
                val emailAddr: String = editText.text.toString()
                with(gSharedPref.edit()) {
                    this?.putString(getString(R.string.pref_key_email), emailAddr)
                    commit()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })
    }
}



