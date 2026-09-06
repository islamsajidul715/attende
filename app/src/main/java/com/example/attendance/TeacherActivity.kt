// ============================================================
// TEACHER SIDE — Full Activity with UI + Permissions
// Shows session code, a live-updating list of present students,
// a running count, and Start/Stop controls.
// ============================================================
package com.example.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.random.Random

class TeacherActivity : AppCompatActivity() {

    private lateinit var scanner: AttendanceScanner
    private lateinit var sessionCodeText: TextView
    private lateinit var countText: TextView
    private lateinit var studentListView: ListView
    private lateinit var startStopButton: Button

    private val displayList = mutableListOf<String>()  // "STU2245 — 10:03:12"
    private lateinit var listAdapter: ArrayAdapter<String>

    private var sessionCode: String = ""
    private var isScanning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            beginScan()
        } else {
            countText.text = "Bluetooth permission is required to take attendance."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        scanner = AttendanceScanner(this)
        sessionCodeText = findViewById(R.id.sessionCodeText)
        countText = findViewById(R.id.countText)
        studentListView = findViewById(R.id.studentListView)
        startStopButton = findViewById(R.id.startStopButton)

        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        studentListView.adapter = listAdapter

        // Generate a fresh 4-character session code each time the
        // screen opens (e.g. "4F2A") — write it on the board / project it
        sessionCode = generateSessionCode()
        sessionCodeText.text = "Session Code: $sessionCode"

        startStopButton.setOnClickListener {
            if (!isScanning) {
                if (hasBluetoothPermissions()) beginScan() else requestBluetoothPermissions()
            } else {
                stopScan()
            }
        }

        scanner.onStudentDetected = { studentId ->
            runOnUiThread {
                val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                displayList.add(0, "$studentId  —  $time")  // newest on top
                listAdapter.notifyDataSetChanged()
                countText.text = "${displayList.size} students checked in"
            }
        }
    }

    private fun beginScan() {
        scanner.startScanning(sessionCode)
        isScanning = true
        startStopButton.text = "Stop Attendance"
        countText.text = "0 students checked in"
    }

    private fun stopScan() {
        scanner.stopScanning()
        isScanning = false
        startStopButton.text = "Attendance Closed"
        startStopButton.isEnabled = false
        countText.text = "${displayList.size} students checked in — Final"
    }

    private fun generateSessionCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no confusing 0/O/1/I
        return (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) scanner.stopScanning()
    }
}
