package com.example.notingapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notingapp.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etUsername.setText("John Doe")
        binding.etEmail.setText("john.doe@example.com")
        binding.etPhone.setText("+123 456 7890")

        binding.btnSave.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val phone = binding.etPhone.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
                // Lưu thông tin (ở đây chỉ là toast, bạn có thể thay thế bằng việc lưu vào cơ sở dữ liệu)
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()

                // Ví dụ về việc lưu trữ dữ liệu vào sharedPreferences (hoặc database)
                saveUserProfile(username, email, phone)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveUserProfile(username: String, email: String, phone: String) {
        // Dùng SharedPreferences hoặc bất kỳ nơi lưu trữ nào bạn thích
    }
}