package von.com.au.trackinternet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import mysites.com.au.checkinternetconnection.R
import von.com.au.trackinternet.MyConstants.DIRECTORY
import von.com.au.trackinternet.MyDebug.DEB_FUN_START
import von.com.au.trackinternet.MyDebug.DEB_UTIL_GEN
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Utilities for general operations
 *
 * Just a convenient place to put small functions
 */

class UtilsGeneral(private val mContext: Context?) {
    private val tag = javaClass.simpleName          //used for debugging in Logcat

    /**
     * checkExtStorageForRW()
     *
     * check external storage available for Read/Write
     * @return true if storage accessible
     */
    fun checkExtStorageForRW(): Boolean {
        if (DEB_FUN_START) Log.d(tag, "checkExtStorageRW(): " + mContext?.getString(R.string.debug_started))

        return when (Environment.getExternalStorageState()) {
            Environment.MEDIA_MOUNTED -> true
            Environment.MEDIA_MOUNTED_READ_ONLY -> false
            else -> false
        }
    }

    /**
     * getFileName(view: View)
     *
     * get file name form edit text
     *@return fileName type String
     */
    fun getFileName(view: View): String {
        if (DEB_FUN_START) Log.d(tag, "getFileName): " + mContext?.getString(R.string.debug_started))

        return view.findViewById<EditText>(R.id.edittext_filename).text.toString()
    }

    /**
     * getFilePath()
     *
     * get absolute path to directory on shared external storage
     * @return path as type String
     */
    private fun getFilePath(): File {
        if (DEB_FUN_START) Log.d(tag, "getFilePath(): " + mContext?.getString(R.string.debug_started))

        val s: String = mContext?.getExternalFilesDir(DIRECTORY).toString()
        if (DEB_UTIL_GEN) Log.d(tag, "external dir: $s")
        return File(s)
    }

    /**
     * getFilePathName(mFileName: String)
     *
     * @return file path and name as type File
     */
    fun getFilePathName(mFileName: String): File {
        if (DEB_FUN_START) Log.d(tag, "getFilePathName(): " + mContext?.getString(R.string.debug_started) + "\n")

        //get external directory
        val path = getFilePath()
        return File(path, mFileName)
    }

    /**
     * getFilePathName(view: View)
     *
     * @return file path and name as type File
     */
    fun getFilePathName(view: View): File {
        if (DEB_FUN_START) Log.d(tag, "getFilePathName(): " + mContext?.getString(R.string.debug_started) + "\n")

        //get external directory
        val path: File = getFilePath()
        val fileName: String = getFileName(view)
        return File(path, fileName)
    }

    /**
     * checkFileAccessible(fileName: String)
     *
     * Check if accessible by creating a new file
     * if accessible return true, else false
     * @return true or false
     */
    fun checkFileAccessible(mFileName: String): Boolean {
        if (DEB_FUN_START) Log.d(tag, "checkFileAccessible(): " + mContext?.getString(R.string.debug_started))

        try {
            getFilePathName(mFileName).createNewFile()
        } catch (e: IOException) {   //Unable to create file
            Log.w(tag, "Error writing to file: $mFileName  $e")
            return false
        } catch (f: FileNotFoundException) {
            Log.w(tag, "Error writing to file: $mFileName $f")
            return false
        }
        return true
    }

    /**
     * getDateTime()
     *
     * todo() make formats the same
     * return in format "yyyy MM dd HH:mm:ss
     *
     */
    fun getDateTime(): String {
        if (DEB_FUN_START) Log.d(
            tag,
            "getDateTime(): " + mContext?.getString(R.string.debug_started))

        val dateTime: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd  HH:mm:ss")
                current.format(formatter)
            } else {
                val date = Date()
                val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH)
                formatter.format(date)
            }
        if (MyDebug.DEB_REC_OUT) Log.d(tag, "dateTime: $dateTime")
        return dateTime
    }

    /**
     *
     */
    fun help(view: View) {
        val imageView: ImageView = view.findViewById(R.id.imageView_help)
        val bitMap: Bitmap = BitmapFactory.decodeFile("boys")
        imageView.setImageBitmap(bitMap)

    }
}