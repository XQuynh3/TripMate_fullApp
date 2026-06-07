package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemTripBinding
import com.example.notingapp.model.Trip

class TripAdapter : ListAdapter<Trip, TripAdapter.VH>(DiffCallback()) {

    var onItemClick: ((Trip) -> Unit)? = null
    var onItemLongClick: ((Trip) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTripBinding.inflate(
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
        private val binding: ItemTripBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(pos))
                }
            }

            binding.root.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(pos))
                }
                true
            }
        }

        fun bind(trip: Trip) {
            binding.tvTitle.text = trip.title
            binding.tvDestination.text = trip.destination
            binding.tvOwner.text = "By ${trip.ownerId}"
            binding.tvStatus.text = trip.status ?: "planning"

            val total = trip.checklist.size
            val done = trip.checklist.count { it.done }
            binding.tvProgress.text = "$done/$total notes done"

            val membersText = if (trip.members.isEmpty()) {
                "Only you"
            } else {
                "${trip.members.size + 1} members"
            }

            binding.tvMembers.text = membersText
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }
}