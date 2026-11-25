package com.usbscreensaver

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

class UpdateProvider : ContentProvider() {
    companion object {
        const val APK_TYPE = "application/vnd.android.package-archive"
        private val filename = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.apk")
    }

    override fun onCreate() = true
    override fun getType(uri: Uri) = APK_TYPE

    private fun resolve(uri: Uri): File {
        val name = uri.pathSegments.singleOrNull() ?: throw FileNotFoundException()
        if (!filename.matches(name)) throw FileNotFoundException()
        return File(File(requireNotNull(context).cacheDir, "updates"), name)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException()
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): MatrixCursor {
        val file = resolve(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            val row = newRow()
            for (column in columns) row.add(when (column) {
                OpenableColumns.DISPLAY_NAME -> "USB-Screensaver.apk"
                OpenableColumns.SIZE -> file.length()
                else -> null
            })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
