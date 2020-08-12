package von.com.au.trackinternet

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import von.com.au.trackinternet.MyDebug.DEB_FRAG_OUTAGES
import mysites.com.au.checkinternetconnection.R
import java.io.IOException
import java.util.*

/**
 * Fragment subclass to display a logs of the outages
 */
class FragmentDisplayOutages : Fragment() {
    //had to change name to tag2 as compiler had clash with FragmentMain tag
    private val tag2 = javaClass.simpleName
    private lateinit var utilsGeneral: UtilsGeneral  //hold general functions to handle files and other things
    private lateinit var sharedPref: SharedPreferences

    /** onCreate()
     *
     * sets up default shared preference file
     *  - used to pass file name and operations request to class FragmentDisplayOutages
     *  instantiate classes
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag2, "onCreate(): " + getString(R.string.debug_started))

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!
        //create classes
        utilsGeneral = UtilsGeneral(this.activity?.applicationContext)
    }

    /**
     * onCreateView()
     *
     */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag2, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_display_outages, container, false)
    }

    /**
     * onViewCreated()
     *
     * get filename from shared preferences
     * display outages
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (MyDebug.DEB_FUN_START) Log.d(tag2, "onViewCreated(): " + getString(R.string.debug_started))

        val defaultValue = resources.getString(R.string.pref_key_log_file_default)
        val fileName: String? = sharedPref.getString(getString(R.string.pref_key_log_file), defaultValue)
        if (DEB_FRAG_OUTAGES) Log.d(tag2, "File name from preferences: $fileName")

        displayOutages(fileName!!, view)
    }

    /**
     * displayOutages(fileName: String?, view: View)
     *
     * gets file name, path
     * assumes validity of file name checked by main fragment
     * display the file in the listView
     */
    private fun displayOutages(fileName: String, view: View) {
        if (MyDebug.DEB_FUN_START) Log.d(tag2, "displayOutages: " + getString(R.string.debug_started))
        val listView: ListView = view.findViewById(R.id.listview)

        try {
            val file = utilsGeneral.getFilePathName(fileName)
            val arrayList = ArrayList<String>()

            //read file
            file.useLines { lines: Sequence<String> -> arrayList.addAll(lines) }
            //setup adapter
            val adapter: ArrayAdapter<String> = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                arrayList)

            listView.adapter = adapter
            adapter.notifyDataSetChanged()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * onStart()
     *
     *
     */
    override fun onStart() {
        super.onStart()
        if (MyDebug.DEB_FUN_START) Log.d(tag2, "onStart(): " + getString(R.string.debug_started))
    }
}