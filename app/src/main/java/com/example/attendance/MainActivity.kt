package com.example.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Entry point of the app. Just asks "Teacher or Student?"
 * and routes to the right screen. No BLE logic lives here.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.teacherButton).setOnClickListener {
            startActivity(Intent(this, TeacherActivity::class.java))
        }
        findViewById<Button>(R.id.studentButton).setOnClickListener {
            startActivity(Intent(this, StudentActivity::class.java))
        }
    }
}
