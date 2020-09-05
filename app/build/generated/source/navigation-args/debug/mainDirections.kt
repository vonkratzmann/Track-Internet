import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mysites.com.au.checkinternetconnection.R

class mainDirections private constructor() {
  companion object {
    fun actionGlobalFragmentHelp(): NavDirections =
        ActionOnlyNavDirections(R.id.action_global_FragmentHelp)
  }
}
