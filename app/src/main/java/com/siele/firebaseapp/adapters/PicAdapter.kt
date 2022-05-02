package com.siele.firebaseapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.siele.firebaseapp.databinding.PictureItemBinding
import com.siele.firebaseapp.interfaces.ViewClickListener
import com.siele.firebaseapp.model.Picture

class PicAdapter(
    val clickListener: ItemClickListener,
    val viewClickListener: ViewClickListener,
    val setUpVoteStatus: SetUpVoteStatus,
    private val context: Context
)
    :ListAdapter<Picture,PicAdapter.PicViewHolder>(PicDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PicViewHolder {
        val binding = PictureItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PicViewHolder, position: Int) {
        val picture = getItem(position)
        holder.bind(picture,position)
    }

    inner class PicViewHolder(private val binding: PictureItemBinding)
        :RecyclerView.ViewHolder(binding.root) {
        fun bind(picture: Picture, position: Int){
            binding.apply {
                Glide.with(context).load(picture.pictureUri).into(ivPic)
                tvUser.text = picture.userId
                tvDownvotesCount.text = picture.downVotes
                tvUpvotesCount.text = picture.upVotes.toString()
                tvViewsCount.text = picture.views.toString()

                ibtnDownvote.setOnClickListener {
                    viewClickListener.onItemClickListener(
                        position,
                        ibtnDownvote,
                        binding,
                        picture
                    )
                }
                ibtnUpvote.setOnClickListener {
                    viewClickListener.onItemClickListener(
                        position,
                        ibtnUpvote,
                        binding,
                        picture
                    )
                }
                root.setOnClickListener {
                    clickListener.onClick(picture, position)
                }
                ibtnView.isClickable = false
                setUpVoteStatus.setValues(binding, picture)
            }

        }
    }


    class ItemClickListener(val clickListener:(picture:Picture, position:Int)->Unit){
        fun onClick(picture: Picture, position: Int) = clickListener(picture, position)
    }
    class SetUpVoteStatus(val setValues: (itemBinding: PictureItemBinding, pic:Picture) -> Unit) {
        fun setViewStatus(itemBinding: PictureItemBinding, pic:Picture) = setValues(itemBinding, pic)
    }
}

object PicDiffUtil:DiffUtil.ItemCallback<Picture>() {
    override fun areItemsTheSame(oldItem: Picture, newItem: Picture): Boolean {
        return oldItem.pictureUri == newItem.pictureUri
    }

    override fun areContentsTheSame(oldItem: Picture, newItem: Picture): Boolean {
        return oldItem == newItem
    }

}


