package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageCompressor {

    // Max dimensions for photos (good balance between quality and file size)
    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1920
    private const val JPEG_QUALITY = 85 // 85% quality, good compression

    /**
     * Compresses an image from URI and saves it to internal storage
     * Returns the file path of the compressed image
     */
    fun compressImage(
        context: Context,
        imageUri: Uri,
        outputFileName: String
    ): String? {
        try {
            // Read the original image
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Get image orientation from EXIF data
            val rotation = getImageRotation(context, imageUri)

            // Resize the bitmap
            val resizedBitmap = resizeBitmap(originalBitmap, MAX_WIDTH, MAX_HEIGHT)

            // Rotate if needed
            val rotatedBitmap = if (rotation != 0) {
                rotateBitmap(resizedBitmap, rotation.toFloat())
            } else {
                resizedBitmap
            }

            // Save compressed image to internal storage
            val outputFile = File(context.filesDir, outputFileName)
            val outputStream = FileOutputStream(outputFile)

            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()

            // Clean up bitmaps
            if (resizedBitmap != rotatedBitmap) {
                resizedBitmap.recycle()
            }
            if (originalBitmap != rotatedBitmap) {
                originalBitmap.recycle()
            }
            rotatedBitmap.recycle()

            return outputFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate the scaling factor
        val scale = min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        // Don't upscale images
        if (scale >= 1.0f) return bitmap

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getImageRotation(context: Context, imageUri: Uri): Int {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    /**
     * Get the size of a file in bytes
     */
    fun getFileSize(filePath: String): Long {
        return File(filePath).length()
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
}
