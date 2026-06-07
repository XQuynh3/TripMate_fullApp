package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemPlaceBinding
import com.example.notingapp.model.Place

class PlaceAdapter : ListAdapter<Place, PlaceAdapter.VH>(DiffCallback()) {

    var onAddClick: ((item: Place) -> Unit)? = null
    var onItemClick: ((item: Place) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Place) {
            binding.tvName.text = item.name
            binding.tvType.text = item.type
            binding.tvReason.text = item.reason
            binding.tvCost.text = item.estimatedCost

            binding.btnAdd.setOnClickListener {
                onAddClick?.invoke(item)
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Place, newItem: Place) = oldItem == newItem
    }
}
