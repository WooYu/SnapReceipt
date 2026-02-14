package com.snapreceipt.io.ui.preview

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityImagePreviewBinding
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import java.io.File

class ImagePreviewActivity : BaseActivity<BaseViewModel>() {
    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }

    private var _binding: ActivityImagePreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }
        binding.previewImage.setOnClickListener { finish() }

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        val model = imageModel(imagePath, imageUrl)
        if (model == null) {
            Toast.makeText(this, getString(R.string.image_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Glide.with(this)
            .load(model)
            .fitCenter()
            .into(binding.previewImage)
    }

    override fun onDestroy() {
        _binding?.btnClose?.setOnClickListener(null)
        _binding?.previewImage?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun imageModel(imagePath: String, imageUrl: String): Any? {
        localImageModel(imagePath)?.let { return it }
        return imageUrl.takeIf { it.isNotBlank() }
    }

    private fun localImageModel(path: String): Any? {
        if (path.isBlank()) return null
        if (path.startsWith("content://", ignoreCase = true) ||
            path.startsWith("file://", ignoreCase = true)
        ) {
            return Uri.parse(path)
        }
        return File(path).takeIf { it.exists() }
    }
}
