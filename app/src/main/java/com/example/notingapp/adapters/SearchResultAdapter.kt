package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemSearchResultBinding
import com.example.notingapp.model.SearchResultItem

class SearchResultAdapter :
    ListAdapter<SearchResultItem, SearchResultAdapter.VH>(DiffCallback()) {

    var onItemClick: ((item: SearchResultItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResultItem) {
            binding.tvName.text = item.name
            binding.tvSubtitle.text = item.subtitle ?: item.destination ?: ""
            binding.tvReason.text = item.reason ?: ""
            binding.tvCost.text = item.estimatedCost ?: ""
            binding.tvCost.visibility = if (item.estimatedCost.isNullOrBlank()) View.GONE else View.VISIBLE

            val typeLabel = when (item.type) {
                "destination" -> "Destination"
                "place" -> "Place"
                "food" -> "Food"
                else -> item.type.replaceFirstChar { it.uppercase() }
            }
            binding.tvTypeBadge.text = typeLabel

            val ratingText = if ((item.ratingCount ?: 0) > 0) {
                val avg = String.format("%.1f", item.ratingAverage ?: 0.0)
                "\u2B50 $avg (${item.ratingCount} reviews)"
            } else {
                "No reviews yet"
            }
            binding.tvRating.text = ratingText

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
            return oldItem.type == newItem.type && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
            return oldItem == newItem
        }
    }
}
