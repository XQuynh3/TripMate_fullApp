package com.example.notingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notingapp.databinding.ItemChecklistBinding
import com.example.notingapp.model.SuggestionChecklistItem

class SuggestionChecklistAdapter : ListAdapter<SuggestionChecklistItem, SuggestionChecklistAdapter.VH>(DiffCallback()) {

    private val selectedTexts = mutableSetOf<String>()

    fun setItems(items: List<SuggestionChecklistItem>) {
        selectedTexts.clear()
        selectedTexts.addAll(items.map { it.text })
        submitList(items)
    }

    fun getSelectedItems(): List<SuggestionChecklistItem> {
        return currentList.filter { selectedTexts.contains(it.text) }
    }

    fun getAllItems(): List<SuggestionChecklistItem> {
        return currentList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemChecklistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SuggestionChecklistItem) {
            binding.cbSelected.setOnCheckedChangeListener(null)
            binding.cbSelected.isChecked = selectedTexts.contains(item.text)
            binding.tvText.text = item.text
            binding.tvCategory.text = item.category

            binding.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTexts.add(item.text) else selectedTexts.remove(item.text)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SuggestionChecklistItem>() {
        override fun areItemsTheSame(oldItem: SuggestionChecklistItem, newItem: SuggestionChecklistItem) = oldItem.text == newItem.text
        override fun areContentsTheSame(oldItem: SuggestionChecklistItem, newItem: SuggestionChecklistItem) = oldItem == newItem
    }
}
