package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notingapp.R
import com.example.notingapp.adapters.TripAdapter
import com.example.notingapp.databinding.ActivityMyTripsBinding
import com.example.notingapp.model.JoinTripRequestBody
import com.example.notingapp.model.Trip
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class MyTripsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyTripsBinding

    private val adapter = TripAdapter()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getString("userId", null) ?: run {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvUserId.text = "User: $currentUserId"

        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = adapter

        adapter.onItemClick = { trip ->
            val intent = Intent(this, TripDetailActivity::class.java)
            intent.putExtra("tripId", trip._id)
            startActivity(intent)
        }

        adapter.onItemLongClick = { trip ->
            showTripActionDialog(trip)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            loadTrips(currentUserId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadTrips(currentUserId)
        }

        binding.btnJoinTrip.setOnClickListener {
            showJoinTripDialog()
        }

        binding.btnRequests.setOnClickListener {
            startActivity(Intent(this, TripRequestsActivity::class.java))
        }

        loadTrips(currentUserId)
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId.isNotEmpty()) {
            loadTrips(currentUserId)
        }
    }

    private fun showJoinTripDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etCode = EditText(this).apply {
            hint = "Enter invite code, e.g. ABC123"
            setTextColor(ContextCompat.getColor(this@MyTripsActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@MyTripsActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val etMessage = EditText(this).apply {
            hint = "Say something to the owner (optional)"
            minLines = 2
            setTextColor(ContextCompat.getColor(this@MyTripsActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@MyTripsActivity,
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

                sendJoinRequest(currentUserId, code, message)
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
                    this@MyTripsActivity,
                    response.message ?: "Join request sent",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Cannot send request"

                when {
                    msg.contains("Invalid invite code", ignoreCase = true) ||
                            msg.contains("404") -> {
                        Toast.makeText(
                            this@MyTripsActivity,
                            "Invalid invite code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("already pending", ignoreCase = true) ||
                            (msg.contains("409") && msg.contains("pending", ignoreCase = true)) -> {
                        Toast.makeText(
                            this@MyTripsActivity,
                            "Request already pending",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("already member", ignoreCase = true) ||
                            msg.contains("already a member", ignoreCase = true) -> {
                        Toast.makeText(
                            this@MyTripsActivity,
                            "You are already a member",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    msg.contains("owner", ignoreCase = true) -> {
                        Toast.makeText(
                            this@MyTripsActivity,
                            "You are already the owner",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Toast.makeText(
                            this@MyTripsActivity,
                            msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun loadTrips(userId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val trips = RetrofitClient.api.getTrips(userId)

                binding.progressBar.visibility = View.GONE

                if (trips.isEmpty()) {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    adapter.submitList(trips)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvError.text = "Error: ${e.localizedMessage ?: "Cannot load trips"}"
                binding.tvError.visibility = View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showTripActionDialog(trip: Trip) {
        val isOwner = trip.ownerId == currentUserId

        if (!isOwner) {
            AlertDialog.Builder(this)
                .setTitle("Shared trip")
                .setMessage("You are a member of this trip. Only the owner can delete it.")
                .setPositiveButton("Open Trip") { _, _ ->
                    val intent = Intent(this, TripDetailActivity::class.java)
                    intent.putExtra("tripId", trip._id)
                    startActivity(intent)
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(trip.title)
            .setMessage(
                "Choose an action for this trip.\n\n" +
                        "Delete will permanently remove this trip, including notes, members, reminders and ratings."
            )
            .setPositiveButton("Delete Trip") { _, _ ->
                showDeleteConfirmDialog(trip)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Open Trip") { _, _ ->
                val intent = Intent(this, TripDetailActivity::class.java)
                intent.putExtra("tripId", trip._id)
                startActivity(intent)
            }
            .show()
    }

    private fun showDeleteConfirmDialog(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Delete this trip?")
            .setMessage(
                "This action cannot be undone.\n\n" +
                        "Trip: ${trip.title}\n" +
                        "Destination: ${trip.destination}"
            )
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip(trip: Trip) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                RetrofitClient.api.deleteTrip(trip._id)

                Toast.makeText(
                    this@MyTripsActivity,
                    "Trip deleted",
                    Toast.LENGTH_SHORT
                ).show()

                loadTrips(currentUserId)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    this@MyTripsActivity,
                    "Delete failed: ${e.localizedMessage ?: "Cannot delete trip"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}