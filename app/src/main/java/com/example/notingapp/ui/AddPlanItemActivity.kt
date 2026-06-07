package com.example.notingapp.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notingapp.R
import com.example.notingapp.databinding.ActivityAddPlanItemBinding
import com.example.notingapp.model.AddPlanItemRequest
import com.example.notingapp.model.PlanItem
import com.example.notingapp.model.UpdatePlanItemRequest
import com.example.notingapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AddPlanItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPlanItemBinding

    private var tripId: String? = null
    private var mode: String = "add"
    private var editingItem: PlanItem? = null

    private var currentUserId: String? = null
    private var ownerId: String = ""
    private var tripMembers = listOf<String>()

    private var selectedType = "custom"
    private var selectedMember = ""
    private val typeButtons = mutableListOf<com.google.android.material.button.MaterialButton>()
    private val memberButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

    private val types = listOf("custom", "place", "food", "transport", "hotel")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlanItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getStringExtra("tripId") ?: run {
            Toast.makeText(this, "No tripId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mode = intent.getStringExtra("mode") ?: "add"
        currentUserId = getSharedPreferences("tripmate_prefs", Context.MODE_PRIVATE)
            .getString("userId", null)

        if (mode == "edit") {
            val itemId = intent.getStringExtra("itemId")
            val title = intent.getStringExtra("title") ?: ""
            val note = intent.getStringExtra("note")
            val type = intent.getStringExtra("type") ?: "custom"
            val startTime = intent.getStringExtra("startTime") ?: ""
            val endTime = intent.getStringExtra("endTime")
            val locationName = intent.getStringExtra("locationName")
            val assignedTo = intent.getStringExtra("assignedTo")
            editingItem = PlanItem(
                _id = itemId,
                title = title,
                note = note,
                type = type,
                startTime = startTime,
                endTime = endTime,
                locationName = locationName,
                assignedTo = assignedTo
            )
            binding.tvTitle.text = "Edit Plan Item"
            binding.etTitle.setText(title)
            binding.etNote.setText(note ?: "")
            binding.etStartTime.setText(startTime)
            binding.etEndTime.setText(endTime ?: "")
            binding.etLocationName.setText(locationName ?: "")
            selectedType = type
            selectedMember = assignedTo ?: ""
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { savePlanItem() }

        binding.etStartTime.setOnClickListener {
            showTimePicker(binding.etStartTime.text.toString()) {
                binding.etStartTime.setText(it)
            }
        }

        binding.etEndTime.setOnClickListener {
            showTimePicker(binding.etEndTime.text.toString()) {
                binding.etEndTime.setText(it)
            }
        }

        loadTripDetail()
    }

    private fun showTimePicker(initial: String?, onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        var hour = 8
        var minute = 0
        if (!initial.isNullOrBlank() && initial.contains(":")) {
            val parts = initial.split(":")
            hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        }
        TimePickerDialog(this, { _, h, m ->
            onPicked(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
    }

    private fun loadTripDetail() {
        lifecycleScope.launch {
            try {
                val trip = RetrofitClient.api.getTripDetail(tripId!!)
                ownerId = trip.ownerId
                tripMembers = trip.members
                renderTypeChips()
                renderMemberChips()
            } catch (e: Exception) {
                renderTypeChips()
                renderMemberChips()
            }
        }
    }

    private fun renderTypeChips() {
        binding.llTypeChips.removeAllViews()
        typeButtons.clear()

        types.forEach { type ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = type.replaceFirstChar { it.uppercase() }
                textSize = 11f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 0) }

                setOnClickListener {
                    selectedType = type
                    updateTypeButtons()
                }
            }
            typeButtons.add(btn)
            binding.llTypeChips.addView(btn)
        }
        updateTypeButtons()
    }

    private fun updateTypeButtons() {
        typeButtons.forEach { btn ->
            val isSelected = btn.text.toString().equals(selectedType.replaceFirstChar { it.uppercase() }, ignoreCase = true)
            if (isSelected) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun renderMemberChips() {
        binding.llMemberChips.removeAllViews()
        memberButtons.clear()

        val allMembers = (listOf(ownerId) + tripMembers).distinct()

        allMembers.forEach { member ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = member
                textSize = 11f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 0) }

                setOnClickListener {
                    selectedMember = if (selectedMember == member) "" else member
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
            val isSelected = btn.text.toString() == selectedMember
            if (isSelected) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.tripmate_primary))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.tripmate_primary))
            }
        }
    }

    private fun savePlanItem() {
        val title = binding.etTitle.text.toString().trim()
        val startTime = binding.etStartTime.text.toString().trim()
        val userId = currentUserId ?: return
        val tripId = this.tripId ?: return

        if (title.isEmpty()) {
            Toast.makeText(this, "Enter title", Toast.LENGTH_SHORT).show()
            return
        }
        if (startTime.isEmpty()) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show()
            return
        }

        val endTime = binding.etEndTime.text.toString().trim()
        if (endTime.isNotEmpty() && endTime < startTime) {
            Toast.makeText(this, "End time is earlier than start time", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            try {
                if (mode == "edit" && editingItem?._id != null) {
                    RetrofitClient.api.updatePlanItem(
                        tripId,
                        editingItem!!._id!!,
                        UpdatePlanItemRequest(
                            title = title,
                            note = binding.etNote.text.toString().trim(),
                            type = selectedType,
                            startTime = startTime,
                            endTime = binding.etEndTime.text.toString().trim(),
                            locationName = binding.etLocationName.text.toString().trim(),
                            assignedTo = selectedMember.ifEmpty { null },
                            userId = userId
                        )
                    )
                    Toast.makeText(this@AddPlanItemActivity, "Plan item updated!", Toast.LENGTH_SHORT).show()
                } else {
                    RetrofitClient.api.addPlanItem(
                        tripId,
                        AddPlanItemRequest(
                            title = title,
                            note = binding.etNote.text.toString().trim(),
                            type = selectedType,
                            startTime = startTime,
                            endTime = binding.etEndTime.text.toString().trim(),
                            locationName = binding.etLocationName.text.toString().trim(),
                            assignedTo = selectedMember,
                            userId = userId
                        )
                    )
                    Toast.makeText(this@AddPlanItemActivity, "Plan item added!", Toast.LENGTH_SHORT).show()
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddPlanItemActivity, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
