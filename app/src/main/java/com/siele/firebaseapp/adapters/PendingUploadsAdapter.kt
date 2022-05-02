package com.siele.firebaseapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.siele.firebaseapp.databinding.ToUploadItemBinding
import com.siele.firebaseapp.model.Picture

class PendingUploadsAdapter(
    val currentItemViewClickListener: CurrentItemViewClickListener,
    val context: Context
):ListAdapter<Picture, PendingUploadsAdapter.PendingViewHolder>(DiffCallback()) {
    inner class PendingViewHolder(private val binding: ToUploadItemBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(picture: Picture) {
            binding.apply {
                Glide.with(context)
                    .load(picture.pictureUri)
                    .into(ivPic)
                btnRemove.setOnClickListener {
                    currentItemViewClickListener.onItemClick(picture, it)
                }
                btnUploadSaved.setOnClickListener {
                    currentItemViewClickListener.onItemClick(picture, it)
                }
            }

        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val binding = ToUploadItemBinding.inflate(LayoutInflater.from(context), parent, false)
       return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
       holder.bind(getItem(position))
    }

    class DiffCallback:DiffUtil.ItemCallback<Picture>() {
        override fun areItemsTheSame(oldItem: Picture, newItem: Picture): Boolean =
            oldItem.pictureUri == newItem.pictureUri

        override fun areContentsTheSame(oldItem: Picture, newItem: Picture): Boolean =
            oldItem == newItem

    }
    class CurrentItemViewClickListener(val clickListener:(picture:Picture, view:View)->Unit){
        fun onItemClick(picture: Picture, view:View) = clickListener(picture, view )
    }
}