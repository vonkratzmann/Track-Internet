package von.com.au.trackinternet

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mainDirections
import mysites.com.au.checkinternetconnection.R

class FragmentDisplayWifiScanDirections private constructor() {
  companion object {
    fun actionFragmentWifiScanToMain(): NavDirections =
        ActionOnlyNavDirections(R.id.action_Fragment_Wifi_Scan_To_Main)

    fun actionGlobalFragmentHelp(): NavDirections = mainDirections.actionGlobalFragmentHelp()
  }
}
