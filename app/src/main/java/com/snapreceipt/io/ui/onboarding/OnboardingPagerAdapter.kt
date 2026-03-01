package com.snapreceipt.io.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemOnboardingPageBinding

class OnboardingPagerAdapter(
    @DrawableRes private val pages: List<Int>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemOnboardingPageBinding.inflate(inflater, parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position)
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(@DrawableRes imageRes: Int, position: Int) {
            binding.onboardingImage.setImageResource(imageRes)
            binding.onboardingImage.contentDescription =
                binding.root.context.getString(R.string.onboarding_image_desc, position + 1)
        }
    }
}
