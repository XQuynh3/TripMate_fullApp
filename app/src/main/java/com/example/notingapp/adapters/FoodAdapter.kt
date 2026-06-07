package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemFoodBinding
import com.example.notingapp.model.FoodItem

class FoodAdapter : ListAdapter<FoodItem, FoodAdapter.VH>(DiffCallback()) {

    var onAddClick: ((item: FoodItem) -> Unit)? = null
    var onItemClick: ((item: FoodItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FoodItem) {
            binding.tvName.text = item.name
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

    class DiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem) = oldItem == newItem
    }
}
