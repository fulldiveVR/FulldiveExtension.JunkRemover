package theredspy15.ltecleanerfoss.extensionapps

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import theredspy15.ltecleanerfoss.MainActivity

class ExtensionContentProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            WorkType.CLEAN.id, WorkType.ANALYZE.id -> {
                launchScan(method)
                null
            }
            WorkType.OPEN.id -> {
                context?.let { context ->
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
                null
            }
            GET_STATUS -> {
                bundleOf(
                    Pair(
                        WORK_STATUS,
                        AppExtensionState.STOP.id
                    )
                )
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
            intent.putExtra(WORK_STATUS, AppExtensionState.START.id)
            intent.putExtra(WORK_TYPE, workType)
            context.startActivity(intent)
        }
    }

    companion object {
        private const val PREFERENCE_AUTHORITY =
            "com.fulldive.extension.cleaner.FulldiveContentProvider"
        const val BASE_URL = "content://$PREFERENCE_AUTHORITY"
        const val WORK_STATUS = "work_status"
        const val GET_STATUS = "GET_STATUS"
        const val WORK_TYPE = "WORK_TYPE"
        const val WORK_PROGRESS = "work_progress"
    }
}

fun getContentUri(value: String): Uri {
    return Uri
        .parse(ExtensionContentProvider.BASE_URL)
        .buildUpon()
        .appendPath(ExtensionContentProvider.WORK_STATUS)
        .appendPath(value)
        .build()
}

fun getWorkProgressUri(value: String): Uri {
    return Uri
        .parse(ExtensionContentProvider.BASE_URL)
        .buildUpon()
        .appendPath(ExtensionContentProvider.WORK_PROGRESS)
        .appendPath(value)
        .build()
}