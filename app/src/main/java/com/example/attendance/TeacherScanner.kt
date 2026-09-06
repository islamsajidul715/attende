// ============================================================
// TEACHER SIDE — BLE Scanner
// This runs on the teacher's phone. It continuously listens
// for broadcasts from students, checks the session code matches,
// and logs each unique student ID as "present".
// ============================================================
package com.example.attendance

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import java.util.UUID

class AttendanceScanner(private val context: android.content.Context) {

    private val SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE)
                as android.bluetooth.BluetoothManager).adapter

    private var scanner: BluetoothLeScanner? = null

    // Tracks who's already been marked present, so we don't
    // double-count the same student's repeated broadcasts.
    // studentId -> (name/time, whatever you want to store)
    val presentStudents = linkedMapOf<String, Long>()  // id -> timestamp (millis)

    // Callback the UI can hook into to update a live list on screen
    var onStudentDetected: ((studentId: String) -> Unit)? = null

    private var activeSessionCode: String = ""

    fun startScanning(sessionCode: String) {
        activeSessionCode = sessionCode
        scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            android.util.Log.e("Attendance", "BLE scanning not supported on this device")
            return
        }

        // Filter to only our app's packets (by service UUID) —
        // ignores all the random BLE noise (headphones, smartwatches, etc.)
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // scan aggressively
            .build()

        scanner?.startScan(listOf(filter), settings, scanCallback)
        android.util.Log.i("Attendance", "Scanning started for session: $sessionCode")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val manufacturerData = result.scanRecord?.getManufacturerSpecificData(0x1234) ?: return
            val payload = String(manufacturerData, Charsets.UTF_8)

            // payload looks like "4F2A|STU2245"
            val parts = payload.split("|")
            if (parts.size != 2) return

            val (code, studentId) = parts

            // Reject if session code doesn't match today's code
            if (code != activeSessionCode) return

            // Already logged? skip (this is how we ignore the
            // hundreds of repeated broadcasts from the same student)
            if (presentStudents.containsKey(studentId)) return

            presentStudents[studentId] = System.currentTimeMillis()
            android.util.Log.i("Attendance", "Marked present: $studentId")

            // Notify UI (e.g. update a RecyclerView list)
            onStudentDetected?.invoke(studentId)
        }

        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("Attendance", "Scan failed, error code: $errorCode")
        }
    }

    fun stopScanning() {
        scanner?.stopScan(scanCallback)
    }
}

// ------------------------------------------------------------
// USAGE (in your teacher-facing Activity):
//
//   val scanner = AttendanceScanner(context)
//   scanner.onStudentDetected = { studentId ->
//       runOnUiThread {
//           // add studentId to your dashboard list/RecyclerView
//       }
//   }
//   scanner.startScanning(sessionCode = "4F2A")
//
//   // after ~5 minutes, or teacher taps "Close Attendance":
//   scanner.stopScanning()
//   // scanner.presentStudents now holds everyone who checked in
// ------------------------------------------------------------
