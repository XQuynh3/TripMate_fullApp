package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemPlanItemBinding
import com.example.notingapp.model.PlanItem

class PlanItemAdapter :
    ListAdapter<PlanItem, PlanItemAdapter.VH>(DiffCallback()) {

    var onItemClick: ((item: PlanItem) -> Unit)? = null
    var onItemLongClick: ((item: PlanItem) -> Unit)? = null
    var onDoneToggle: ((itemId: String, done: Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPlanItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemPlanItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanItem) {
            binding.tvTitle.text = item.title
            binding.tvTime.text = if (item.endTime.isNullOrBlank()) {
                item.startTime
            } else {
                "${item.startTime} - ${item.endTime}"
            }
            binding.tvType.text = item.type?.replaceFirstChar { it.uppercase() } ?: "Custom"
            binding.tvLocation.text = item.locationName ?: ""
            binding.tvLocation.visibility = if (item.locationName.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvAssigned.text = if (item.assignedTo.isNullOrBlank()) "Unassigned" else "Assigned to ${item.assignedTo}"

            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = item.done == true
            binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
                item._id?.let { id -> onDoneToggle?.invoke(id, isChecked) }
            }

            binding.root.setOnClickListener { onItemClick?.invoke(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlanItem>() {
        override fun areItemsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
            return oldItem == newItem
        }
    }
}
