package von.com.au.trackinternet

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import mainDirections
import mysites.com.au.checkinternetconnection.R

class FragmentMainDirections private constructor() {
  companion object {
    fun actionFragmentMainToDisplayOutages(): NavDirections =
        ActionOnlyNavDirections(R.id.action_Fragment_Main_to_Display_Outages)

    fun actionFragmentMainToDisplayWifiScan(): NavDirections =
        ActionOnlyNavDirections(R.id.action_Fragment_Main_to_Display_Wifi_Scan)

    fun actionGlobalFragmentHelp(): NavDirections = mainDirections.actionGlobalFragmentHelp()
  }
}
