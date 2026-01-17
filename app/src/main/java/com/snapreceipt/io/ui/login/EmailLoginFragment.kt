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

class EmailLoginFragment : Fragment() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginBtn: Button
    private lateinit var forgotPassword: TextView
    private lateinit var switchLogin: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_email_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailInput = view.findViewById(R.id.email_input)
        passwordInput = view.findViewById(R.id.password_input)
        loginBtn = view.findViewById(R.id.login_btn)
        forgotPassword = view.findViewById(R.id.forgot_password)
        switchLogin = view.findViewById(R.id.switch_login)

        loginBtn.setOnClickListener { onLoginClick() }
        forgotPassword.setOnClickListener { onForgotPassword() }
        switchLogin.setOnClickListener { onSwitchLogin() }
    }

    private fun onLoginClick() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.input_required, Toast.LENGTH_SHORT).show()
            return
        }

        // Simulate login
        (activity as? LoginActivity)?.navigateToMainActivity()
    }

    private fun onForgotPassword() {
        Toast.makeText(requireContext(), R.string.forgot_password_sent, Toast.LENGTH_SHORT).show()
    }

    private fun onSwitchLogin() {
        parentFragmentManager.popBackStack()
    }
}
