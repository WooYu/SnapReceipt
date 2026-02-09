package com.snapreceipt.io.ui.invoice.bottomsheet

import android.animation.ObjectAnimator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.databinding.ItemCategoryAddBinding
import com.snapreceipt.io.databinding.ItemCategoryChipBinding

/**
 * Adapter for category chips + trailing add-entry row.
 * It keeps selection refresh granular to avoid full-list flicker.
 */
internal class CategoryChipAdapter(
    private val onSelect: (CategoryOption) -> Unit,
    private val onLongPress: (CategoryOption) -> Unit,
    private val onAddClick: () -> Unit
) : ListAdapter<CategoryListItem, RecyclerView.ViewHolder>(CategoryListItemDiff) {

    private var selectedLabel: String = ""

    fun hasCategoryItems(): Boolean = currentList.any { it is CategoryListItem.Category }

    fun submitCategories(
        list: List<CategoryOption>,
        selected: String,
        onCommitted: (() -> Unit)? = null
    ) {
        val items = ArrayList<CategoryListItem>(list.size + 1).apply {
            addAll(list.map { CategoryListItem.Category(it) })
            add(CategoryListItem.AddCategory)
        }
        val previous = selectedLabel
        selectedLabel = selected
        submitList(items) {
            notifySelectionChanged(previous, selectedLabel)
            onCommitted?.invoke()
        }
    }

    fun updateSelection(label: String) {
        if (selectedLabel.equals(label, ignoreCase = true)) return
        val previous = selectedLabel
        selectedLabel = label
        notifySelectionChanged(previous, selectedLabel)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CategoryListItem.Category -> VIEW_TYPE_CATEGORY
            is CategoryListItem.AddCategory -> VIEW_TYPE_ADD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val binding = ItemCategoryChipBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CategoryViewHolder(binding)
            }

            VIEW_TYPE_ADD -> {
                val binding = ItemCategoryAddBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddViewHolder(binding)
            }

            else -> error("Unsupported viewType=$viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                val item = getItem(position) as CategoryListItem.Category
                val option = item.option
                holder.bind(
                    option = option,
                    selected = option.matches(selectedLabel),
                    onSelect = onSelect,
                    onLongPress = onLongPress
                )
            }

            is AddViewHolder -> holder.bind(onAddClick)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        // Payload path only updates selected state; avoids re-binding click listeners/text.
        if (payloads.any { it === SelectionPayload } && holder is CategoryViewHolder) {
            val item = getItem(position) as? CategoryListItem.Category ?: return
            holder.bindSelection(item.option.matches(selectedLabel))
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    private fun notifySelectionChanged(previousLabel: String, currentLabel: String) {
        val previousIndex = indexOfCategoryByLabel(previousLabel)
        val currentIndex = indexOfCategoryByLabel(currentLabel)

        // Only refresh old/new selected rows, avoid full list redraw.
        if (previousIndex >= 0) notifyItemChanged(previousIndex, SelectionPayload)
        if (currentIndex >= 0 && currentIndex != previousIndex) {
            notifyItemChanged(currentIndex, SelectionPayload)
        }
    }

    private fun indexOfCategoryByLabel(label: String): Int {
        return currentList.indexOfFirst { item ->
            val categoryItem = item as? CategoryListItem.Category ?: return@indexOfFirst false
            categoryItem.option.matches(label)
        }
    }

    private class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            option: CategoryOption,
            selected: Boolean,
            onSelect: (CategoryOption) -> Unit,
            onLongPress: (CategoryOption) -> Unit
        ) {
            binding.chipText.text = option.label
            bindSelection(selected)
            binding.chipText.setOnClickListener { onSelect(option) }
            binding.chipText.setOnLongClickListener {
                if (option.isCustom) {
                    // Haptic + shake animation makes delete gesture discoverable.
                    binding.chipText.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    playDeleteAnimation(binding.chipText)
                    onLongPress(option)
                    true
                } else {
                    false
                }
            }
        }

        fun bindSelection(selected: Boolean) {
            binding.chipText.isSelected = selected
        }

        private fun playDeleteAnimation(target: View) {
            ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -2.5f, 2.5f, -2f, 2f, 0f).apply {
                duration = 260L
                start()
            }
        }
    }

    private class AddViewHolder(
        private val binding: ItemCategoryAddBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onAddClick: () -> Unit) {
            binding.addText.setOnClickListener { onAddClick() }
        }
    }

    private companion object {
        private const val VIEW_TYPE_CATEGORY = 1
        private const val VIEW_TYPE_ADD = 2

        private object SelectionPayload

        private val CategoryListItemDiff = object : DiffUtil.ItemCallback<CategoryListItem>() {
            override fun areItemsTheSame(
                oldItem: CategoryListItem,
                newItem: CategoryListItem
            ): Boolean {
                return when {
                    oldItem is CategoryListItem.Category && newItem is CategoryListItem.Category ->
                        oldItem.option.id == newItem.option.id

                    oldItem is CategoryListItem.AddCategory && newItem is CategoryListItem.AddCategory ->
                        true

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: CategoryListItem,
                newItem: CategoryListItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
