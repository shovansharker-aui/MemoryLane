package com.memorylane.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memorylane.app.databinding.ItemMediaBinding
import com.memorylane.app.model.MediaItem

class MediaAdapter(
    private val onClick: (MediaItem, Int) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem, position: Int) {
            Glide.with(binding.imageThumb)
                .load(item.uri)
                .centerCrop()
                .into(binding.imageThumb)

            binding.iconVideo.visibility =
                if (item.isVideo) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onClick(item, position) }
            binding.root.withBouncyPress()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem == newItem
        }
    }
}
