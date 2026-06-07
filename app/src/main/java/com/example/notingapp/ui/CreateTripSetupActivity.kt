package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.databinding.ActivityCreateTripSetupBinding
import com.example.notingapp.model.CreateTripRequest
import com.example.notingapp.model.DeleteChecklistRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class CreateTripSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateTripSetupBinding

    private var destination: String = ""
    private var uncheckedTexts: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        destination = intent.getStringExtra("destination") ?: ""
        uncheckedTexts = intent.getStringArrayListExtra("uncheckedTexts") ?: arrayListOf()

        if (destination.isEmpty()) {
            Toast.makeText(this, "Missing destination", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDestination.text = destination
        binding.etTitle.setText("$destination cuối tuần")

        binding.tvChecklistPreview.text = if (uncheckedTexts.isEmpty()) {
            "All suggested notes will be included"
        } else {
            "${uncheckedTexts.size} items will be excluded"
        }

        binding.btnCreate.setOnClickListener {
            createTrip()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun createTrip() {
        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.etTitle.text.toString().trim()
        val note = binding.etNote.text.toString().trim()
        val membersRaw = binding.etMembers.text.toString().trim()

        val memberIds = if (membersRaw.isEmpty()) {
            emptyList()
        } else {
            membersRaw.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != userId }
                .distinct()
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCreate.isEnabled = false
        binding.btnCreate.text = "Creating..."

        lifecycleScope.launch {
            try {
                var trip = RetrofitClient.api.createTripFromSuggestion(
                    CreateTripRequest(
                        destination = destination,
                        userId = userId,
                        title = title,
                        memberIds = memberIds,
                        note = note.ifEmpty { null },
                        uncheckedTexts = uncheckedTexts
                    )
                )

                // Backend mới đã hỗ trợ uncheckedTexts trong createTripFromSuggestion.
                // Đoạn dưới giữ lại như fallback phòng khi server cũ/chưa redeploy.
                if (uncheckedTexts.isNotEmpty()) {
                    val toDelete = trip.checklist.filter { item ->
                        uncheckedTexts.any { unchecked ->
                            unchecked.equals(item.text, ignoreCase = true)
                        }
                    }

                    for (item in toDelete) {
                        try {
                            val deleteResponse = RetrofitClient.api.deleteChecklistItem(
                                trip._id,
                                item._id,
                                DeleteChecklistRequest(userId)
                            )

                            trip = deleteResponse.trip ?: trip
                        } catch (_: Exception) {
                            // Bỏ qua từng item nếu server đã xử lý uncheckedTexts trước đó
                        }
                    }
                }

                Toast.makeText(
                    this@CreateTripSetupActivity,
                    "Trip created!",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(
                    this@CreateTripSetupActivity,
                    TripDetailActivity::class.java
                )

                intent.putExtra("tripId", trip._id)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateTripSetupActivity,
                    "Error: ${e.localizedMessage ?: "Cannot create trip"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnCreate.isEnabled = true
                binding.btnCreate.text = "Create Trip"
            }
        }
    }
}