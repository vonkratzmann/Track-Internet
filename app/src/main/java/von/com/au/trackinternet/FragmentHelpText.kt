package von.com.au.trackinternet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mysites.com.au.checkinternetconnection.R

/**
 * Fragment class for help text
 */
class FragmentHelpText : Fragment() {
    private val tag4: String = javaClass.simpleName

    private lateinit var gUtilsGeneral: UtilsGeneral       //used for simple utilities

    /** onCreate()
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag4, "onCreate(): " + getString(R.string.debug_started))

        gUtilsGeneral = UtilsGeneral(context)
    }

    /**
     * onCreateView()
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag4, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help_text, container, false)
    }

    /**
     * onViewCreated()
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag4, "onViewCreated(): " + getString(R.string.debug_started))

        val textView: TextView = view.findViewById(R.id.textView_helptext)
        textView.setText("Hello")
    }
}