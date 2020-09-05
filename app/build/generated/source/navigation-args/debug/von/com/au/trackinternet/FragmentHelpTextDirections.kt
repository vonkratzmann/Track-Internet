package von.com.au.trackinternet

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mainDirections
import mysites.com.au.checkinternetconnection.R

class FragmentHelpTextDirections private constructor() {
  companion object {
    fun actionFragmentHelpTextToMain(): NavDirections =
        ActionOnlyNavDirections(R.id.action_FragmentHelpText_To_Main)

    fun actionGlobalFragmentHelp(): NavDirections = mainDirections.actionGlobalFragmentHelp()
  }
}
