package von.com.au.trackinternet

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mainDirections
import mysites.com.au.checkinternetconnection.R

class FragmentDisplayOutagesDirections private constructor() {
  companion object {
    fun actionFragmentDisplayOutagesToMain(): NavDirections =
        ActionOnlyNavDirections(R.id.action_FragmentDisplayOutages_To_Main)

    fun actionGlobalFragmentHelp(): NavDirections = mainDirections.actionGlobalFragmentHelp()
  }
}
