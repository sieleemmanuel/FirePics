package com.siele.firebaseapp.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siele.firebaseapp.model.Picture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
class PendingUploadViewModel:ViewModel() {
    private val _savedPictures = MutableLiveData<List<Picture>>()
    val savedPictures: LiveData<List<Picture>> = _savedPictures

    private suspend fun loadSavedPictures(context: Context): List<Picture> {
        return withContext(Dispatchers.IO) {
            val picFiles: Array<File>? = context.filesDir.listFiles()
            Log.d("PendingUploads", "loadSavedPictures:${picFiles?.size}")
            val pics = picFiles?.filter {
                it.path.endsWith(".jpg") || it.path.endsWith(".png")
            }?.map {
                Picture(pictureUri = it.path, name = it.name)
            }
            pics!!
        }
    }
    fun getSavedPics(context: Context){
        viewModelScope.launch {
         _savedPictures.postValue(loadSavedPictures(context))
        }
    }
    fun removePicture(pictureUri:String){
        try {
            val picFile = File(pictureUri)
            picFile.delete()
        }catch (exception:FileSystemException){
            exception.printStackTrace()
        }

    }

    fun getPictureBitmap(pictureUri:Uri, context: Context): Bitmap? {
        return  try {
            val stream = context.contentResolver.openInputStream(Uri.fromFile(File(pictureUri.toString())))
            val picBitmap = BitmapFactory.decodeStream(stream)
           picBitmap
        }catch (exception:FileSystemException){
            exception.printStackTrace()
            throw IOException("No such file")
        }

    }
}