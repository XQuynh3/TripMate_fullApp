package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemReviewBinding
import com.example.notingapp.model.PlaceReviewItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter : ListAdapter<PlaceReviewItem, ReviewAdapter.VH>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceReviewItem) {
            val name = item.displayName ?: item.userId ?: "Traveler"
            binding.tvDisplayName.text = name
            binding.tvAvatar.text = name.take(1).uppercase()

            val color = item.avatarColor
            if (!color.isNullOrBlank()) {
                try {
                    binding.tvAvatar.background.setTint(android.graphics.Color.parseColor(color))
                } catch (_: Exception) { }
            }

            binding.tvDate.text = formatDate(item.createdAt)
            binding.tvStars.text = buildStars(item.score ?: 0)
            binding.tvComment.text = if (item.comment.isNullOrBlank()) "No comment" else item.comment

            val sourceLabel = when (item.source) {
                "location_reminder" -> "Arrived review"
                "plan_done" -> "Plan review"
                else -> "Manual review"
            }
            binding.tvSource.text = sourceLabel

            binding.tvTripDestination.text = "Trip: ${item.destination ?: ""}"
            binding.tvTripDestination.visibility = if (item.destination.isNullOrBlank()) View.GONE else View.VISIBLE

            binding.tvImageHint.visibility = if (item.imageUrls.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlaceReviewItem>() {
        override fun areItemsTheSame(oldItem: PlaceReviewItem, newItem: PlaceReviewItem): Boolean {
            return oldItem.userId == newItem.userId && oldItem.createdAt == newItem.createdAt
        }

        override fun areContentsTheSame(oldItem: PlaceReviewItem, newItem: PlaceReviewItem): Boolean {
            return oldItem == newItem
        }
    }

    private fun buildStars(score: Int): String {
        val filled = "\u2605"
        val empty = "\u2606"
        return (1..5).joinToString("") { if (it <= score) filled else empty }
    }

    private fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Recently"
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
            sdf.format(Date(timestamp))
        } catch (_: Exception) {
            "Recently"
        }
    }
}
