package com.siele.firebaseapp.interfaces

import android.widget.ImageButton
import com.siele.firebaseapp.databinding.PictureItemBinding
import com.siele.firebaseapp.model.Picture

interface ViewClickListener {
    fun onItemClickListener(
        position: Int,
        imageButton: ImageButton,
        binding: PictureItemBinding,
        picture: Picture
    )
}