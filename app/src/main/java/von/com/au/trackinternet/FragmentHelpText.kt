package von.com.au.trackinternet

import android.os.Bundle
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import mysites.com.au.checkinternetconnection.R
import java.io.InputStream

/**
 * Displays help text which covers overview of the app and operating instructions
 *
 * loads a html file stored in [assets] folder
 * The file provides a short overview and simple instructions on using the app.
 * Reads the file and loads it directly into scrollable TextView
 */
class FragmentHelpText : Fragment() {
    private val tag4: String = javaClass.simpleName

    /**
     * Inflates layout for this fragment
     *
     * @param inflater layoutInflater
     * @param container ViewGroup
     * @param savedInstanceState reference to the Bundle object
     * @return returns the view hierarchy associated with the fragment
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (MyDebug.DEB_FUN_START) Log.d(tag4, "onCreateView(): " + getString(R.string.debug_started))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help_text, container, false)
    }

    /**
     * Displays help text into a TextView
     *
     * @param view view where to display the help text
     * @param savedInstanceState reference to the Bundle object
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (MyDebug.DEB_FUN_START) Log.d(tag4, "onViewCreated(): " + getString(R.string.debug_started))

        val textView: TextView = view.findViewById(R.id.textView_helptext)
        textView.movementMethod = ScrollingMovementMethod()

        //get the the file containing the help text
        val inputStream: InputStream = requireContext().assets.open("helptext.html")
        val buffer = inputStream.bufferedReader().use { it.readText() }

        //setText(Html.fromHtml(bodyData)) is deprecated after api 24
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textView.text = Html.fromHtml(buffer, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            textView.text = Html.fromHtml(buffer)
        }
    }
}