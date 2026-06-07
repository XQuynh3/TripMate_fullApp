package com.example.notingapp.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.notingapp.R
import com.example.notingapp.model.Note
import com.example.notingapp.model.Tag
import com.example.notingapp.viewmodel.TagViewModel
import com.example.notingapp.viewmodel.NoteViewModel
import com.example.notingapp.location.GeofenceHelper
import com.google.android.gms.location.LocationServices

class CreateNoteActivity : AppCompatActivity() {

    lateinit var titleInput: EditText
    lateinit var contentInput: EditText
    lateinit var tagSpinner: Spinner
    lateinit var boldBtn: Button
    lateinit var italicBtn: Button
    lateinit var textSizeSeek: SeekBar
    lateinit var saveBtn: Button
    lateinit var addTagBtn: Button
    lateinit var locationBtn: Button
    lateinit var checkboxBtn: Button
    lateinit var removeLocationBtn: Button

    private val tagViewModel: TagViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()

    private var tagList: List<Tag> = emptyList()

    var selectedTag = ""
    var selectedPosition = 0
    var textSize = 16f

    private var noteId = -1
    private var currentNote: Note? = null

    var latitude: Double? = null
    var longitude: Double? = null
    var locationName: String? = null

    private var pendingGeofenceNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        tagSpinner = findViewById(R.id.tagSpinner)
        boldBtn = findViewById(R.id.boldBtn)
        italicBtn = findViewById(R.id.italicBtn)
        textSizeSeek = findViewById(R.id.textSizeSeek)
        saveBtn = findViewById(R.id.saveBtn)
        addTagBtn = findViewById(R.id.addTagBtn)
        locationBtn = findViewById(R.id.locationBtn)
        checkboxBtn = findViewById(R.id.checkboxBtn)
        removeLocationBtn = findViewById(R.id.removeLocationBtn)

        noteId = intent.getIntExtra("noteId", -1)

        setupTagSpinner()
        setupTextSize()
        setupStyleButtons()
        setupChecklist()
        setupRemoveLocation()

        locationBtn.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivityForResult(intent, 100)
        }

        addTagBtn.setOnClickListener {
            showAddTagDialog()
        }

        tagSpinner.setOnLongClickListener {
            deleteCurrentTag()
            true
        }

        if (noteId != -1) {
            loadNoteForEdit(noteId)
        }

        saveBtn.setOnClickListener {
            saveNote()
        }
    }

    private fun setupChecklist() {
        checkboxBtn.setOnClickListener {
            val cursorPos = contentInput.selectionStart
            val text = contentInput.text

            if (cursorPos != 0 && text[cursorPos - 1].toString() != "\n") {
                text.insert(cursorPos, "\n")
            }

            text.insert(contentInput.selectionStart, "☐ ")
        }

        contentInput.setOnTouchListener { v: View, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as EditText
                val cursor = editText.selectionStart
                val text = editText.text.toString()

                val start = text.lastIndexOf('\n', cursor - 1) + 1
                val end = text.indexOf('\n', cursor).let {
                    if (it == -1) text.length else it
                }

                val line = text.substring(start, end)

                if (line.startsWith("☐") || line.startsWith("☑")) {
                    val newLine = if (line.startsWith("☐")) {
                        line.replaceFirst("☐", "☑")
                    } else {
                        line.replaceFirst("☑", "☐")
                    }

                    editText.text.replace(start, end, newLine)
                }
            }

            false
        }
    }

    private fun setupRemoveLocation() {
        removeLocationBtn.setOnClickListener {
            if (latitude == null) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Remove location?")
                .setMessage("This will remove location reminder from this note.")
                .setPositiveButton("Remove") { _, _ ->
                    val client = LocationServices.getGeofencingClient(this)

                    currentNote?.let {
                        client.removeGeofences(listOf(it.id.toString()))
                    }

                    latitude = null
                    longitude = null
                    locationName = null

                    Toast.makeText(this, "Location removed", Toast.LENGTH_SHORT).show()

                    removeLocationBtn.visibility = View.GONE
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            latitude = data?.getDoubleExtra("lat", 0.0)
            longitude = data?.getDoubleExtra("lng", 0.0)
            locationName = data?.getStringExtra("locationName")

            removeLocationBtn.visibility = View.VISIBLE

            Toast.makeText(this, locationName, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTagSpinner() {
        tagViewModel.tags.observe(this) { tags ->
            tagList = tags

            val tagNames = mutableListOf(
                "All",
                "Idea",
                "Personal",
                "Study",
                "Work"
            )

            tags.forEach { tag ->
                if (!tagNames.contains(tag.name)) {
                    tagNames.add(tag.name)
                }
            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                tagNames
            )

            tagSpinner.adapter = adapter

            tagSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedPosition = position
                        selectedTag = tagNames[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            currentNote?.let { note ->
                val index = tagNames.indexOf(note.tag)
                if (index >= 0) {
                    tagSpinner.setSelection(index)
                }
            }
        }
    }

    private fun setupTextSize() {
        textSizeSeek.max = 40
        textSizeSeek.progress = 16

        textSizeSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textSize = progress.toFloat()
                    contentInput.textSize = textSize
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
    }

    private fun setupStyleButtons() {
        boldBtn.setOnClickListener {
            contentInput.setTypeface(null, Typeface.BOLD)
        }

        italicBtn.setOnClickListener {
            contentInput.setTypeface(null, Typeface.ITALIC)
        }
    }

    private fun loadNoteForEdit(id: Int) {
        noteViewModel.notes.observe(this) { notes ->
            val note = notes.find { it.id == id } ?: return@observe

            currentNote = note

            titleInput.setText(note.title)
            contentInput.setText(note.content)

            textSize = note.textSize
            contentInput.textSize = textSize
            textSizeSeek.progress = textSize.toInt()

            latitude = note.latitude
            longitude = note.longitude
            locationName = note.locationName

            if (latitude != null) {
                removeLocationBtn.visibility = View.VISIBLE
            } else {
                removeLocationBtn.visibility = View.GONE
            }
        }
    }

    private fun showAddTagDialog() {
        val input = EditText(this)
        input.hint = "Enter tag name"

        AlertDialog.Builder(this)
            .setTitle("Add Tag")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val tagName = input.text.toString().trim()

                if (tagName.isNotEmpty()) {
                    tagViewModel.insert(Tag(name = tagName))
                    Toast.makeText(this, "Tag added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Tag name is empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurrentTag() {}

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showPermissionSettingsDialog()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            999
        )
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cần cấp quyền vị trí")
            .setMessage("Bạn đã từ chối quyền. Hãy vào Cài đặt để cấp quyền cho app.")
            .setPositiveButton("Mở cài đặt") { _, _ ->
                val intent = Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                )

                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri

                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addGeofence(note: Note) {
        try {
            val geofenceHelper = GeofenceHelper(this)

            val geofence = geofenceHelper.getGeofence(
                note.id.toString(),
                note.latitude!!,
                note.longitude!!
            )

            val request = geofenceHelper.getGeofencingRequest(geofence)

            val client = LocationServices.getGeofencingClient(this)

            client.addGeofences(
                request,
                geofenceHelper.getPendingIntent()
            )

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun saveNote() {
        val isEdit = noteId != -1

        val note = if (isEdit) {
            currentNote!!.copy(
                title = titleInput.text.toString(),
                content = contentInput.text.toString(),
                tag = selectedTag,
                textSize = textSize,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName
            )
        } else {
            Note(
                title = titleInput.text.toString(),
                content = contentInput.text.toString(),
                tag = selectedTag,
                textSize = textSize,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName
            )
        }

        if (latitude == null || longitude == null) {
            if (isEdit) noteViewModel.update(note)
            else noteViewModel.insert(note)

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!hasLocationPermission()) {
            pendingGeofenceNote = note
            requestLocationPermission()
            return
        }

        if (isEdit) noteViewModel.update(note)
        else noteViewModel.insert(note)

        addGeofence(note)

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 999) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                pendingGeofenceNote?.let { note ->
                    val isEdit = note.id != 0

                    if (isEdit) noteViewModel.update(note)
                    else noteViewModel.insert(note)

                    addGeofence(note)

                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } else {
                pendingGeofenceNote?.let { note ->
                    val noteWithoutLocation = note.copy(
                        latitude = null,
                        longitude = null,
                        locationName = null
                    )

                    val isEdit = noteWithoutLocation.id != 0

                    if (isEdit) noteViewModel.update(noteWithoutLocation)
                    else noteViewModel.insert(noteWithoutLocation)

                    Toast.makeText(this, "Saved without location", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}