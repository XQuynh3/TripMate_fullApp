package com.example.notingapp.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.R
import com.example.notingapp.databinding.ActivityTripRequestsBinding
import com.example.notingapp.model.AcceptRejectRequestBody
import com.example.notingapp.model.TripJoinRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class TripRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripRequestsBinding
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
            .getString("userId", null) ?: run {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            loadRequests()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadRequests()
        }

        loadRequests()
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getTripRequests(currentUserId)
                renderOwnerRequests(response.ownerRequests)
                renderMyRequests(response.myRequests)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripRequestsActivity,
                    "Load failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun renderOwnerRequests(requests: List<TripJoinRequest>) {
        binding.llOwnerRequests.removeAllViews()

        if (requests.isEmpty()) {
            binding.tvEmptyOwner.visibility = View.VISIBLE
            return
        }

        binding.tvEmptyOwner.visibility = View.GONE

        requests.forEach { req ->
            val card = createRequestCard(req, isOwner = true)
            binding.llOwnerRequests.addView(card)
        }
    }

    private fun renderMyRequests(requests: List<TripJoinRequest>) {
        binding.llMyRequests.removeAllViews()

        if (requests.isEmpty()) {
            binding.tvEmptyMy.visibility = View.VISIBLE
            return
        }

        binding.tvEmptyMy.visibility = View.GONE

        requests.forEach { req ->
            val card = createRequestCard(req, isOwner = false)
            binding.llMyRequests.addView(card)
        }
    }

    private fun createRequestCard(req: TripJoinRequest, isOwner: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(
                ContextCompat.getColor(
                    this@TripRequestsActivity,
                    R.color.tripmate_card
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }

        val tvTitle = TextView(this).apply {
            text = req.tripTitle ?: "Trip"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(
                ContextCompat.getColor(
                    this@TripRequestsActivity,
                    R.color.tripmate_text
                )
            )
        }

        val tvDetail = TextView(this).apply {
            text = buildString {
                append(req.destination ?: "")

                if (isOwner) {
                    append("\nFrom: ${req.fromUserId}")
                } else {
                    append("\nOwner: ${req.toUserId}")
                }

                if (!req.message.isNullOrBlank()) {
                    append("\nMessage: ${req.message}")
                }
            }
            textSize = 12f
            setTextColor(
                ContextCompat.getColor(
                    this@TripRequestsActivity,
                    R.color.tripmate_text_secondary
                )
            )
            setPadding(0, 4, 0, 8)
        }

        val tvStatus = TextView(this).apply {
            text = req.status.uppercase()
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 8)

            val color = when (req.status) {
                "accepted" -> R.color.tripmate_primary
                "rejected" -> R.color.tripmate_error
                else -> R.color.tripmate_accent
            }

            setTextColor(
                ContextCompat.getColor(
                    this@TripRequestsActivity,
                    color
                )
            )
        }

        card.addView(tvTitle)
        card.addView(tvDetail)
        card.addView(tvStatus)

        if (isOwner && req.status == "pending") {
            val btnLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnAccept = com.google.android.material.button.MaterialButton(this).apply {
                text = "Accept"
                textSize = 12f
                isAllCaps = false
                setBackgroundColor(
                    ContextCompat.getColor(
                        this@TripRequestsActivity,
                        R.color.tripmate_primary
                    )
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@TripRequestsActivity,
                        R.color.white
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 0, 8, 0)
                }
                setOnClickListener {
                    acceptRequest(req._id)
                }
            }

            val btnReject = com.google.android.material.button.MaterialButton(this).apply {
                text = "Reject"
                textSize = 12f
                isAllCaps = false
                setBackgroundColor(
                    ContextCompat.getColor(
                        this@TripRequestsActivity,
                        R.color.tripmate_error
                    )
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@TripRequestsActivity,
                        R.color.white
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    rejectRequest(req._id)
                }
            }

            btnLayout.addView(btnAccept)
            btnLayout.addView(btnReject)
            card.addView(btnLayout)
        }

        if (!isOwner && req.status == "accepted") {
            val tvHint = TextView(this).apply {
                text = "This trip will appear in My Trips."
                textSize = 11f
                setTextColor(
                    ContextCompat.getColor(
                        this@TripRequestsActivity,
                        R.color.tripmate_text_secondary
                    )
                )
            }
            card.addView(tvHint)
        }

        return card
    }

    private fun acceptRequest(requestId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.acceptTripRequest(
                    requestId,
                    AcceptRejectRequestBody(currentUserId)
                )

                Toast.makeText(
                    this@TripRequestsActivity,
                    response.message ?: "Accepted",
                    Toast.LENGTH_SHORT
                ).show()

                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripRequestsActivity,
                    "Accept failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejectRequest(requestId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.rejectTripRequest(
                    requestId,
                    AcceptRejectRequestBody(currentUserId)
                )

                Toast.makeText(
                    this@TripRequestsActivity,
                    response.message ?: "Rejected",
                    Toast.LENGTH_SHORT
                ).show()

                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripRequestsActivity,
                    "Reject failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}