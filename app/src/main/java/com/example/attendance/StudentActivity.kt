// ============================================================
// STUDENT SIDE — Full Activity with UI + Permissions
// Simple screen: enter session code + student ID, tap Join,
// see a "Broadcasting..." status. Handles Android 12+ permissions.
// ============================================================
package com.example.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class StudentActivity : AppCompatActivity() {

    private lateinit var advertiser: AttendanceAdvertiser
    private lateinit var statusText: TextView
    private lateinit var codeInput: EditText
    private lateinit var idInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var joinButton: Button

    // Handles the one-time permission popup (Bluetooth advertise/connect)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            beginBroadcast()
        } else {
            statusText.text = "Bluetooth permission is required to check in."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        advertiser = AttendanceAdvertiser(this)
        statusText = findViewById(R.id.statusText)
        codeInput = findViewById(R.id.codeInput)
        idInput = findViewById(R.id.idInput)
        nameInput = findViewById(R.id.nameInput)
        joinButton = findViewById(R.id.joinButton)

        joinButton.setOnClickListener { onJoinTapped() }
    }

    private fun onJoinTapped() {
        val code = codeInput.text.toString().trim()
        val id = idInput.text.toString().trim()
        val name = nameInput.text.toString().trim()

        if (code.isEmpty() || id.isEmpty() || name.isEmpty()) {
            statusText.text = "Please fill in all fields."
            return
        }

        // Save for use after permission grant
        pendingCode = code
        pendingId = id

        if (hasBluetoothPermissions()) {
            beginBroadcast()
        } else {
            requestBluetoothPermissions()
        }
    }

    private var pendingCode: String = ""
    private var pendingId: String = ""

    private fun beginBroadcast() {
        advertiser.startAdvertising(pendingCode, pendingId)
        statusText.text = "✅ Broadcasting attendance — you can put your phone away.\nSession: $pendingCode"
        joinButton.isEnabled = false
        codeInput.isEnabled = false
        idInput.isEnabled = false
        nameInput.isEnabled = false
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // older Android versions grant Bluetooth at install time
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        advertiser.stopAdvertising()
    }
}
