package com.sanwal.thetransformationapp


import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.rotationMatrix
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class VideoViewModel :ViewModel(){

//
//    suspend fun loadPhotosFromInternalStorage(fileDirectory:File): List<InternalStoragePhoto>{
//        return withContext(Dispatchers.IO){
//            val files = fileDirectory.listFiles()
//            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg")}?.map {
//                  Log.d("sanchit", "loadPhotosFromInternalStorage: ${it.path}")
//                //val bytes = it.()
//                val uri = Uri.fromFile(it)
//                var matrix  = Matrix()
//                matrix.postRotate(90F)
////                Log.d("sanchit", "loadPhotosFromInternalStorage: $uri")
////                var myBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
////                myBitmap = Bitmap.createScaledBitmap(myBitmap,1000,1000, false)
//////                val bmp = Bitmap.createBitmap(myBitmap,0,0,600,900,matrix,true)
//              //  val bmp = BitmapFactory.decodeFile(it.path)
//
//                val options = BitmapFactory.Options()
//                    options.inMutable = true
//                    options.outHeight = 900
//                    options.outWidth = 600
//                var bmp = BitmapFactory.decodeFile(it.path, options)
//                bmp = Bitmap.createBitmap(bmp,0,0,3000,3000,matrix,true)
//                matrix = Matrix()
//                bmp = Bitmap.createBitmap(bmp,0,0,600,900,matrix,false)
//                val imageHeight: Int = options.outHeight
//                val imageWidth: Int = options.outWidth
//                val imageType: String? = options.outMimeType
//                Log.d("sanchit", "loadPhotosFromInternalStorage: $imageHeight , $imageWidth , $imageType")
//
////                val source = ImageDecoder.createSource(it)
////                val bmp = ImageDecoder.decodeBitmap(source)
//
////                bmp.isMutable
////                val  myBitmap = Bitmap.createScaledBitmap(bmp,600,900, false)
//
//                InternalStoragePhoto(it.name, bmp, uri)
//            }?: listOf()
//        }
//    }

    val imageFilePaths = mutableListOf<String>()

    fun getInternalStoragePhotoFromFilePaths(): List<InternalStoragePhoto?> {
        Log.d("sanchit", "getInternalStoragePhotoFromFilePaths: ")
        return imageFilePaths.map { setPic(it) }
    }
    private fun setPic(currentPhotoPath:String): InternalStoragePhoto? {
        // Get the dimensions of the View
        val targetW: Int = 600
        val targetH: Int = 900

        val bmOptions = BitmapFactory.Options().apply {
            rotationMatrix(180f)
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            val photo = bitmap.rotate(90f)
            return InternalStoragePhoto(currentPhotoPath, Bitmap.createScaledBitmap(photo, 600, 900, false))
        }
        return null
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}