package von.com.au.trackinternet

/**
 * Constants for enabling disabling Debug log messages
 *
 * debug code in the source is always predicated by an if statement on one of the object properties
 * so can turn logging on and off by setting a property true or false
 * ensure all properties are false before compiling production version
 *
 *  aim is to set up properties to log different parts of the application
 *
 *  each function as a first statement should have a log statement to
 *  log the function has started
 */

object MyDebug {

    //if true used to log a function has started
    const val DEB_FUN_START = false

    //used in debugging of Class UtilsRecOutages
    const val DEB_REC_OUT = false

    //used in debugging of FragmentMain
    const val DEB_FRAG_MAIN = false

    //used in debugging of FragmentDisplayOutages
    const val DEB_FRAG_OUTAGES = false

    //used in debugging of MainActivity
    const val DEB_ACT_MAIN = false

    //used in debugging of class UtilsGeneral
    const val DEB_UTIL_GEN = false
}