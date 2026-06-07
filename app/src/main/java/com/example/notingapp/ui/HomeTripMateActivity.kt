package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notingapp.R
import com.example.notingapp.adapters.SearchResultAdapter
import com.example.notingapp.databinding.ActivityHomeTripmateBinding
import com.example.notingapp.model.JoinTripRequestBody
import com.example.notingapp.model.SearchResultItem
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeTripMateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeTripmateBinding
    private val searchResultAdapter = SearchResultAdapter()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeTripmateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        val displayName = prefs.getString("displayName", "Traveler")
        binding.tvGreeting.text = "Hi, ${displayName ?: "Traveler"}"

        setupSearchResults()

        binding.etDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.isEmpty()) {
                    hideSearchResults()
                    return
                }
                searchJob = lifecycleScope.launch {
                    delay(300)
                    performSmartSearch(query)
                }
            }
        })

        binding.btnSearch.setOnClickListener {
            val query = binding.etDestination.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openSuggestionResult(query)
        }

        binding.chipVungTau.setOnClickListener { openSuggestionResult("Vũng Tàu") }
        binding.chipDaLat.setOnClickListener { openSuggestionResult("Đà Lạt") }
        binding.chipNhaTrang.setOnClickListener { openSuggestionResult("Nha Trang") }

        binding.btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnMyTrips.setOnClickListener {
            startActivity(Intent(this, MyTripsActivity::class.java))
        }

        binding.btnJoinTrip.setOnClickListener {
            showJoinTripDialog()
        }

        binding.btnRequests.setOnClickListener {
            startActivity(Intent(this, TripRequestsActivity::class.java))
        }
    }

    private fun setupSearchResults() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchResultAdapter
        searchResultAdapter.onItemClick = { item ->
            handleSearchResultClick(item)
        }
    }

    private fun hideSearchResults() {
        binding.rvSearchResults.visibility = android.view.View.GONE
        binding.tvSearchEmpty.visibility = android.view.View.GONE
    }

    private fun showSearchResults() {
        binding.rvSearchResults.visibility = android.view.View.VISIBLE
    }

    private fun performSmartSearch(query: String) {
        lifecycleScope.launch {
            try {
                val results = RetrofitClient.api.smartSearch(query)
                searchResultAdapter.submitList(results)
                if (results.isEmpty()) {
                    binding.tvSearchEmpty.visibility = android.view.View.VISIBLE
                    binding.rvSearchResults.visibility = android.view.View.GONE
                } else {
                    binding.tvSearchEmpty.visibility = android.view.View.GONE
                    showSearchResults()
                }
            } catch (_: Exception) {
                hideSearchResults()
            }
        }
    }

    private fun handleSearchResultClick(item: SearchResultItem) {
        when (item.type) {
            "destination" -> openSuggestionResult(item.destination ?: item.name)
            "place", "food" -> openReviewDetail(
                item.name,
                item.destination.orEmpty(),
                item.type
            )
            else -> openSuggestionResult(item.destination ?: item.name)
        }
    }

    private fun openReviewDetail(placeName: String, destination: String, type: String) {
        val intent = Intent(this, ReviewDetailActivity::class.java)
        intent.putExtra("placeName", placeName)
        intent.putExtra("destination", destination)
        intent.putExtra("type", type)
        startActivity(intent)
    }

    private fun showJoinTripDialog() {
        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etCode = EditText(this).apply {
            hint = "Enter invite code, e.g. ABC123"
            setTextColor(ContextCompat.getColor(this@HomeTripMateActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@HomeTripMateActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val etMessage = EditText(this).apply {
            hint = "Say something to the owner (optional)"
            minLines = 2
            setTextColor(ContextCompat.getColor(this@HomeTripMateActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@HomeTripMateActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        layout.addView(etCode)
        layout.addView(etMessage)

        AlertDialog.Builder(this)
            .setTitle("Join a Trip")
            .setView(layout)
            .setPositiveButton("Send Request") { _, _ ->
                val code = etCode.text.toString().trim()
                val message = etMessage.text.toString().trim()

                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter invite code", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sendJoinRequest(userId, code, message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendJoinRequest(userId: String, code: String, message: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.sendJoinRequest(
                    JoinTripRequestBody(
                        inviteCode = code,
                        userId = userId,
                        message = message
                    )
                )

                Toast.makeText(
                    this@HomeTripMateActivity,
                    response.message ?: "Join request sent",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Cannot send request"

                when {
                    msg.contains("Invalid invite code", ignoreCase = true) ||
                            msg.contains("404") -> {
                        Toast.makeText(
                            this@HomeTripMateActivity,
                            "Invalid invite code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("already pending", ignoreCase = true) ||
                            (msg.contains("409") && msg.contains("pending", ignoreCase = true)) -> {
                        Toast.makeText(
                            this@HomeTripMateActivity,
                            "Request already pending",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("already member", ignoreCase = true) ||
                            msg.contains("already a member", ignoreCase = true) -> {
                        Toast.makeText(
                            this@HomeTripMateActivity,
                            "You are already a member",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("owner", ignoreCase = true) -> {
                        Toast.makeText(
                            this@HomeTripMateActivity,
                            "You are already the owner",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Toast.makeText(
                            this@HomeTripMateActivity,
                            msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun openSuggestionResult(destination: String) {
        val intent = Intent(this, SuggestionResultActivity::class.java)
        intent.putExtra("destination", destination)
        startActivity(intent)
    }
}
