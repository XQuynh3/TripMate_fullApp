package com.example.notingapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notingapp.adapters.ReviewAdapter
import com.example.notingapp.databinding.ActivityReviewDetailBinding
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding
    private val reviewAdapter = ReviewAdapter()

    private var placeName: String = ""
    private var destination: String = ""
    private var type: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        placeName = intent.getStringExtra("placeName") ?: ""
        destination = intent.getStringExtra("destination") ?: ""
        type = intent.getStringExtra("type") ?: ""

        if (placeName.isEmpty()) {
            Toast.makeText(this, "Missing place name", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvPlaceName.text = placeName
        val typeLabel = when (type) {
            "place" -> "Place"
            "food" -> "Food"
            else -> type.replaceFirstChar { it.uppercase() }
        }
        binding.tvSubtitle.text = if (destination.isNotEmpty()) {
            "$typeLabel \u00b7 $destination"
        } else {
            typeLabel
        }

        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        binding.rvReviews.adapter = reviewAdapter

        binding.btnBack.setOnClickListener { finish() }

        binding.swipeRefresh.setOnRefreshListener {
            loadReviews()
        }

        loadReviews()
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            try {
                val detail = RetrofitClient.api.getPlaceReviewDetail(placeName)
                binding.swipeRefresh.isRefreshing = false

                val avg = detail.averageScore
                val count = detail.totalReviews

                if (count > 0) {
                    binding.tvAverageScore.text = String.format(Locale.US, "%.1f", avg)
                    binding.tvStars.text = buildStars(avg)
                    binding.tvReviewCount.text = "Based on $count reviews"
                } else {
                    binding.tvAverageScore.text = "-"
                    binding.tvStars.text = ""
                    binding.tvReviewCount.text = "No reviews yet"
                }

                reviewAdapter.submitList(detail.reviews)

                if (detail.reviews.isEmpty()) {
                    binding.tvEmptyReviews.visibility = View.VISIBLE
                    binding.rvReviews.visibility = View.GONE
                } else {
                    binding.tvEmptyReviews.visibility = View.GONE
                    binding.rvReviews.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@ReviewDetailActivity,
                    "Load failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buildStars(score: Double): String {
        val filled = "\u2605"
        val empty = "\u2606"
        val rounded = score.toInt().coerceIn(0, 5)
        return (1..5).joinToString("") { if (it <= rounded) filled else empty }
    }
}
