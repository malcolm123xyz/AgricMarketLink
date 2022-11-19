package mx.mobile.solution.nabia04_beta1.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.*

/**
 * Created by Suleiman on 30-04-2015.
 */
object Utils {

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun copyInputStreamToFile(`in`: InputStream, file: File?) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                out?.close()

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                `in`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun resizeImageFile(stream: InputStream, file: File, reqWidth: Int, reqHeight: Int) {
        copyInputStreamToFile(stream, file)
        val path = file.path
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(path, options)
        if (bitmap == null) {
            Log.i("tag", "bitmap is null")
        }
        var fOut: FileOutputStream? = null
        try {
            fOut = FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
        try {
            fOut!!.flush()
            fOut.close()
            bitmap.recycle()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}