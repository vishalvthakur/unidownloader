package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.InputStream
import java.io.OutputStream

data class StatusMedia(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val size: Long
)

object WhatsAppStatusHelper {

    /**
     * Lists all status files (videos and images) from a given SAF Document Tree Uri using standard DocumentsContract.
     */
    fun getStatusesFromTree(context: Context, treeUri: Uri): List<StatusMedia> {
        val result = mutableListOf<StatusMedia>()
        val resolver = context.contentResolver
        try {
            // Build the children URI using standard DocumentsContract
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idIndex != -1) cursor.getString(idIndex) else continue
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "status_file"
                    val mimeType = if (mimeIndex != -1) cursor.getString(mimeIndex) ?: "" else ""
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L

                    // Skip hidden files if any
                    if (name.startsWith(".")) continue

                    val isVideo = mimeType.startsWith("video/") || name.endsWith(".mp4", ignoreCase = true)
                    val isImage = mimeType.startsWith("image/") || name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)

                    if (isVideo || isImage) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        result.add(
                            StatusMedia(
                                uri = fileUri,
                                name = name,
                                isVideo = isVideo,
                                size = size
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.sortedByDescending { it.name }
    }

    /**
     * Saves a chosen WhatsApp status to the public gallery using MediaStore.
     */
    fun saveStatusToGallery(context: Context, status: StatusMedia): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "status_" + status.name)
            put(MediaStore.MediaColumns.TITLE, "status_" + status.name)
            if (status.isVideo) {
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/UniversalDownloader")
            } else {
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/UniversalDownloader")
            }
        }

        val collectionUri = if (status.isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val targetUri = resolver.insert(collectionUri, contentValues) ?: return false

        return try {
            val inputStream: InputStream? = resolver.openInputStream(status.uri)
            val outputStream: OutputStream? = resolver.openOutputStream(targetUri)
            if (inputStream != null && outputStream != null) {
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                inputStream.close()
                outputStream.close()
                true
            } else {
                resolver.delete(targetUri, null, null)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(targetUri, null, null)
            false
        }
    }
}
