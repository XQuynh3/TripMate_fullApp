package com.example.notingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.R
import com.example.notingapp.databinding.ActivityAddTaskBinding
import com.example.notingapp.model.AddChecklistRequest
import com.example.notingapp.model.ChecklistUpdateRequest
import com.example.notingapp.model.LocationReminderRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AddTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTaskBinding

    private var tripId: String? = null
    private var currentUserId: String? = null

    private var mode: String = "add"
    private var editingItemId: String? = null

    private var ownerId: String = ""
    private var tripMembers = listOf<String>()

    private var selectedCategory = "General"
    private var selectedMember = ""

    private val defaultCategories = listOf(
        "Shopping",
        "Transport",
        "Hotel",
        "Food",
        "Personal",
        "General"
    )

    private var customCategories = mutableListOf<String>()

    private val categoryButtons =
        mutableListOf<com.google.android.material.button.MaterialButton>()

    private val memberButtons =
        mutableListOf<com.google.android.material.button.MaterialButton>()

    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data

                val lat = data?.getDoubleExtra("lat", 0.0) ?: 0.0
                val lng = data?.getDoubleExtra("lng", 0.0) ?: 0.0
                val name = data?.getStringExtra("locationName") ?: ""

                binding.etReminderLat.setText(lat.toString())
                binding.etReminderLng.setText(lng.toString())
                binding.etReminderLocationName.setText(name)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getStringExtra("tripId") ?: run {
            Toast.makeText(this, "Missing trip ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mode = intent.getStringExtra("mode") ?: "add"
        editingItemId = intent.getStringExtra("itemId")

        currentUserId = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
            .getString("userId", null)

        setupModeFromIntent()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            if (mode == "edit") {
                updateNote()
            } else {
                saveTask()
            }
        }

        binding.btnChooseOnMap.setOnClickListener {
            mapLauncher.launch(Intent(this, MapActivity::class.java))
        }

        binding.swReminder.setOnCheckedChangeListener { _, isChecked ->
            binding.llReminderSection.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        setupReminderPresets()
        loadTripDetail()
    }

    private fun setupModeFromIntent() {
        if (mode == "edit") {
            val text = intent.getStringExtra("text") ?: ""
            val category = intent.getStringExtra("category") ?: "General"
            val assignedTo = intent.getStringExtra("assignedTo") ?: ""

            binding.etTaskText.setText(text)
            selectedCategory = category
            selectedMember = assignedTo

            // Nếu layout có title text riêng thì để đó qua XML. Nếu không, đổi nút là đủ.
            binding.btnSave.text = "Save Note"
        } else {
            binding.btnSave.text = "Add Note"
        }

        // Wording cho UI: dùng note thay vì task
        binding.etTaskText.hint = "Write note, e.g. Buy water and snacks"
    }

    private fun loadTripDetail() {
        lifecycleScope.launch {
            try {
                val trip = RetrofitClient.api.getTripDetail(tripId!!)

                ownerId = trip.ownerId
                tripMembers = trip.members
                customCategories = getCustomCategories(tripId!!)

                val backendCustom = trip.customCategories
                if (backendCustom.isNotEmpty()) {
                    customCategories = (customCategories + backendCustom).distinct().toMutableList()
                    saveCustomCategories(tripId!!, customCategories)
                }

                renderCategoryChips()
                renderMemberChips()

                if (mode == "edit") {
                    val reminderId = intent.getStringExtra("reminderId")
                    if (!reminderId.isNullOrEmpty()) {
                        val reminder = trip.locationReminders.find { it._id == reminderId }
                        reminder?.let {
                            binding.swReminder.isChecked = true
                            binding.llReminderSection.visibility = View.VISIBLE
                            binding.etReminderTitle.setText(it.title)
                            binding.etReminderMessage.setText(it.message ?: "")
                            binding.etReminderLocationName.setText(it.locationName ?: "")
                            binding.etReminderLat.setText(it.latitude.toString())
                            binding.etReminderLng.setText(it.longitude.toString())
                            binding.etReminderRadius.setText((it.radiusMeters ?: 200).toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddTaskActivity,
                    "Load trip failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()

                renderCategoryChips()
                renderMemberChips()
            }
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

    private fun createChipLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 12, 0)
        }
    }

    private fun renderCategoryChips() {
        binding.llCategoryChips.removeAllViews()
        categoryButtons.clear()

        val allCategories = (defaultCategories + customCategories).distinct()

        allCategories.forEach { category ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = category
                textSize = 11f
                isAllCaps = false
                layoutParams = createChipLayoutParams()

                setOnClickListener {
                    selectedCategory = category
                    updateCategoryButtons()
                }
            }

            categoryButtons.add(btn)
            binding.llCategoryChips.addView(btn)
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

        binding.llCategoryChips.addView(addBtn)
        updateCategoryButtons()
    }

    private fun updateCategoryButtons() {
        categoryButtons.forEach { btn ->
            val isSelected = btn.text.toString() == selectedCategory

            if (isSelected) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun showAddCategoryDialog() {
        val total = defaultCategories.size + customCategories.size

        if (total >= 20) {
            Toast.makeText(this, "Maximum 20 categories per trip", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = "New category name"
            setTextColor(ContextCompat.getColor(this@AddTaskActivity, R.color.tripmate_text))
            setHintTextColor(
                ContextCompat.getColor(
                    this@AddTaskActivity,
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
                tripId?.let { saveCustomCategories(it, customCategories) }

                selectedCategory = name
                renderCategoryChips()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderMemberChips() {
        binding.llMemberChips.removeAllViews()
        memberButtons.clear()

        val members = (listOf(ownerId) + tripMembers)
            .filter { it.isNotBlank() }
            .distinct()

        val unassignedBtn = com.google.android.material.button.MaterialButton(this).apply {
            text = "Unassigned"
            textSize = 11f
            isAllCaps = false
            layoutParams = createChipLayoutParams()

            setOnClickListener {
                selectedMember = ""
                updateMemberButtons()
            }
        }

        memberButtons.add(unassignedBtn)
        binding.llMemberChips.addView(unassignedBtn)

        members.forEach { member ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = member
                textSize = 11f
                isAllCaps = false
                layoutParams = createChipLayoutParams()

                setOnClickListener {
                    selectedMember = member
                    updateMemberButtons()
                }
            }

            memberButtons.add(btn)
            binding.llMemberChips.addView(btn)
        }

        updateMemberButtons()
    }

    private fun updateMemberButtons() {
        memberButtons.forEach { btn ->
            val isSelected = if (selectedMember.isEmpty()) {
                btn.text.toString() == "Unassigned"
            } else {
                btn.text.toString() == selectedMember
            }

            if (isSelected) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun setupReminderPresets() {
        binding.chipSupermarket.setOnClickListener {
            applyPreset(
                title = "Tới siêu thị thì mua đồ",
                message = "Mua nước, snack, khăn giấy",
                locationName = "Siêu thị",
                lat = 10.762622,
                lng = 106.660172,
                radius = 200
            )
        }

        binding.chipBusStation.setOnClickListener {
            applyPreset(
                title = "Tới bến xe thì kiểm tra vé",
                message = "Kiểm tra vé xe, giấy tờ",
                locationName = "Bến xe",
                lat = 10.768,
                lng = 106.681,
                radius = 200
            )
        }

        binding.chipHotel.setOnClickListener {
            applyPreset(
                title = "Tới khách sạn thì check-in",
                message = "Chuẩn bị CCCD, booking",
                locationName = "Khách sạn",
                lat = 10.775,
                lng = 106.7,
                radius = 200
            )
        }

        binding.chipBeach.setOnClickListener {
            applyPreset(
                title = "Tới bãi biển thì lấy đồ chống nắng",
                message = "Mang kem chống nắng, nước",
                locationName = "Bãi biển",
                lat = 10.334,
                lng = 107.084,
                radius = 300
            )
        }
    }

    private fun applyPreset(
        title: String,
        message: String,
        locationName: String,
        lat: Double,
        lng: Double,
        radius: Int
    ) {
        binding.etReminderTitle.setText(title)
        binding.etReminderMessage.setText(message)
        binding.etReminderLocationName.setText(locationName)
        binding.etReminderLat.setText(lat.toString())
        binding.etReminderLng.setText(lng.toString())
        binding.etReminderRadius.setText(radius.toString())
    }

    private fun saveTask() {
        val text = binding.etTaskText.text.toString().trim()
        val userId = currentUserId ?: return
        val tripId = this.tripId ?: return

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val updatedTrip = RetrofitClient.api.addChecklistItem(
                    tripId,
                    AddChecklistRequest(
                        text = text,
                        category = selectedCategory,
                        assignedTo = selectedMember,
                        userId = userId
                    )
                )

                if (binding.swReminder.isChecked) {
                    val newItem = updatedTrip.checklist.findLast { it.text == text && it.category == selectedCategory }
                    saveReminder(tripId, userId, text, newItem?._id)
                }

                Toast.makeText(this@AddTaskActivity, "Note added!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTaskActivity, "Add failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNote() {
        val text = binding.etTaskText.text.toString().trim()
        val userId = currentUserId ?: return
        val tripId = this.tripId ?: return
        val itemId = editingItemId ?: return

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                RetrofitClient.api.updateChecklist(
                    tripId,
                    itemId,
                    ChecklistUpdateRequest(
                        userId = userId,
                        text = text,
                        title = text,
                        category = selectedCategory,
                        assignedTo = selectedMember
                    )
                )

                if (binding.swReminder.isChecked) {
                    val reminderId = intent.getStringExtra("reminderId")
                    if (!reminderId.isNullOrEmpty()) {
                        updateExistingReminder(tripId, reminderId, userId, text, itemId)
                    } else {
                        saveReminder(tripId, userId, text, itemId)
                    }
                }

                Toast.makeText(this@AddTaskActivity, "Note updated!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTaskActivity, "Update failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveReminder(tripId: String, userId: String, taskText: String, checklistItemId: String?) {
        val title = binding.etReminderTitle.text.toString().trim().ifEmpty { taskText }
        val message = binding.etReminderMessage.text.toString().trim().ifEmpty { taskText }
        val locationName = binding.etReminderLocationName.text.toString().trim()
        val lat = binding.etReminderLat.text.toString().toDoubleOrNull()
        val lng = binding.etReminderLng.text.toString().toDoubleOrNull()
        val radius = binding.etReminderRadius.text.toString().toIntOrNull() ?: 200

        if (lat == null || lng == null) {
            runOnUiThread { Toast.makeText(this, "Reminder skipped: no location selected", Toast.LENGTH_SHORT).show() }
            return
        }

        try {
            RetrofitClient.api.createLocationReminder(
                tripId,
                LocationReminderRequest(
                    title = title,
                    message = message,
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng,
                    radiusMeters = radius,
                    userId = userId,
                    checklistItemId = checklistItemId
                )
            )
            runOnUiThread { Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Reminder failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun updateExistingReminder(tripId: String, reminderId: String, userId: String, taskText: String, checklistItemId: String?) {
        val title = binding.etReminderTitle.text.toString().trim().ifEmpty { taskText }
        val message = binding.etReminderMessage.text.toString().trim().ifEmpty { taskText }
        val locationName = binding.etReminderLocationName.text.toString().trim()
        val lat = binding.etReminderLat.text.toString().toDoubleOrNull()
        val lng = binding.etReminderLng.text.toString().toDoubleOrNull()
        val radius = binding.etReminderRadius.text.toString().toIntOrNull() ?: 200

        if (lat == null || lng == null) return

        try {
            RetrofitClient.api.updateLocationReminder(
                tripId,
                reminderId,
                LocationReminderRequest(
                    title = title,
                    message = message,
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng,
                    radiusMeters = radius,
                    userId = userId,
                    checklistItemId = checklistItemId
                )
            )
        } catch (_: Exception) { }
    }
}