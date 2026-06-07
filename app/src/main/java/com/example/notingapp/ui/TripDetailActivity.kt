package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notingapp.R
import com.example.notingapp.adapters.FoodAdapter
import com.example.notingapp.adapters.PlaceAdapter
import com.example.notingapp.adapters.PlanItemAdapter
import com.example.notingapp.adapters.TripChecklistAdapter
import com.example.notingapp.databinding.ActivityTripDetailBinding
import com.example.notingapp.model.AddChecklistRequest
import com.example.notingapp.model.ChecklistUpdateRequest
import com.example.notingapp.model.DeleteChecklistRequest
import com.example.notingapp.model.DeletePlanItemRequest
import com.example.notingapp.model.FoodItem
import com.example.notingapp.model.GeneratePlanRequest
import com.example.notingapp.model.Place
import com.example.notingapp.model.PlanItem
import com.example.notingapp.model.RatingRequest
import com.example.notingapp.model.ShareTripRequest
import com.example.notingapp.model.TripChecklistItem
import com.example.notingapp.model.TriggerReminderRequest
import com.example.notingapp.model.UpdatePlanItemRequest
import com.example.notingapp.model.UpdateTripRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class TripDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailBinding

    private val placeAdapter = PlaceAdapter()
    private val foodAdapter = FoodAdapter()
    private val checklistAdapter = TripChecklistAdapter()
    private val planAdapter = PlanItemAdapter()

    private var currentTripId: String? = null
    private var currentUserId: String? = null

    private var ownerId: String = ""
    private var tripMembers = listOf<String>()
    private var currentInviteCode: String = ""

    private var selectedScore = 5
    private val scoreButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

    private var currentChecklist = listOf<TripChecklistItem>()
    private var selectedCategoryFilter = "All"

    private val defaultCategories = listOf(
        "All",
        "Shopping",
        "Transport",
        "Hotel",
        "Food",
        "Personal",
        "General"
    )

    private var customCategories = mutableListOf<String>()
    private val filterButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

    private var isReadOnly = false

    private val addTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                currentTripId?.let { loadTrip(it) }
                currentUserId?.let { loadActiveReminders(it) }
            }
        }

    private val planLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                currentTripId?.let { loadTrip(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentTripId = intent.getStringExtra("tripId") ?: run {
            Toast.makeText(this, "Missing trip ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserId = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
            .getString("userId", null)

        customCategories = getCustomCategories(currentTripId!!)
        renderFilterChips()

        setupRecyclerViews()
        setupScoreButtons()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            currentTripId?.let { loadTrip(it) }
        }

        binding.btnEditTrip.setOnClickListener {
            showEditTripDialog()
        }

        binding.btnCopyInviteCode.setOnClickListener {
            copyInviteCode()
        }

        binding.btnEndTrip.visibility = View.GONE
        binding.btnEndTrip.setOnClickListener {
            showEditTripDialog()
        }

        binding.btnAddTask.text = "Add Note"
        binding.btnAddTask.setOnClickListener {
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("tripId", currentTripId)
            intent.putExtra("mode", "add")
            addTaskLauncher.launch(intent)
        }

        binding.btnSubmitRating.setOnClickListener {
            submitRating()
        }

        checklistAdapter.onCheckChanged = onCheckChanged@ { itemId, checked ->
            val tripId = currentTripId
            val userId = currentUserId

            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onCheckChanged
            }

            if (tripId != null && userId != null) {
                updateChecklistDone(tripId, itemId, checked)
            }
        }

        checklistAdapter.onAssignClick = onAssignClick@ { itemId ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onAssignClick
            }

            showAssignMemberDialog(itemId)
        }

        checklistAdapter.onItemClick = onChecklistItemClick@ { item ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onChecklistItemClick
            }

            openEditNoteScreen(item)
        }

        checklistAdapter.onItemLongClick = onChecklistItemLongClick@ { item ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onChecklistItemLongClick
            }

            showDeleteNoteDialog(item)
        }

        placeAdapter.onAddClick = onPlaceAddClick@ { place ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onPlaceAddClick
            }

            addPlaceAsNote(place)
        }

        placeAdapter.onItemClick = { place ->
            openReviewDetail(place.name, binding.tvDestination.text.toString(), "place")
        }

        foodAdapter.onAddClick = onFoodAddClick@ { food ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onFoodAddClick
            }

            addFoodAsNote(food)
        }

        foodAdapter.onItemClick = { food ->
            openReviewDetail(food.name, binding.tvDestination.text.toString(), "food")
        }

        planAdapter.onItemClick = onPlanItemClick@ { item ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onPlanItemClick
            }

            openEditPlanItemScreen(item)
        }

        planAdapter.onItemLongClick = onPlanItemLongClick@ { item ->
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onPlanItemLongClick
            }

            showDeletePlanItemDialog(item)
        }

        planAdapter.onDoneToggle = onDoneToggle@ { itemId, done ->
            val tripId = currentTripId ?: return@onDoneToggle
            val userId = currentUserId ?: return@onDoneToggle

            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@onDoneToggle
            }

            updatePlanItemDone(tripId, itemId, done, userId)
        }

        binding.btnTabNotes.setOnClickListener {
            showTabNotes()
        }

        binding.btnTabPlan.setOnClickListener {
            showTabPlan()
        }

        binding.btnGeneratePlan.setOnClickListener {
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            generatePlan()
        }

        binding.btnAddPlanItem.setOnClickListener {
            if (isReadOnly) {
                Toast.makeText(this, "This trip is read-only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AddPlanItemActivity::class.java)
            intent.putExtra("tripId", currentTripId)
            intent.putExtra("mode", "add")
            planLauncher.launch(intent)
        }

        binding.cardRating.visibility = View.GONE

        binding.swipeRefresh.setOnRefreshListener {
            val tripId = currentTripId
            val userId = currentUserId
            if (tripId != null) loadTrip(tripId)
            if (userId != null) loadActiveReminders(userId)
            loadRatingSummary()
        }

        currentTripId?.let { loadTrip(it) }
        currentUserId?.let { loadActiveReminders(it) }
        loadRatingSummary()
        showTabNotes()
    }

    private fun openEditNoteScreen(item: TripChecklistItem) {
        val intent = Intent(this, AddTaskActivity::class.java)
        intent.putExtra("tripId", currentTripId)
        intent.putExtra("mode", "edit")
        intent.putExtra("itemId", item._id)
        intent.putExtra("text", item.text)
        intent.putExtra("category", item.category)
        intent.putExtra("assignedTo", item.assignedTo ?: "")
        intent.putExtra("reminderId", item.reminderId ?: "")
        addTaskLauncher.launch(intent)
    }

    private fun setupRecyclerViews() {
        binding.rvPlaces.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPlaces.adapter = placeAdapter

        binding.rvFoods.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFoods.adapter = foodAdapter

        binding.rvChecklist.layoutManager = LinearLayoutManager(this)
        binding.rvChecklist.adapter = checklistAdapter

        binding.rvPlanItems.layoutManager = LinearLayoutManager(this)
        binding.rvPlanItems.adapter = planAdapter
    }

    private fun setupScoreButtons() {
        scoreButtons.clear()

        scoreButtons.add(binding.btnScore1)
        scoreButtons.add(binding.btnScore2)
        scoreButtons.add(binding.btnScore3)
        scoreButtons.add(binding.btnScore4)
        scoreButtons.add(binding.btnScore5)

        scoreButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                selectedScore = index + 1
                updateScoreButtons()
            }
        }

        updateScoreButtons()
    }

    private fun updateScoreButtons() {
        scoreButtons.forEachIndexed { index, btn ->
            if (index + 1 == selectedScore) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun createChipLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 12, 0)
        }
    }

    private fun getCustomCategories(tripId: String): MutableList<String> {
        val prefs = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("custom_categories_$tripId", emptySet()) ?: emptySet()
        return set.toMutableList()
    }

    private fun saveCustomCategories(tripId: String, categories: List<String>) {
        getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("custom_categories_$tripId", categories.toSet())
            .apply()
    }

    private fun renderFilterChips() {
        binding.llFilterChips.removeAllViews()
        filterButtons.clear()

        val allCategories = defaultCategories + customCategories

        allCategories.forEach { category ->
            val isCustom = customCategories.contains(category)

            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = category
                textSize = 11f
                isAllCaps = false
                layoutParams = createChipLayoutParams()

                setOnClickListener {
                    selectedCategoryFilter = category
                    updateFilterButtons()
                    applyChecklistFilter()
                }

                if (isCustom) {
                    setOnLongClickListener {
                        showDeleteCategoryDialog(category)
                        true
                    }
                }
            }

            filterButtons.add(btn)
            binding.llFilterChips.addView(btn)
        }

        val addBtn = com.google.android.material.button.MaterialButton(this).apply {
            text = "＋ Category"
            textSize = 11f
            isAllCaps = false
            layoutParams = createChipLayoutParams()

            setOnClickListener {
                showAddCategoryDialog()
            }
        }

        binding.llFilterChips.addView(addBtn)
        updateFilterButtons()
    }

    private fun updateFilterButtons() {
        filterButtons.forEach { btn ->
            val isSelected = btn.text.toString() == selectedCategoryFilter

            if (isSelected) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun applyChecklistFilter() {
        val filtered = if (selectedCategoryFilter == "All") {
            currentChecklist
        } else {
            currentChecklist.filter {
                it.category.equals(selectedCategoryFilter, ignoreCase = true)
            }
        }

        checklistAdapter.submitList(filtered.toList())

        if (filtered.isEmpty() && selectedCategoryFilter != "All") {
            binding.tvChecklistProgress.text = "No notes in $selectedCategoryFilter"
        } else {
            val total = currentChecklist.size
            val done = currentChecklist.count { it.done }
            binding.tvChecklistProgress.text = "$done / $total done"
        }
    }

    private fun showAddCategoryDialog() {
        val total = defaultCategories.size + customCategories.size - 1

        if (total >= 20) {
            Toast.makeText(this, "Maximum 20 categories per trip", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = "New category name"
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    return@setPositiveButton
                }

                val all = defaultCategories + customCategories

                if (all.any { it.equals(name, ignoreCase = true) }) {
                    Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                customCategories.add(name)
                currentTripId?.let { saveCustomCategories(it, customCategories) }

                selectedCategoryFilter = name
                renderFilterChips()
                applyChecklistFilter()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete category?")
            .setMessage("This only removes the filter category. Existing notes will remain.")
            .setPositiveButton("Delete") { _, _ ->
                customCategories.remove(category)
                currentTripId?.let { saveCustomCategories(it, customCategories) }

                if (selectedCategoryFilter == category) {
                    selectedCategoryFilter = "All"
                }

                renderFilterChips()
                applyChecklistFilter()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadTrip(tripId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val trip = RetrofitClient.api.getTripDetail(tripId)

                binding.progressBar.visibility = View.GONE

                ownerId = trip.ownerId
                tripMembers = trip.members

                binding.tvTitle.text = trip.title
                binding.tvDestination.text = trip.destination
                binding.tvNote.text = trip.note ?: "No note"
                binding.tvOwner.text = "Owner: ${trip.ownerId}"
                binding.tvStatus.text = trip.status ?: "planning"
                binding.tvMembers.text =
                    "Members: ${(trip.members + trip.ownerId).distinct().joinToString(", ")}"
                binding.tvUpdatedBy.text =
                    trip.updatedBy?.let { "Last updated by $it" } ?: ""

                currentInviteCode = trip.inviteCode ?: ""

                if (currentInviteCode.isNotEmpty()) {
                    binding.tvInviteCode.text = currentInviteCode
                    binding.cardInviteCode.visibility = View.VISIBLE
                } else {
                    loadInviteCode(tripId)
                }

                placeAdapter.submitList(trip.places)
                foodAdapter.submitList(trip.foods)

                refreshChecklist(trip.checklist)
                refreshPlanItems(trip.planItems)

                isReadOnly = trip.status == "finished" || trip.status == "cancelled"
                applyReadOnlyLock()

                updateRatingVisibility()

                if (trip.places.isNotEmpty() && binding.etRatingPlace.text.isNullOrEmpty()) {
                    binding.etRatingPlace.setText(trip.places.first().name)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvError.text = "Error: ${e.localizedMessage ?: "Cannot load trip"}"
                binding.tvError.visibility = View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateRatingVisibility() {
        binding.cardRating.visibility = View.GONE
        binding.btnEndTrip.visibility = View.GONE
    }

    private fun loadActiveReminders(userId: String) {
        lifecycleScope.launch {
            try {
                val reminders = RetrofitClient.api.getActiveReminders(userId)
                binding.llActiveReminders.removeAllViews()

                val currentId = currentTripId
                val filtered = if (currentId != null) {
                    reminders.filter { it.tripId == currentId }
                } else {
                    reminders
                }

                if (filtered.isEmpty()) {
                    val tv = TextView(this@TripDetailActivity).apply {
                        text = "No active reminders for this trip"
                        textSize = 13f
                        setTextColor(
                            ContextCompat.getColor(
                                this@TripDetailActivity,
                                R.color.tripmate_text_secondary
                            )
                        )
                    }

                    binding.llActiveReminders.addView(tv)
                } else {
                    filtered.forEach { active ->
                        val card = LinearLayout(this@TripDetailActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 16, 16, 16)
                            setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_background
                                )
                            )
                        }

                        val tvTitle = TextView(this@TripDetailActivity).apply {
                            text = active.reminder.title
                            setTypeface(null, Typeface.BOLD)
                            textSize = 14f
                            setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_text
                                )
                            )
                        }

                        val tvDetail = TextView(this@TripDetailActivity).apply {
                            text =
                                "${active.reminder.locationName} • ${active.reminder.radiusMeters}m • ${active.tripTitle}"
                            textSize = 12f
                            setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_text_secondary
                                )
                            )
                        }

                        val btnSimulate =
                            com.google.android.material.button.MaterialButton(
                                this@TripDetailActivity
                            ).apply {
                                text = "Simulate Arrived"
                                isAllCaps = false
                                setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@TripDetailActivity,
                                        R.color.tripmate_accent
                                    )
                                )
                                cornerRadius = 24

                                setOnClickListener {
                                    simulateTrigger(active.tripId, active.reminder._id)
                                }
                            }

                        card.addView(tvTitle)
                        card.addView(tvDetail)
                        card.addView(btnSimulate)

                        binding.llActiveReminders.addView(card)

                        val divider = View(this@TripDetailActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            )
                            setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_text_secondary
                                )
                            )
                        }

                        binding.llActiveReminders.addView(divider)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun simulateTrigger(tripId: String, reminderId: String) {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.triggerReminder(
                    tripId,
                    reminderId,
                    TriggerReminderRequest(userId)
                )

                Toast.makeText(
                    this@TripDetailActivity,
                    response.message ?: "Simulated arrival!",
                    Toast.LENGTH_SHORT
                ).show()

                currentUserId?.let { loadActiveReminders(it) }

                response.reviewPrompt?.let { prompt ->
                    if (prompt.shouldAskReview) {
                        showReviewPromptDialog(prompt)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Trigger failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadInviteCode(tripId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getInviteCode(tripId)

                response.inviteCode?.let { code ->
                    currentInviteCode = code
                    binding.tvInviteCode.text = code
                    binding.cardInviteCode.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun copyInviteCode() {
        if (currentInviteCode.isEmpty()) return

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Invite Code", currentInviteCode)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "Invite code copied", Toast.LENGTH_SHORT).show()
    }

    private fun showReviewPromptDialog(prompt: com.example.notingapp.model.ReviewPrompt) {
        AlertDialog.Builder(this)
            .setTitle("You arrived at ${prompt.placeName}")
            .setMessage("Do you want to review this place?")
            .setPositiveButton("Review now") { _, _ ->
                showArrivedReviewDialog(prompt)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showArrivedReviewDialog(prompt: com.example.notingapp.model.ReviewPrompt) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val tvPlace = TextView(this).apply {
            text = "Place: ${prompt.placeName}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setPadding(0, 0, 0, 12)
        }

        val scoreLabel = TextView(this).apply {
            text = "Score (1-5)"
            textSize = 13f
            setTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val scoreLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        var selected = 5
        val scoreBtns = mutableListOf<com.google.android.material.button.MaterialButton>()

        (1..5).forEach { s ->
            val b = com.google.android.material.button.MaterialButton(this).apply {
                text = s.toString()
                textSize = 12f
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, 4, 0)
                    }

                setOnClickListener {
                    selected = s

                    scoreBtns.forEach { sb ->
                        val sel = sb.text.toString().toInt() == selected

                        if (sel) {
                            sb.setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_primary
                                )
                            )
                            sb.setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.white
                                )
                            )
                        } else {
                            sb.setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    android.R.color.transparent
                                )
                            )
                            sb.setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_primary
                                )
                            )
                        }
                    }
                }
            }

            scoreBtns.add(b)
            scoreLayout.addView(b)
        }

        scoreBtns.forEach { sb ->
            val sel = sb.text.toString().toInt() == selected

            if (sel) {
                sb.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                sb.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                sb.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                sb.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }

        val etComment = EditText(this).apply {
            hint = "Write a short review"
            minLines = 2
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        layout.addView(tvPlace)
        layout.addView(scoreLabel)
        layout.addView(scoreLayout)
        layout.addView(
            TextView(this).apply {
                text = "Comment"
                textSize = 13f
                setTextColor(
                    ContextCompat.getColor(
                        this@TripDetailActivity,
                        R.color.tripmate_text_secondary
                    )
                )
                setPadding(0, 12, 0, 4)
            }
        )
        layout.addView(etComment)

        val etImageUrl = EditText(this).apply {
            hint = "Image URL (optional)"
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }
        layout.addView(
            TextView(this).apply {
                text = "Image URL (optional)"
                textSize = 13f
                setTextColor(
                    ContextCompat.getColor(
                        this@TripDetailActivity,
                        R.color.tripmate_text_secondary
                    )
                )
                setPadding(0, 12, 0, 4)
            }
        )
        layout.addView(etImageUrl)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Review ${prompt.placeName}")
            .setView(layout)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            submitBtn.setOnClickListener {
                val comment = etComment.text.toString().trim()
                val imageUrl = etImageUrl.text.toString().trim()
                val images = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList()
                val tripId = currentTripId ?: return@setOnClickListener
                val userId = currentUserId ?: return@setOnClickListener

                lifecycleScope.launch {
                    try {
                        RetrofitClient.api.submitRating(
                            tripId,
                            RatingRequest(
                                placeName = prompt.placeName ?: "Unknown place",
                                userId = userId,
                                score = selected,
                                comment = comment,
                                imageUrls = images,
                                source = "location_reminder",
                                reminderId = prompt.reminderId ?: "",
                                checklistItemId = prompt.checklistItemId ?: ""
                            )
                        )

                        Toast.makeText(
                            this@TripDetailActivity,
                            "Thanks for your review!",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadRatingSummary()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: "Review failed"

                        when {
                            msg.contains("already reviewed", ignoreCase = true) -> {
                                Toast.makeText(
                                    this@TripDetailActivity,
                                    "You already reviewed this item",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            msg.contains("only review after arriving", ignoreCase = true) -> {
                                Toast.makeText(
                                    this@TripDetailActivity,
                                    "You can only review after arriving at this location",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                Toast.makeText(
                                    this@TripDetailActivity,
                                    "Review failed: $msg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun submitRating() {
        val tripId = currentTripId ?: return
        val userId = currentUserId ?: return

        val placeName = binding.etRatingPlace.text.toString().trim()
        val comment = binding.etRatingComment.text.toString().trim()

        if (placeName.isEmpty()) {
            Toast.makeText(this, "Please enter a place name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                RetrofitClient.api.submitRating(
                    tripId,
                    RatingRequest(
                        placeName = placeName,
                        userId = userId,
                        score = selectedScore,
                        comment = comment
                    )
                )

                Toast.makeText(
                    this@TripDetailActivity,
                    "Rating submitted!",
                    Toast.LENGTH_SHORT
                ).show()

                loadRatingSummary()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Submit failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadRatingSummary() {
        lifecycleScope.launch {
            try {
                val summary = RetrofitClient.api.getRatingSummary()
                binding.llRatingSummary.removeAllViews()

                if (summary.isEmpty()) {
                    val tv = TextView(this@TripDetailActivity).apply {
                        text = "No ratings yet"
                        textSize = 13f
                        setTextColor(
                            ContextCompat.getColor(
                                this@TripDetailActivity,
                                R.color.tripmate_text_secondary
                            )
                        )
                    }

                    binding.llRatingSummary.addView(tv)
                } else {
                    summary.forEach { item ->
                        val tv = TextView(this@TripDetailActivity).apply {
                            text = "${item._id}: ⭐ ${item.averageScore} (${item.totalReviews} reviews)"
                            textSize = 14f
                            setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_text
                                )
                            )
                            setPadding(0, 8, 0, 8)
                        }

                        binding.llRatingSummary.addView(tv)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun updateChecklistDone(tripId: String, itemId: String, checked: Boolean) {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                val updated = RetrofitClient.api.updateChecklist(
                    tripId,
                    itemId,
                    ChecklistUpdateRequest(
                        userId = userId,
                        done = checked
                    )
                )

                refreshChecklist(updated.checklist)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Update failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()

                loadTrip(tripId)
            }
        }
    }

    private fun showDeleteNoteDialog(item: TripChecklistItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete note?")
            .setMessage("“${item.text}”\n\nLong-press delete only removes this note.")
            .setPositiveButton("Delete") { _, _ ->
                currentTripId?.let { deleteChecklistItem(it, item._id) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAssignMemberDialog(itemId: String) {
        val members = (listOf(ownerId) + tripMembers)
            .filter { it.isNotBlank() }
            .distinct()

        if (members.isEmpty()) {
            Toast.makeText(this, "No members available", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val chipLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        var selected = ""
        val buttons = mutableListOf<com.google.android.material.button.MaterialButton>()

        members.forEach { member ->
            val button = com.google.android.material.button.MaterialButton(this).apply {
                text = member
                textSize = 11f
                isAllCaps = false
                layoutParams = createChipLayoutParams()

                setOnClickListener {
                    selected = member

                    buttons.forEach { btn ->
                        val isSelected = btn.text.toString() == selected

                        if (isSelected) {
                            btn.setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_primary
                                )
                            )
                            btn.setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.white
                                )
                            )
                        } else {
                            btn.setBackgroundColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    android.R.color.transparent
                                )
                            )
                            btn.setTextColor(
                                ContextCompat.getColor(
                                    this@TripDetailActivity,
                                    R.color.tripmate_primary
                                )
                            )
                        }
                    }
                }
            }

            buttons.add(button)
            chipLayout.addView(button)
        }

        layout.addView(
            TextView(this).apply {
                text = "Select member to assign"
                setPadding(0, 0, 0, 16)
                setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            }
        )

        layout.addView(chipLayout)

        AlertDialog.Builder(this)
            .setTitle("Assign Note")
            .setView(layout)
            .setPositiveButton("Assign") { _, _ ->
                if (selected.isNotEmpty()) {
                    currentTripId?.let { tripId ->
                        assignChecklistItem(tripId, itemId, selected)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun assignChecklistItem(tripId: String, itemId: String, assignTo: String) {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                val updated = RetrofitClient.api.updateChecklist(
                    tripId,
                    itemId,
                    ChecklistUpdateRequest(
                        userId = userId,
                        assignedTo = assignTo
                    )
                )

                refreshChecklist(updated.checklist)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Assign failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteChecklistItem(tripId: String, itemId: String) {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteChecklistItem(
                    tripId,
                    itemId,
                    DeleteChecklistRequest(userId)
                )

                val updatedTrip = response.trip

                if (updatedTrip != null) {
                    refreshChecklist(updatedTrip.checklist)
                } else {
                    loadTrip(tripId)
                }

                Toast.makeText(
                    this@TripDetailActivity,
                    response.message ?: "Note deleted",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Delete failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()

                loadTrip(tripId)
            }
        }
    }

    private fun refreshChecklist(checklist: List<TripChecklistItem>) {
        currentChecklist = checklist.toList()

        val categoriesFromItems = checklist.map { it.category }.distinct()
            .filter { cat -> defaultCategories.none { it.equals(cat, ignoreCase = true) } }

        if (categoriesFromItems.isNotEmpty()) {
            customCategories = (customCategories + categoriesFromItems).distinct().toMutableList()
            currentTripId?.let { saveCustomCategories(it, customCategories) }
        }

        renderFilterChips()
        applyChecklistFilter()

        val total = checklist.size
        val done = checklist.count { it.done }
        binding.tvChecklistProgress.text = "$done / $total done"
    }

    private fun addPlaceAsNote(place: Place) {
        addNoteFromText("Visit ${place.name}", "General")
    }

    private fun addFoodAsNote(food: FoodItem) {
        addNoteFromText("Try ${food.name}", "Food")
    }

    private fun addNoteFromText(text: String, category: String) {
        val userId = currentUserId ?: return
        val tripId = currentTripId ?: return

        lifecycleScope.launch {
            try {
                RetrofitClient.api.addChecklistItem(
                    tripId,
                    AddChecklistRequest(
                        text = text,
                        category = category,
                        assignedTo = "",
                        userId = userId
                    )
                )

                Toast.makeText(
                    this@TripDetailActivity,
                    "Added to notes",
                    Toast.LENGTH_SHORT
                ).show()

                loadTrip(tripId)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Add failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun showEditTripDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etTitle = EditText(this).apply {
            hint = "Title"
            setText(binding.tvTitle.text)
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val etNote = EditText(this).apply {
            hint = "Trip note"
            setText(binding.tvNote.text)
            minLines = 2
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val memberText = tripMembers
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        val etMembers = EditText(this).apply {
            hint = "Members, example: userB,userC"
            setText(memberText)
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val etShare = EditText(this).apply {
            hint = "Quick share with userId"
            setTextColor(ContextCompat.getColor(this@TripDetailActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@TripDetailActivity,
                    R.color.tripmate_text_secondary
                )
            )
        }

        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val statuses = listOf("planning", "finished", "cancelled")
        var selectedStatus = binding.tvStatus.text.toString().trim().ifEmpty { "planning" }

        if (!statuses.contains(selectedStatus)) {
            selectedStatus = "planning"
        }

        val statusButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

        fun updateStatusButtons() {
            statusButtons.forEach { btn ->
                val isSelected = btn.text.toString() == selectedStatus

                if (isSelected) {
                    btn.setBackgroundColor(
                        ContextCompat.getColor(
                            this@TripDetailActivity,
                            R.color.tripmate_primary
                        )
                    )
                    btn.setTextColor(
                        ContextCompat.getColor(
                            this@TripDetailActivity,
                            R.color.white
                        )
                    )
                } else {
                    btn.setBackgroundColor(
                        ContextCompat.getColor(
                            this@TripDetailActivity,
                            android.R.color.transparent
                        )
                    )
                    btn.setTextColor(
                        ContextCompat.getColor(
                            this@TripDetailActivity,
                            R.color.tripmate_primary
                        )
                    )
                }
            }
        }

        statuses.forEach { status ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = status
                textSize = 11f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 0, 8, 0)
                }

                setOnClickListener {
                    selectedStatus = status
                    updateStatusButtons()
                }
            }

            statusButtons.add(btn)
            statusLayout.addView(btn)
        }

        updateStatusButtons()

        layout.addView(etTitle)
        layout.addView(etNote)
        layout.addView(etMembers)

        layout.addView(
            TextView(this).apply {
                text = "Status"
                setPadding(0, 16, 0, 8)
                setTextColor(
                    ContextCompat.getColor(
                        this@TripDetailActivity,
                        R.color.tripmate_text_secondary
                    )
                )
            }
        )

        layout.addView(statusLayout)

        layout.addView(
            TextView(this).apply {
                text = "Share Trip"
                setPadding(0, 24, 0, 8)
                setTextColor(
                    ContextCompat.getColor(
                        this@TripDetailActivity,
                        R.color.tripmate_text_secondary
                    )
                )
            }
        )

        layout.addView(etShare)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Trip Info")
            .setView(layout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            saveBtn.setOnClickListener {
                val title = etTitle.text.toString().trim()
                val note = etNote.text.toString().trim()

                val membersRaw = etMembers.text.toString().trim()
                val members = if (membersRaw.isEmpty()) {
                    emptyList()
                } else {
                    membersRaw.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && it != ownerId }
                        .distinct()
                }

                val shareTarget = etShare.text.toString().trim()

                val userId = currentUserId ?: return@setOnClickListener
                val tripId = currentTripId ?: return@setOnClickListener

                lifecycleScope.launch {
                    try {
                        val updated = RetrofitClient.api.updateTrip(
                            tripId,
                            UpdateTripRequest(
                                title = title,
                                note = note,
                                members = members,
                                status = selectedStatus,
                                updatedBy = userId
                            )
                        )

                        ownerId = updated.ownerId
                        tripMembers = updated.members

                        binding.tvTitle.text = updated.title
                        binding.tvNote.text = updated.note ?: "No note"
                        binding.tvStatus.text = updated.status ?: "planning"
                        binding.tvMembers.text =
                            "Members: ${(updated.members + updated.ownerId).distinct().joinToString(", ")}"
                        binding.tvUpdatedBy.text =
                            updated.updatedBy?.let { "Last updated by $it" } ?: ""

                        updateRatingVisibility()

                        if (shareTarget.isNotEmpty()) {
                            shareTrip(tripId, shareTarget)
                        } else {
                            loadTrip(tripId)
                        }

                        Toast.makeText(
                            this@TripDetailActivity,
                            "Trip updated",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@TripDetailActivity,
                            "Update failed: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun endTrip() {
        showEditTripDialog()
    }

    private fun shareTrip(tripId: String, targetUserId: String) {
        val fromUserId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.shareTrip(
                    tripId,
                    ShareTripRequest(fromUserId, targetUserId)
                )

                Toast.makeText(
                    this@TripDetailActivity,
                    response.message ?: "Shared",
                    Toast.LENGTH_SHORT
                ).show()

                response.trip?.let { trip ->
                    ownerId = trip.ownerId
                    tripMembers = trip.members

                    binding.tvMembers.text =
                        "Members: ${(trip.members + trip.ownerId).distinct().joinToString(", ")}"

                    loadTrip(tripId)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Share failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showTabNotes() {
        binding.sectionNotes.visibility = View.VISIBLE
        binding.sectionPlan.visibility = View.GONE
        binding.btnTabNotes.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
        binding.btnTabPlan.setTextColor(ContextCompat.getColor(this, R.color.tripmate_text_secondary))
    }

    private fun showTabPlan() {
        binding.sectionNotes.visibility = View.GONE
        binding.sectionPlan.visibility = View.VISIBLE
        binding.btnTabNotes.setTextColor(ContextCompat.getColor(this, R.color.tripmate_text_secondary))
        binding.btnTabPlan.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
    }

    private fun normalizeTimeForSort(time: String?): String {
        val raw = time?.trim().orEmpty()
        if (raw.isEmpty()) return "99:99"
        val parts = raw.split(":")
        return if (parts.size >= 2) {
            val h = parts[0].padStart(2, '0')
            val m = parts[1].padStart(2, '0')
            "$h:$m"
        } else {
            raw.padStart(5, '0')
        }
    }

    private fun refreshPlanItems(planItems: List<PlanItem>) {
        val sorted = planItems.sortedWith(compareBy { normalizeTimeForSort(it.startTime) })
        planAdapter.submitList(sorted)
        binding.tvPlanEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun generatePlan(overwrite: Boolean = false) {
        val tripId = currentTripId ?: return
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val response = RetrofitClient.api.generatePlan(
                    tripId,
                    GeneratePlanRequest(userId = userId, overwrite = overwrite)
                )

                binding.progressBar.visibility = View.GONE
                response.trip?.let { loadTrip(tripId) }
                showTabPlan()

                Toast.makeText(
                    this@TripDetailActivity,
                    response.message ?: "Plan generated",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val msg = e.localizedMessage ?: ""
                if (msg.contains("409") || msg.contains("already exists", ignoreCase = true)) {
                    AlertDialog.Builder(this@TripDetailActivity)
                        .setTitle("Plan already exists")
                        .setMessage("Do you want to overwrite it?")
                        .setPositiveButton("Overwrite") { _, _ ->
                            generatePlan(overwrite = true)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    Toast.makeText(
                        this@TripDetailActivity,
                        "Generate failed: $msg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openEditPlanItemScreen(item: PlanItem) {
        val intent = Intent(this, AddPlanItemActivity::class.java)
        intent.putExtra("tripId", currentTripId)
        intent.putExtra("mode", "edit")
        intent.putExtra("itemId", item._id)
        intent.putExtra("title", item.title)
        intent.putExtra("note", item.note)
        intent.putExtra("type", item.type)
        intent.putExtra("startTime", item.startTime)
        intent.putExtra("endTime", item.endTime)
        intent.putExtra("locationName", item.locationName)
        intent.putExtra("assignedTo", item.assignedTo)
        planLauncher.launch(intent)
    }

    private fun showDeletePlanItemDialog(item: PlanItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete plan item?")
            .setMessage(item.title)
            .setPositiveButton("Delete") { _, _ ->
                deletePlanItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePlanItem(item: PlanItem) {
        val tripId = currentTripId ?: return
        val userId = currentUserId ?: return
        val itemId = item._id ?: return

        lifecycleScope.launch {
            try {
                RetrofitClient.api.deletePlanItem(
                    tripId,
                    itemId,
                    DeletePlanItemRequest(userId = userId)
                )

                loadTrip(tripId)
                Toast.makeText(this@TripDetailActivity, "Deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Delete failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updatePlanItemDone(tripId: String, itemId: String, done: Boolean, userId: String) {
        lifecycleScope.launch {
            try {
                RetrofitClient.api.updatePlanItem(
                    tripId,
                    itemId,
                    UpdatePlanItemRequest(
                        done = done,
                        userId = userId
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@TripDetailActivity,
                    "Update failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyReadOnlyLock() {
        val alpha = if (isReadOnly) 0.5f else 1.0f

        binding.btnAddTask.isEnabled = !isReadOnly
        binding.btnAddTask.alpha = alpha

        binding.btnGeneratePlan.isEnabled = !isReadOnly
        binding.btnGeneratePlan.alpha = alpha

        binding.btnAddPlanItem.isEnabled = !isReadOnly
        binding.btnAddPlanItem.alpha = alpha

        // Edit Trip stays enabled so owner can change status back to planning
        binding.btnEditTrip.isEnabled = true
        binding.btnEditTrip.alpha = 1.0f
    }
}