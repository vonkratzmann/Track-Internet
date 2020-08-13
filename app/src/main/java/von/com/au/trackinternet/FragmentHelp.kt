package von.com.au.trackinternet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mysites.com.au.checkinternetconnection.R

/**
 * Fragment class for help
 */
class FragmentHelp : Fragment() {
    private val tag3: String = javaClass.simpleName

    private lateinit var gUtilsGeneral: UtilsGeneral       //used for simple utilities

    /** onCreate()
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreate(): " + getString(R.string.debug_started))

        gUtilsGeneral = UtilsGeneral(context)
    }

    /**
     * onCreateView()
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    /**
     * onViewCreated()
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onViewCreated(): " + getString(R.string.debug_started))

        //set up on click listener to go help text
        view.findViewById<ImageView>(R.id.imageView_help).setOnClickListener {
            findNavController().navigate(R.id.action_FragmentHelp_To_FragmentHelpText)
        }
    }
}
