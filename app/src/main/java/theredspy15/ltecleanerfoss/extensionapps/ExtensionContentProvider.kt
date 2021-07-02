package theredspy15.ltecleanerfoss.extensionapps

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import theredspy15.ltecleanerfoss.MainActivity
import java.util.*

class ExtensionContentProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method.lowercase(Locale.ENGLISH)) {
            WorkType.Clean.id, WorkType.Analyze.id -> {
                launchScan(method)
                null
            }
            WorkType.Open.id -> {
                context?.let { context ->
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                null
            }
            WorkType.GetStatus.id -> {
                bundleOf(KEY_WORK_STATUS to AppExtensionState.Stop.id)
            }
            WorkType.GetPermissionsRequired.id -> {
                bundleOf(KEY_RESULT to context?.let { !isPermissionGranted(it) }?.or(false))
            }
            else -> {
                super.call(method, arg, extras)
            }
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun getType(uri: Uri): String? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    private fun launchScan(workType: String) {
        context?.let { context ->
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(KEY_WORK_STATUS, AppExtensionState.Start.id)
            intent.putExtra(KEY_WORK_TYPE, workType)
            context.startActivity(intent)
        }
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val PREFERENCE_AUTHORITY =
            "com.fulldive.extension.cleaner.FulldiveContentProvider"
        const val BASE_URL = "content://$PREFERENCE_AUTHORITY"
        const val KEY_WORK_STATUS = "work_status"
        const val KEY_WORK_TYPE = "work_type"
        const val KEY_WORK_PROGRESS = "work_progress"
        const val KEY_RESULT = "result"
    }
}

fun getContentUri(value: String): Uri {
    return Uri
        .parse(ExtensionContentProvider.BASE_URL)
        .buildUpon()
        .appendPath(ExtensionContentProvider.KEY_WORK_STATUS)
        .appendPath(value)
        .build()
}

fun getWorkProgressUri(value: String): Uri {
    return Uri
        .parse(ExtensionContentProvider.BASE_URL)
        .buildUpon()
        .appendPath(ExtensionContentProvider.KEY_WORK_PROGRESS)
        .appendPath(value)
        .build()
}