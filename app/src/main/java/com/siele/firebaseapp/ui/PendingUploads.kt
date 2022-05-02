package com.siele.firebaseapp.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.siele.firebaseapp.R
import com.siele.firebaseapp.adapters.PendingUploadsAdapter
import com.siele.firebaseapp.databinding.FragmentPendingUploadsBinding
import com.siele.firebaseapp.model.Picture
import com.siele.firebaseapp.utils.SetUpToolbar
import com.siele.firebaseapp.viewmodels.PendingUploadViewModel
import com.siele.firebaseapp.viewmodels.SharedViewModel
import kotlinx.coroutines.launch

class PendingUploads : Fragment() {
    lateinit var binding: FragmentPendingUploadsBinding
    private lateinit var pendingUploadsAdapter: PendingUploadsAdapter
    private val TAG = "PendingUploads"
    private val pendingUploadViewModel: PendingUploadViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPendingUploadsBinding.inflate(inflater)
        pendingUploadViewModel.getSavedPics(requireContext())
        userId = FirebaseAuth.getInstance().uid!!

        pendingUploadsAdapter =
            PendingUploadsAdapter(currentItemViewClickListener(), requireContext())
        savedPicsHandler {
            pendingUploadsAdapter.submitList(it)
            Log.d(TAG, "onCreateView: $it")
        }
        binding.apply {
            SetUpToolbar.setUpToolbar(
                pendingUploadsToolbar,
                this@PendingUploads,
                activity as AppCompatActivity
            )
            rvSavedPic.layoutManager = LinearLayoutManager(requireContext())
            rvSavedPic.adapter = pendingUploadsAdapter
        }
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            true
        } else {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun currentItemViewClickListener() =
        PendingUploadsAdapter.CurrentItemViewClickListener { picture, view ->
            when (view.id) {
                R.id.btnRemove -> {
                    pendingUploadViewModel.removePicture(picture.pictureUri!!)
                    updateSavedPicList()
                    Toast.makeText(context, "Uri ${picture.pictureUri}", Toast.LENGTH_SHORT).show()
                }
                R.id.btnUploadSaved -> {
                    val picName = picture.pictureUri?.substringAfterLast("/")
                    val bitmap = pendingUploadViewModel.getPictureBitmap(
                        Uri.parse(picture.pictureUri),
                        requireContext()
                    )
                    Toast.makeText(
                        context,
                        "Dimens ${bitmap?.height} X ${bitmap?.width}",
                        Toast.LENGTH_SHORT
                    ).show()
                    lifecycleScope.launch {
                        sharedViewModel.uploadPicture(bitmap!!, userId, picName!!, requireContext())
                    }

                    pendingUploadViewModel.removePicture(picture.pictureUri!!)
                    updateSavedPicList()
                }
            }

        }


    private fun updateSavedPicList() {
        pendingUploadViewModel.getSavedPics(requireContext())
        savedPicsHandler {
            pendingUploadsAdapter.submitList(it)
            if (it.isEmpty()){
                requireActivity().onBackPressed()
            }
        }
    }

    private fun savedPicsHandler(savedPicHandler: (pics: List<Picture>) -> Unit) {
        pendingUploadViewModel.savedPictures.observe(viewLifecycleOwner, { savedPics ->
            savedPicHandler.invoke(savedPics)
        })
    }
}