package von.com.au.trackinternet

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mainDirections
import mysites.com.au.checkinternetconnection.R

class FragmentHelpDirections private constructor() {
  companion object {
    fun actionFragmentHelpToFragmentHelpText(): NavDirections =
        ActionOnlyNavDirections(R.id.action_FragmentHelp_To_FragmentHelpText)

    fun actionFragmentHelpToMain(): NavDirections =
        ActionOnlyNavDirections(R.id.action_FragmentHelp_To_Main)

    fun actionGlobalFragmentHelp(): NavDirections = mainDirections.actionGlobalFragmentHelp()
  }
}
