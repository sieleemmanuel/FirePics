package com.siele.firebaseapp.viewmodels


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.siele.firebaseapp.di.AppModule
import com.siele.firebaseapp.model.Picture
import com.siele.firebaseapp.utils.Constants.UPLOAD_REF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class SharedViewModel @Inject constructor(var uploadStorageReference: StorageReference):ViewModel() {
    private val _pictures = MutableLiveData<List<Picture>>()
    val pictures: LiveData<List<Picture>> = _pictures
    @Inject @AppModule.UploadsQualifier
    lateinit var uploadDatabaseReference: DatabaseReference
    private val oAuthProvider: OAuthProvider.Builder = OAuthProvider.newBuilder("twitter.com")

    private val _firebaseUser = MutableLiveData<FirebaseUser?>()
    val firebaseUser:LiveData<FirebaseUser?> = _firebaseUser


    @Inject
    lateinit var auth: FirebaseAuth


    fun getUploadedPictures(pics:List<Picture>) {
        _pictures.value = pics
    }

    fun savePic(
        picName: String,
        pic: Bitmap, context: Context
    ) {
        val fos = context.openFileOutput(picName, Context.MODE_PRIVATE)
        val byteArrayOutputStream = ByteArrayOutputStream()
        pic.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArrayInputStream = byteArrayOutputStream.toByteArray()
        fos.apply {
            write(byteArrayInputStream)
            flush()
            close()
        }
        byteArrayOutputStream.close()
    }

    fun getPictures() {
        val picList = ArrayList<Picture>()
        uploadDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.children.forEach { childrenSnapshot ->
                        picList.add(childrenSnapshot.getValue(Picture::class.java) as Picture)
                        return@forEach
                    }
                    getUploadedPictures(picList.distinct())
                } else {
                    getUploadedPictures(picList.distinct())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                uploadDatabaseReference.removeEventListener(this)
            }
        })

    }

    suspend fun uploadPicture(
        pic: Bitmap,
        userId: String,
        picName: String,
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            pic.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val picData = byteArrayOutputStream.toByteArray()
            uploadStorageReference.child("Pictures/$userId/$picName.png").putBytes(picData)
                .addOnSuccessListener {
                    val ref =
                        FirebaseStorage.getInstance().getReference("Pictures/$userId/$picName.png")
                    ref.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            val uploadReferenceKey = uploadDatabaseReference.push()
                            val picture = Picture(
                                userId = userId,
                                pictureUri = downloadUri.toString(),
                                picId = uploadReferenceKey.key!!,
                                name = "$picName.png",
                            )
                            uploadReferenceKey.setValue(picture)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "UploadSuccessful", Toast.LENGTH_SHORT)
                                        .show()
                                    getPictures()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        context,
                                        "RealTime: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.d("RealTime", exception.message.toString())
                                }
                                .addOnCompleteListener {
                                    getPictures()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                context,
                                "Firestore: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.d("Firestore", exception.message.toString())
                        }
                }
        }
    }

    fun twitterLoginTask(activity:Activity) {
        val pendingResultTask: Task<AuthResult>? = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    Log.d("AuthResult", "${authResult.user}")
                    _firebaseUser.postValue(authResult.user)
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Log.d("TwitterLogin", "error:::${exception.message} ")
                    _firebaseUser.postValue(null)
                }
        } else {
            auth
                .startActivityForSignInWithProvider( activity, oAuthProvider.build() )
                .addOnSuccessListener { authResult ->
                    Log.d("AuthResult", "${authResult.user}")
                    _firebaseUser.postValue(authResult.user)
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Log.d("TwitterLogin", "error:::${exception.message} ")
                    _firebaseUser.postValue(null)
                }
        }
    }

    fun searchFilter(searchString:String, pictures: List<Picture>) = pictures.filter { picture ->
        picture.name.lowercase(Locale.getDefault()).contains(searchString) ||
                picture.userId.lowercase(Locale.getDefault()).contains(searchString)

    }
}