package com.snapreceipt.io.ui.preview

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityImagePreviewBinding
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import java.io.File

class ImagePreviewActivity : EdgeToEdgeActivity() {
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
        when {
            imagePath.isNotBlank() && File(imagePath).exists() ->
                binding.previewImage.setImageURI(Uri.fromFile(File(imagePath)))
            imageUrl.isNotBlank() ->
                binding.previewImage.setImageURI(Uri.parse(imageUrl))
            else -> {
                Toast.makeText(this, getString(R.string.image_missing), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
