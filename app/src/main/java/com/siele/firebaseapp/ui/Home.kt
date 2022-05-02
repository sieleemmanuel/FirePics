package com.siele.firebaseapp.ui

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.siele.firebaseapp.R
import com.siele.firebaseapp.adapters.PicAdapter
import com.siele.firebaseapp.databinding.FragmentHomeBinding
import com.siele.firebaseapp.databinding.PictureDialogBinding
import com.siele.firebaseapp.databinding.PictureItemBinding
import com.siele.firebaseapp.di.AppModule
import com.siele.firebaseapp.interfaces.ViewClickListener
import com.siele.firebaseapp.model.Picture
import com.siele.firebaseapp.utils.Constants
import com.siele.firebaseapp.utils.SetUpToolbar
import com.siele.firebaseapp.viewmodels.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class Home : Fragment(), ViewClickListener {
    private val TAG = "HOME"
    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth

    //@Inject lateinit var firebaseDb: FirebaseDatabase
    @Inject
    @AppModule.UploadsQualifier
    lateinit var uploadsDbReference: DatabaseReference

    @Inject
    @AppModule.UpvotesQualifier
    lateinit var upVotesDbReference: DatabaseReference

    @Inject
    @AppModule.ViewsQualifier
    lateinit var viewsDbReference: DatabaseReference
    private lateinit var picAdapter: PicAdapter
    private lateinit var userId: String
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        userId = FirebaseAuth.getInstance().uid!!
        val user = FirebaseAuth.getInstance().currentUser?.email
        sharedViewModel.getPictures()
        picAdapter = PicAdapter(
            rvItemClickListener(), this,
            setUpVoteStatus(), requireContext()
        )

        getPicturesHandler {
            picAdapter.submitList(it)
            binding.pbLoadingPictures.visibility = View.GONE
        }
        binding.apply {
            SetUpToolbar.setUpToolbar(homeToolbar, this@Home, activity as AppCompatActivity)
            fabOpenCamera.setOnClickListener {
                openCamera()
            }
            rvPictures.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = picAdapter
            }
        }
        return binding.root
    }


    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.CAMERA_CODE -> {
                val pic: Bitmap = data?.extras?.get("data")!! as Bitmap
                optionsDialog(pic)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context!!, "Write permission denied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Write permission granted", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.actionSearch)
        val searchView = (searchItem.actionView) as androidx.appcompat.widget.SearchView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            searchView.isIconfiedByDefault
        } else {
            searchView.isIconified = true
        }
        performSearch(searchView)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                signOut()
                true
            }
            R.id.actionSavedPic -> {
                findNavController().navigate(R.id.action_home2_to_pendingUploads)
                true
            }
            R.id.actionLogout -> {
                signOut()
                true
            }
            else -> false
        }

    }

    override fun onItemClickListener(
        position: Int,
        imageButton: ImageButton,
        binding: PictureItemBinding,
        picture: Picture
    ) {
        when (imageButton.id) {
            R.id.ibtnUpvote -> {
                upVotesDbReference.child(picture.picId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(userId)) {
                                upVotesDbReference.child(picture.picId).child(userId)
                                    .removeValue()
                                imageButton.setImageResource(R.drawable.ic_upvote_unchecked)
                                uploadsDbReference.child(picture.picId)
                                    .child("upVotes").setValue(snapshot.childrenCount.minus(1))
                            } else {
                                imageButton.setImageResource(R.drawable.ic_upvote)
                                upVotesDbReference.child(picture.picId).child(userId)
                                    .setValue(true)
                                binding.ibtnDownvote.setImageResource(R.drawable.ic_downvote_unchecked)
                                uploadsDbReference.child(picture.picId)
                                    .child("upVotes").setValue(snapshot.childrenCount.plus(1))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            uploadsDbReference.removeEventListener(this)
                        }
                    })

            }
            R.id.ibtnDownvote -> {
                upVotesDbReference.child(picture.picId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(userId)) {
                                upVotesDbReference.child(picture.picId).child(userId).removeValue()
                                imageButton.setImageResource(R.drawable.ic_downvote)
                                binding.ibtnUpvote.setImageResource(R.drawable.ic_upvote_unchecked)
                                uploadsDbReference.child(picture.picId)
                                    .child("upVotes").setValue(snapshot.childrenCount.minus(1))
                            } else {
                                imageButton.setImageResource(R.drawable.ic_downvote_unchecked)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            upVotesDbReference.removeEventListener(this)
                        }

                    })
            }
        }
    }

    private fun rvItemClickListener() = PicAdapter.ItemClickListener { picture, position ->
        getPicturesHandler { pictures ->
            findNavController().navigate(
                HomeDirections.actionHome2ToViewPicture(
                    pictures[position], position
                )
            )
        }

        viewsDbReference.child(picture.picId)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChild(userId)) {
                        viewsDbReference.child(picture.picId).child(userId)
                            .setValue(true)
                        uploadsDbReference.child(picture.picId)
                            .child("views").setValue(snapshot.childrenCount)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun setUpVoteStatus() = PicAdapter.SetUpVoteStatus { itemBinding, picture ->
        upVotesDbReference.child(picture.picId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val votesCount = snapshot.childrenCount
                    if (snapshot.hasChild(userId)) {
                        itemBinding.apply {
                            ibtnUpvote.setImageResource(R.drawable.ic_upvote)
                            tvUpvotesCount.text = votesCount.toString()
                        }
                    } else {
                        itemBinding.apply {
                            ibtnUpvote.setImageResource(R.drawable.ic_upvote_unchecked)
                            tvUpvotesCount.text = votesCount.toString()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }

            })
        viewsDbReference.child(picture.picId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val viewsCount = snapshot.childrenCount
                    if (snapshot.hasChild(userId)) {
                        itemBinding.apply {
                            ibtnView.setImageResource(R.drawable.ic_views)
                            tvViewsCount.text = viewsCount.toString()
                        }
                        uploadsDbReference.child(picture.picId).child("views")
                            .setValue(viewsCount)
                    } else {
                        itemBinding.apply {
                            ibtnView.setImageResource(R.drawable.ic_views_unchecked)
                            tvViewsCount.text = viewsCount.toString()
                        }
                        uploadsDbReference.child(picture.picId).child("views")
                            .setValue(viewsCount)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun getPicturesHandler(picHandler: (pics: List<Picture>) -> Unit) {
        sharedViewModel.pictures.observe(viewLifecycleOwner, { pics ->
            picHandler.invoke(pics)
        })
    }

    private fun checkPermission(permission: String, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return when {
                ContextCompat.checkSelfPermission(
                    context!!,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> true
                shouldShowRequestPermissionRationale(permission) -> {
                    showPermissionDialog(permission, requestCode)
                    false
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(permission),
                        requestCode
                    )
                    false
                }
            }
        }
        return false
    }

    private fun showPermissionDialog(permission: String, requestCode: Int) {
        MaterialAlertDialogBuilder(context!!)
            .setTitle("Permission Request")
            .setMessage("Write permission is required to save the picture")
            .setPositiveButton("ALLOW") { _, _ ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun optionsDialog(pic: Bitmap) {
        val dialogBinding: PictureDialogBinding = PictureDialogBinding
            .inflate(LayoutInflater.from(requireContext()))
        dialogBinding.apply {
            etPictureName.requestFocus()
            ivPicture.setImageBitmap(pic)

            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setCancelable(false)
                .setPositiveButton("Upload") { d, _ ->
                    if (etPictureName.text.isNotEmpty()) {
                        uploadPicture(pic, "${etPictureName.text}")
                    } else {
                        etPictureName.apply {
                            error = "Name is required!"
                            requestFocus()
                        }
                    }
                }
                .setNegativeButton("Cancel") { d, _ ->
                    d.dismiss()
                }
                .setNeutralButton("Save") { _, _ ->
                    val picFileName = "${etPictureName.text}.png"
                    if (etPictureName.text.toString().isNotEmpty()) {
                        savePicture(pic, picFileName)
                    } else {
                        etPictureName.apply {
                            error = "Picture name is required"
                            requestFocus()
                        }
                    }
                    val picFile = File(context!!.filesDir, picFileName)
                    Log.d("Home", "optionsDialog:${picFile.name} ")
                    if (picFile.exists()) {
                        Toast.makeText(
                            context,
                            "${getPictureUri(picFile)}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("Home", "optionsDialog:${getPictureUri(picFile)} ")
                    }

                }
                .show()
        }


    }

    private fun uploadPicture(pic: Bitmap, picName: String) {
        lifecycleScope.launch {
            sharedViewModel.uploadPicture(pic, userId, picName, requireContext())
        }

    }

    private fun savePicture(pic: Bitmap, picName: String) {
        if (checkPermission(WRITE_EXTERNAL_STORAGE, Constants.WRITE_REQUEST_CODE)) {
            sharedViewModel.savePic(picName, pic, requireContext())
        }
    }

    private fun getPictureUri(picFile: File): Uri? {
        if (checkPermission(READ_EXTERNAL_STORAGE, Constants.READ_REQUEST_CODE)) {
            return FileProvider.getUriForFile(
                requireContext(),
                "com.siele.firebaseapp.fileprovider",
                picFile
            )
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)
        startActivityForResult(cameraIntent, Constants.CAMERA_CODE)
    }

    private fun signOut() {
        auth = FirebaseAuth.getInstance()
        findNavController().navigate(R.id.action_home2_to_login)
        auth.signOut()

    }

    private fun performSearch(searchView: SearchView) {
        getPicturesHandler {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val filteredPics = sharedViewModel.searchFilter(query!!, it)
                    return if (filteredPics.isNotEmpty()) {
                        picAdapter.submitList(null)
                        picAdapter.submitList(filteredPics)
                        true
                    } else {
                        picAdapter.submitList(null)
                        false
                    }

                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    val filteredPics = sharedViewModel.searchFilter(newText!!, it)
                    return if (filteredPics.isNotEmpty()) {
                        picAdapter.submitList(null)
                        picAdapter.submitList(filteredPics)
                        true
                    } else {
                        picAdapter.submitList(null)
                        false
                    }
                }

            })
        }
    }

}