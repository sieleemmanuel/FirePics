package com.siele.firebaseapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Picture(
    var userId: String = "",
    var pictureUri: String? = null,
    var upVotes: Long? = 0L,
    var downVotes: String? = "",
    var views: Long? = 0L,
    var picId: String = "",
    var name: String = "IMG"
) : Parcelable
