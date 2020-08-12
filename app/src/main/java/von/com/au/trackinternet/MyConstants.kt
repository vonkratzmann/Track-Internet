package von.com.au.trackinternet

/**
 * Global constants
 */

object MyConstants {
    /**
     * maximum number of records that can be written to log file
     * arbitrary choice
     * limit file size to 1Mb
     * assumes each record is 100 characters
     */
    const val MAX_FILE_RECORDS: Int = 10_000

    /**
     * Directory in external storage where log file is stored
     */
    const val DIRECTORY: String = "checkInternet"

    /**
     * used when showing error messages
     * show the message, keep the activity visible for this time
     * before killing the app, so the user can see the message
     */
    const val DELAY_KILL = 3000L

    /**
     * name of extra key
     * to pass log file name to intent
     */
    const val INTENT_EXTRA_FILENAME: String = "intent_filename"

    /**
     * name of notification channel for foreground service
     */
    const val NOT_CHANNEL_ID = "notchannelId"

    const val ONGOING_NOTIFICATION_ID = 1

    /**
     * Used to check if have coarse location access permission
     */
    const val PERM_REQUEST_CODE = 10

    /**
     * columns widths and layout for log file
     * (limit SSID column width to 25 chars)
     * Column number where field starts
     * 1          12       21    47   52
     * Date       Time     SSID  Freq Status
     * 2020/01/01 00:00:00 ...   0000 Disconnected
     */
    const val LOG_HEADER_SPACING: Int = 12

}