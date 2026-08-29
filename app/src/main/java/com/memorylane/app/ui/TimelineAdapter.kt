package com.memorylane.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memorylane.app.databinding.ItemMediaBinding
import com.memorylane.app.databinding.ItemTimelineHeaderBinding
import com.memorylane.app.model.MediaItem

sealed class TimelineRow {
    data class Header(val label: String) : TimelineRow()
    data class Photo(val item: MediaItem) : TimelineRow()
}

class TimelineAdapter(
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<TimelineRow> = emptyList()

    fun submitRows(newRows: List<TimelineRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is TimelineRow.Header) TYPE_HEADER else TYPE_PHOTO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemTimelineHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            PhotoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is TimelineRow.Header -> (holder as HeaderViewHolder).bind(row)
            is TimelineRow.Photo -> (holder as PhotoViewHolder).bind(row)
        }
    }

    override fun getItemCount(): Int = rows.size

    inner class HeaderViewHolder(private val binding: ItemTimelineHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: TimelineRow.Header) {
            binding.textDateHeader.text = row.label
        }
    }

    inner class PhotoViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: TimelineRow.Photo) {
            Glide.with(binding.imageThumb).load(row.item.uri).centerCrop().into(binding.imageThumb)
            binding.iconVideo.visibility =
                if (row.item.isVideo) android.view.View.VISIBLE else android.view.View.GONE
            binding.root.setOnClickListener { onClick(row.item) }
            binding.root.withBouncyPress()
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PHOTO = 1
    }
}
