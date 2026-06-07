package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notingapp.adapters.PlaceAdapter
import com.example.notingapp.adapters.FoodAdapter
import com.example.notingapp.adapters.SuggestionChecklistAdapter
import com.example.notingapp.databinding.ActivitySuggestionResultBinding
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class SuggestionResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuggestionResultBinding

    private val placeAdapter = PlaceAdapter()
    private val foodAdapter = FoodAdapter()
    private val checklistAdapter = SuggestionChecklistAdapter()

    private var currentDestination: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuggestionResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getStringExtra("destination")
        if (destination.isNullOrBlank()) {
            Toast.makeText(this, "Missing destination", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentDestination = destination

        binding.tvDestination.text = destination
        binding.tvBadge.text = "Smart Suggestion"

        setupRecyclerViews()

        placeAdapter.onItemClick = { place ->
            openReviewDetail(place.name, currentDestination ?: "", "place")
        }
        foodAdapter.onItemClick = { food ->
            openReviewDetail(food.name, currentDestination ?: "", "food")
        }

        binding.btnCreateTrip.setOnClickListener {
            createTripFromSuggestion(destination)
        }

        loadSuggestions(destination)
    }

    private fun setupRecyclerViews() {
        binding.rvPlaces.apply {
            layoutManager = LinearLayoutManager(
                this@SuggestionResultActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = placeAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvFoods.apply {
            layoutManager = LinearLayoutManager(
                this@SuggestionResultActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = foodAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvChecklist.apply {
            layoutManager = LinearLayoutManager(this@SuggestionResultActivity)
            adapter = checklistAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadSuggestions(destination: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.tvError.text = ""

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getSuggestions(destination)

                binding.tvDestination.text = response.destination
                binding.tvIntro.text = response.intro

                placeAdapter.submitList(response.places)
                foodAdapter.submitList(response.foods)
                checklistAdapter.setItems(response.checklist)
            } catch (e: Exception) {
                binding.tvError.text = "Error: ${e.localizedMessage ?: "Cannot load suggestions"}"
                binding.tvError.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openReviewDetail(placeName: String, destination: String, type: String) {
        val intent = Intent(this, ReviewDetailActivity::class.java)
        intent.putExtra("placeName", placeName)
        intent.putExtra("destination", destination)
        intent.putExtra("type", type)
        startActivity(intent)
    }

    private fun createTripFromSuggestion(destination: String) {
        val selected = checklistAdapter.getSelectedItems()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        val allItems = checklistAdapter.getAllItems()
        val selectedTexts = selected.map { it.text }.toSet()
        val uncheckedTexts = allItems.filter { !selectedTexts.contains(it.text) }.map { it.text }

        val intent = Intent(this, CreateTripSetupActivity::class.java)
        intent.putExtra("destination", destination)
        intent.putStringArrayListExtra("uncheckedTexts", ArrayList(uncheckedTexts))
        startActivity(intent)
    }
}
