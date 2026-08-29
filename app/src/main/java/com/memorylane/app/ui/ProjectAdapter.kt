package com.memorylane.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memorylane.app.data.Project
import com.memorylane.app.databinding.ItemProjectBinding
import java.text.DateFormat
import java.util.Date

class ProjectAdapter(
    private val onClick: (Project) -> Unit,
    private val onLongClick: (Project) -> Unit
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.textProjectName.text = project.name
            binding.textProjectDate.text =
                DateFormat.getDateInstance().format(Date(project.createdAt))

            binding.root.setOnClickListener { onClick(project) }
            binding.root.setOnLongClickListener {
                onLongClick(project)
                true
            }
            binding.root.withBouncyPress()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Project, newItem: Project) =
                oldItem == newItem
        }
    }
}
