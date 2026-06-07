package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.ui.HomeTripMateActivity
import com.example.notingapp.databinding.ActivityLoginBinding
import com.example.notingapp.model.LoginRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nếu đã đăng nhập trước đó, chuyển luôn vào MainActivity
        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        if (!prefs.getString("userId", null).isNullOrEmpty()) {
            startActivity(Intent(this, HomeTripMateActivity::class.java))
            finish()
            return
        }

        binding.loginBtn.setOnClickListener {
            val userId = binding.userIdInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (userId.isEmpty() || password.isEmpty()) {
                binding.errorText.text = "Please fill in all fields"
                return@setOnClickListener
            }

            binding.errorText.text = ""
            setLoading(true)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(userId, password))
                    if (response.user != null) {
                        prefs.edit()
                            .putString("userId", response.user.userId)
                            .putString("displayName", response.user.displayName ?: response.user.userId)
                            .apply()
                        Toast.makeText(
                            this@LoginActivity,
                            "Welcome ${response.user.displayName ?: response.user.userId}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@LoginActivity, HomeTripMateActivity::class.java))
                        finish()
                    } else {
                        binding.errorText.text = response.message ?: "Login failed"
                    }
                } catch (e: Exception) {
                    binding.errorText.text = "Error: ${e.localizedMessage ?: "Cannot connect to server"}"
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.goSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginBtn.isEnabled = !isLoading
        binding.loginBtn.text = if (isLoading) "Logging in..." else getString(com.example.notingapp.R.string.btn_login)
    }
}
