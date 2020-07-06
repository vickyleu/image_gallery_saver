package com.example.imagegallerysaver

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.*


class ImageGallerySaverPlugin(private val registrar: Registrar): MethodCallHandler {

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "image_gallery_saver")
      channel.setMethodCallHandler(ImageGallerySaverPlugin(registrar))
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    when {
        call.method == "saveImageToGallery" -> {
          val image = call.arguments as ByteArray
          result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image,0,image.size)))
        }
        call.method == "saveFileToGallery" -> {
          val path = call.arguments as String
          result.success(saveFileToGallery(path))
        }
        else -> result.notImplemented()
    }

  }

  private fun generateFile(context:Context,extension: String = ""): File {

    val storePath =  Environment.getExternalStorageDirectory() .absolutePath + File.separator + "/DCIM/Camera/"///getApplicationName()
//    val storePath =  (context.getExternalFilesDir(Environment.MEDIA_SHARED)?.absolutePath?:(Environment.getDataDirectory().absolutePath)) + File.separator + getApplicationName()
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    if(!appDir.exists()){
      throw  RuntimeException("请在AndroidManifest.xml Application节点添加android:requestLegacyExternalStorage=\"true\"")
    }
    var fileName = System.currentTimeMillis().toString()
    if (extension.isNotEmpty()) {
      fileName += ("." + extension)
    }
    return File(appDir, fileName)
  }

  private fun saveImageToGallery(bmp: Bitmap): String {
    val context = registrar.activeContext().applicationContext
    if (Build.VERSION.SDK_INT >= 29) {
      val values = ContentValues()
      values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/${getApplicationName()}")
      values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
      values.put(MediaStore.MediaColumns.IS_PENDING, true)
      val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
      if (uri != null) {
        try {
          if (WriteBitmapToStream(bmp, context.contentResolver.openOutputStream(uri))) {
            values.put(MediaStore.MediaColumns.IS_PENDING, false)
            context.contentResolver.update(uri, values, null, null)
          }
          return uri.toString()
        } catch (e: Exception) {
          Log.e("Unity", "Exception:", e)
          context.contentResolver.delete(uri, null, null)
        }
      }
//    } else if (Build.VERSION.SDK_INT >= 27) {
//      val values = ContentValues()
//      values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/${getApplicationName()}")
//      values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
//      values.put(MediaStore.MediaColumns.IS_PENDING, true)
//      val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//      if (uri != null) {
//        try {
//          if (WriteBitmapToStream(bmp, context.contentResolver.openOutputStream(uri))) {
//            values.put(MediaStore.MediaColumns.IS_PENDING, false)
//            context.contentResolver.update(uri, values, null, null)
//          }
//          return uri.toString()
//        } catch (e: Exception) {
//          Log.e("Unity", "Exception:", e)
//          context.contentResolver.delete(uri, null, null)
//        }
//      }
    }else{
      val file = generateFile(context,"png")
      try {
        val fos = FileOutputStream(file)
        bmp.compress(Bitmap.CompressFormat.PNG, 60, fos)
        fos.flush()
        fos.close()
        val uri = Uri.fromFile(file)
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        return uri.toString()
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
    return ""
  }

  private fun WriteFileToStream(file: File, out: OutputStream?): Boolean {
    try {
      if(out==null)return  false
//      val bos = ByteArrayOutputStream()
//      bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
//      val bitmapdata = bos.toByteArray()
//      val `in`= ByteArrayInputStream(bitmapdata)
//      try {
//        val buf = ByteArray(1024)
//        var len: Int
//        while (`in`.read(buf).also { len = it } > 0) out.write(buf, 0, len)
//      } finally {
//        try {
//          `in`.close()
//        } catch (e: Exception) {
//          Log.e("Unity", "Exception:", e)
//        }
//      }
    } catch (e: Exception) {
      Log.e("Unity", "Exception:", e)
      return false
    } finally {
      try {
        out?.close()
      } catch (e: Exception) {
        Log.e("Unity", "Exception:", e)
      }
    }

    return true
  }

  private fun WriteBitmapToStream(bitmap: Bitmap, out: OutputStream?): Boolean {
    try {
      if(out==null)return  false
      val bos = ByteArrayOutputStream()
      bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
      val bitmapdata = bos.toByteArray()
      val `in`= ByteArrayInputStream(bitmapdata)
      try {
        val buf = ByteArray(1024)
        var len: Int
        while (`in`.read(buf).also { len = it } > 0) out.write(buf, 0, len)
      } finally {
        try {
          `in`.close()
        } catch (e: Exception) {
          Log.e("Unity", "Exception:", e)
        }
      }
    } catch (e: Exception) {
      Log.e("Unity", "Exception:", e)
      return false
    } finally {
      try {
        out?.close()
      } catch (e: Exception) {
        Log.e("Unity", "Exception:", e)
      }
    }

    return true
  }

  private fun saveFileToGallery(filePath: String): String {
    val context = registrar.activeContext().applicationContext
    val originalFile = File(filePath)
//    val context = registrar.activeContext().applicationContext
//    if (Build.VERSION.SDK_INT >= 29) {
//      val values = ContentValues()
//      values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/${getApplicationName()}")
//      values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
//      values.put(MediaStore.MediaColumns.IS_PENDING, true)
//      val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//      if (uri != null) {
//        try {
//          if (WriteFileToStream(originalFile, context.contentResolver.openOutputStream(uri))) {
//            values.put(MediaStore.MediaColumns.IS_PENDING, false)
//            context.contentResolver.update(uri, values, null, null)
//          }
//          return uri.toString()
//        } catch (e: Exception) {
//          Log.e("Unity", "Exception:", e)
//          context.contentResolver.delete(uri, null, null)
//        }
//      }
//    }else{
//
//    }
    return try {
      val file = generateFile(context,originalFile.extension)
      originalFile.copyTo(file)

      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      return uri.toString()
    } catch (e: IOException) {
      e.printStackTrace()
      ""
    }
  }

  private fun getApplicationName(): String {
    val context = registrar.activeContext().applicationContext
    var ai: ApplicationInfo? = null
    try {
        ai = context.packageManager.getApplicationInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
    }
    var appName: String
    appName = if (ai != null) {
      val charSequence = context.packageManager.getApplicationLabel(ai)
      StringBuilder(charSequence.length).append(charSequence).toString()
    } else {
      "image_gallery_saver"
    }
    return  appName
  }


}
