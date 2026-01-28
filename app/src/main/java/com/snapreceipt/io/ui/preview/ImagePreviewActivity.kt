package com.snapreceipt.io.ui.preview

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import java.io.File

class ImagePreviewActivity : EdgeToEdgeActivity() {
    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imageView = findViewById<ImageView>(R.id.preview_image)
        val closeBtn = findViewById<ImageView>(R.id.btn_close)
        closeBtn.setOnClickListener { finish() }
        imageView.setOnClickListener { finish() }

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        when {
            imagePath.isNotBlank() && File(imagePath).exists() ->
                imageView.setImageURI(Uri.fromFile(File(imagePath)))
            imageUrl.isNotBlank() ->
                imageView.setImageURI(Uri.parse(imageUrl))
            else -> {
                Toast.makeText(this, getString(R.string.image_missing), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
