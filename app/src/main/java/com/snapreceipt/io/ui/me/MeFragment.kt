package com.snapreceipt.io.ui.me

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MeFragment : Fragment() {
    private lateinit var editProfileBtn: View
    private lateinit var exportBtn: View
    private lateinit var settingsBtn: View
    private lateinit var feedbackBtn: View
    private lateinit var aboutBtn: View
    private lateinit var logoutBtn: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_me, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editProfileBtn = view.findViewById(R.id.edit_profile_btn)
        exportBtn = view.findViewById(R.id.menu_export)
        settingsBtn = view.findViewById(R.id.menu_settings)
        feedbackBtn = view.findViewById(R.id.menu_feedback)
        aboutBtn = view.findViewById(R.id.menu_about)
        logoutBtn = view.findViewById(R.id.logout_btn)

        setupListeners()
    }

    private fun setupListeners() {
        editProfileBtn.setOnClickListener {
            Toast.makeText(requireContext(), "编辑个人资料", Toast.LENGTH_SHORT).show()
        }
        exportBtn.setOnClickListener {
            Toast.makeText(requireContext(), "导出记录", Toast.LENGTH_SHORT).show()
        }
        settingsBtn.setOnClickListener {
            Toast.makeText(requireContext(), "设置", Toast.LENGTH_SHORT).show()
        }
        feedbackBtn.setOnClickListener {
            Toast.makeText(requireContext(), "反馈", Toast.LENGTH_SHORT).show()
        }
        aboutBtn.setOnClickListener {
            Toast.makeText(requireContext(), "关于我们", Toast.LENGTH_SHORT).show()
        }
        logoutBtn.setOnClickListener {
            onLogout()
        }
    }

    private fun onLogout() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }
}
