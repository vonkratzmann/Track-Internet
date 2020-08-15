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
 * Display a photo which the user has to click on
 * to get help information from a html formatted file
 */
class FragmentHelp : Fragment() {
    private val tag3: String = javaClass.simpleName

    private lateinit var gUtilsGeneral: UtilsGeneral       //used for simple utilities

    /**
     * Instantiates general helper class
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreate(): " + getString(R.string.debug_started))

        gUtilsGeneral = UtilsGeneral(context)
    }

    /**
     * Inflates layout for this fragment
     *
     * @param inflater layoutInflater
     * @param container ViewGroup
     * @param savedInstanceState reference to the Bundle object
     * @return returns the view hierarchy associated with the fragment
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag3, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    /**
     * Set up an click listener to go help text from photo in layout
     *
     * Once pictured clicked, calls findNavController() to navigate to [FragmentHelpText]()
     * to display the help text
     *
     * [displayOutages]() does all the work
     * @param view view where to display the results
     * @param savedInstanceState reference to the Bundle object
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
