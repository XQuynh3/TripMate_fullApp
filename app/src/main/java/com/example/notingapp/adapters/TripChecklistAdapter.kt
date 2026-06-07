package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemTripChecklistBinding
import com.example.notingapp.model.TripChecklistItem

class TripChecklistAdapter :
    ListAdapter<TripChecklistItem, TripChecklistAdapter.VH>(DiffCallback()) {

    var onCheckChanged: ((itemId: String, checked: Boolean) -> Unit)? = null
    var onAssignClick: ((itemId: String) -> Unit)? = null
    var onItemClick: ((item: TripChecklistItem) -> Unit)? = null
    var onItemLongClick: ((item: TripChecklistItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTripChecklistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(
        private val binding: ItemTripChecklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TripChecklistItem) {
            binding.cbDone.setOnCheckedChangeListener(null)

            binding.cbDone.isChecked = item.done
            binding.tvText.text = item.text
            binding.tvCategory.text = item.category

            binding.tvAssigned.text = if (item.assignedTo.isNullOrBlank()) {
                "Unassigned"
            } else {
                "Assigned to ${item.assignedTo}"
            }

            val updatedText = if (item.updatedBy.isNullOrBlank()) "" else "Updated by ${item.updatedBy}"
            val reminderText = if (!item.reminderId.isNullOrBlank()) " • \uD83D\uDCCD Reminder" else ""
            binding.tvUpdatedBy.text = updatedText + reminderText

            binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged?.invoke(item._id, isChecked)
            }

            binding.btnAssign.setOnClickListener {
                onAssignClick?.invoke(item._id)
            }

            binding.itemRoot.setOnClickListener {
                onItemClick?.invoke(item)
            }

            binding.itemRoot.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TripChecklistItem>() {
        override fun areItemsTheSame(
            oldItem: TripChecklistItem,
            newItem: TripChecklistItem
        ): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(
            oldItem: TripChecklistItem,
            newItem: TripChecklistItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}