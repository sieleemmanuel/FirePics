package com.siele.firebaseapp.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.siele.firebaseapp.R
import com.siele.firebaseapp.databinding.FragmentViewPictureBinding
import com.siele.firebaseapp.di.AppModule
import com.siele.firebaseapp.model.Picture
import com.siele.firebaseapp.utils.SetUpToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewPicture : Fragment() {
    private val TAG = "VIEWPICTURE"
    private lateinit var binding: FragmentViewPictureBinding

    private val args: ViewPictureArgs by navArgs()

    @Inject @AppModule.UploadsQualifier
    lateinit var uploadsDatabaseReference: DatabaseReference
    @Inject @AppModule.UpvotesQualifier
    lateinit var upVotesDatabaseReference: DatabaseReference
    @Inject @AppModule.ViewsQualifier
    lateinit var viewsDatabaseReference: DatabaseReference

    private lateinit var userId: String
    private lateinit var currentPicture: Picture

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewPictureBinding.inflate(inflater)
        userId = FirebaseAuth.getInstance().uid!!
        currentPicture = args.picture


        setViewsValues()
        binding.apply {
            SetUpToolbar.setUpToolbar(
                viewPicToolbar,
                this@ViewPicture,
                activity as AppCompatActivity
            )
            listenToVoteStatus()

            @Suppress("DEPRECATION")
            ivPictureView.setOnTouchListener {_, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (viewPicToolbar.isVisible || bottomSheetLayout.isVisible) {
                        viewPicToolbar.visibility = View.GONE
                        bottomSheetLayout.visibility = View.GONE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activity!!.window.insetsController?.hide(WindowInsets.Type.statusBars())
                        }else{
                            activity!!.window.setFlags(
                                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN
                            )
                        }
                    } else {
                        viewPicToolbar.visibility = View.VISIBLE
                        bottomSheetLayout.visibility = View.VISIBLE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activity!!.window.insetsController?.show(WindowInsets.Type.statusBars())
                        }else{
                            activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                        }
                    }
                    true
                } else {
                    false
                }
            }
            ibUpvoteCurrentPic.setOnClickListener {
                upVotesDatabaseReference.child(currentPicture.picId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(userId)) {
                                upVotesDatabaseReference.child(currentPicture.picId).child(userId)
                                    .removeValue()
                                ibUpvoteCurrentPic.setImageResource(R.drawable.ic_upvote_unchecked)
                                updateCurrentPictureVotes()

                            } else {
                                ibUpvoteCurrentPic.setImageResource(R.drawable.ic_upvote)
                                upVotesDatabaseReference.child(currentPicture.picId).child(userId)
                                    .setValue(true)
                                ibDownvoteCurrentPic.setImageResource(R.drawable.ic_downvote_unchecked)
                                uploadsDatabaseReference.child(currentPicture.picId)
                                    .child("upVotes").setValue(snapshot.childrenCount)
                                tvPicUpvotes.text = snapshot.childrenCount.toString()
                            }

                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                            upVotesDatabaseReference.removeEventListener(this)
                        }
                    })

            }
            ibDownvoteCurrentPic.setOnClickListener {
                upVotesDatabaseReference.child(currentPicture.picId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(userId)) {
                                upVotesDatabaseReference.child(currentPicture.picId).child(userId)
                                    .removeValue()
                                ibDownvoteCurrentPic.setImageResource(R.drawable.ic_downvote)
                                ibUpvoteCurrentPic.setImageResource(R.drawable.ic_upvote_unchecked)
                                updateCurrentPictureVotes()
                            } else {
                                ibDownvoteCurrentPic.setImageResource(R.drawable.ic_downvote_unchecked)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                            upVotesDatabaseReference.removeEventListener(this)
                        }

                    })
            }
        }

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity!!.window.insetsController?.show(WindowInsets.Type.statusBars())
        }else{
            activity!!.window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
        }
    }

    private fun FragmentViewPictureBinding.updateCurrentPictureVotes() {
        upVotesDatabaseReference.child(currentPicture.picId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    uploadsDatabaseReference.child(currentPicture.picId)
                        .child("upVotes").setValue(snapshot.childrenCount)
                    tvPicUpvotes.text = snapshot.childrenCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    upVotesDatabaseReference.removeEventListener(this)
                }

            })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            true
        } else {
            false
        }
    }

    private fun FragmentViewPictureBinding.listenToVoteStatus() {
        upVotesDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(currentPicture.picId).hasChild(userId)) {
                    ibUpvoteCurrentPic.setImageResource(R.drawable.ic_upvote)
                    tvPicUpvotes.text = snapshot.child(currentPicture.picId).childrenCount.toString()
                } else {
                    ibUpvoteCurrentPic.setImageResource(R.drawable.ic_upvote_unchecked)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                upVotesDatabaseReference.removeEventListener(this)
            }

        })
    }

    private fun setViewsValues() {
        binding.apply {
            tvPicUpvotes.text = currentPicture.upVotes.toString()
            tvPicViews.text = currentPicture.views.toString()
            Log.d(TAG, "currentPicture votes: ${currentPicture.upVotes}")
            Glide.with(requireContext())
                .load(currentPicture.pictureUri)
                .into(ivPictureView)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity!!.window.statusBarColor = Color.TRANSPARENT
        }else{
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }


    }
}