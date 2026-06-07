package com.example.notingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notingapp.data.AppDatabase
import com.example.notingapp.fragments.HomeFragment
import com.example.notingapp.model.Tag
import com.example.notingapp.network.QuoteService
import com.example.notingapp.ui.ProfileActivity
import com.example.notingapp.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var quoteText: TextView
    lateinit var quoteAuthor: TextView
    lateinit var btnProfile: Button // Khai báo biến cho Profile Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quoteText = findViewById(R.id.quoteText)
        quoteAuthor = findViewById(R.id.quoteAuthor)
        btnProfile = findViewById(R.id.btnProfile) // Gán Profile Button

        insertDefaultTags()
        loadQuote()

        // 🔥 FIX: xin quyền notification
        requestNotificationPermission()

        // 🔥 BACKGROUND SYNC
        setupSyncWorker()

        // 🔥 HANDLE NOTIFICATION CLICK
        handleIntent(intent)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, HomeFragment())
            .commit()

        // Xử lý sự kiện click nút Profile
        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // 🔥 QUAN TRỌNG: khi app đang mở mà click notification
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    // 🔥 HANDLE DATA TỪ NOTIFICATION
    private fun handleIntent(intent: Intent?) {

        val noteId = intent?.getStringExtra("noteId")

        if (noteId != null) {
            println("🔥 Open note from notification: $noteId")

            // 👉 Sau này có thể:
            // - scroll tới note
            // - highlight note
        }
    }

    // 🔥 NEW FUNCTION (QUAN TRỌNG)
    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun setupSyncWorker() {

        val workRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun insertDefaultTags() {

        lifecycleScope.launch(Dispatchers.IO) {

            val dao = AppDatabase.getDatabase(this@MainActivity).tagDao()

            if (dao.count() == 0) {

                dao.insert(Tag(name = "Personal"))
                dao.insert(Tag(name = "Work"))
                dao.insert(Tag(name = "Study"))
                dao.insert(Tag(name = "Idea"))
            }
        }
    }

    private fun loadQuote() {

        lifecycleScope.launch {

            try {

                val quote = QuoteService.api.getRandomQuote()

                quoteText.text = quote.content
                quoteAuthor.text = "- ${quote.author}"

            } catch (_: Exception) {

                quoteText.text = "Stay focused and keep coding."
                quoteAuthor.text = ""
            }
        }
    }
}
