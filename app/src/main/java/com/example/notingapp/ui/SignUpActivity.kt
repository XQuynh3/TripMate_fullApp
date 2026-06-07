package com.example.notingapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.databinding.ActivitySignupBinding
import com.example.notingapp.model.SignupRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signUpBtn.setOnClickListener {
            val userId = binding.userIdInput.text.toString().trim()
            val displayName = binding.displayNameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (userId.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
                binding.errorText.text = "Please fill in all fields"
                return@setOnClickListener
            }

            binding.errorText.text = ""
            setLoading(true)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.signup(SignupRequest(userId, displayName, password))
                    if (response.user != null) {
                        Toast.makeText(
                            this@SignUpActivity,
                            response.message ?: "Sign up successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        binding.errorText.text = response.message ?: "Sign up failed"
                    }
                } catch (e: Exception) {
                    binding.errorText.text = "Error: ${e.localizedMessage ?: "Cannot connect to server"}"
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.goLogin.setOnClickListener {
            finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.signUpBtn.isEnabled = !isLoading
        binding.signUpBtn.text = if (isLoading) "Signing up..." else getString(com.example.notingapp.R.string.btn_signup)
    }
}
