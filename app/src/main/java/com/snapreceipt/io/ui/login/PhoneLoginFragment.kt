package com.snapreceipt.io.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.snapreceipt.io.R

class PhoneLoginFragment : Fragment() {
    private lateinit var phoneInput: TextInputEditText
    private lateinit var codeInput: TextInputEditText
    private lateinit var getCodeBtn: Button
    private lateinit var loginBtn: Button
    private lateinit var switchLogin: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_phone_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        phoneInput = view.findViewById(R.id.phone_input)
        codeInput = view.findViewById(R.id.code_input)
        getCodeBtn = view.findViewById(R.id.get_code_btn)
        loginBtn = view.findViewById(R.id.login_btn)
        switchLogin = view.findViewById(R.id.switch_login)

        getCodeBtn.setOnClickListener { onGetCodeClick() }
        loginBtn.setOnClickListener { onLoginClick() }
        switchLogin.setOnClickListener { onSwitchLogin() }
    }

    private fun onGetCodeClick() {
        val phone = phoneInput.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), R.string.phone_empty, Toast.LENGTH_SHORT).show()
            return
        }
        // Simulate sending code
        Toast.makeText(requireContext(), getString(R.string.code_sent, phone), Toast.LENGTH_SHORT).show()
        getCodeBtn.isEnabled = false
    }

    private fun onLoginClick() {
        val phone = phoneInput.text.toString().trim()
        val code = codeInput.text.toString().trim()

        if (phone.isEmpty() || code.isEmpty()) {
            Toast.makeText(requireContext(), R.string.input_required, Toast.LENGTH_SHORT).show()
            return
        }

        // Simulate login
        (activity as? LoginActivity)?.navigateToMainActivity()
    }

    private fun onSwitchLogin() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EmailLoginFragment())
            .addToBackStack(null)
            .commit()
    }
}
