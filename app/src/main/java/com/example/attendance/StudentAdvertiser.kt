// ============================================================
// STUDENT SIDE — BLE Advertiser
// This runs on each student's phone. It broadcasts a tiny
// packet on a loop: "sessionCode|studentID". No connection,
// no pairing — just repeating radio broadcasts.
// ============================================================
package com.example.attendance

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import java.util.UUID

class AttendanceAdvertiser(private val context: android.content.Context) {

    // A fixed UUID that identifies "this is an attendance app packet"
    // so the teacher's scanner can filter out noise from other BLE devices
    private val SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE)
                as android.bluetooth.BluetoothManager).adapter

    private var advertiser: BluetoothLeAdvertiser? = null

    /**
     * Starts broadcasting. Call this once when the student
     * taps "Join Attendance" — it keeps running in the background
     * until stopAdvertising() is called (or app is closed).
     */
    fun startAdvertising(sessionCode: String, studentId: String) {
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run {
            android.util.Log.e("Attendance", "BLE advertising not supported on this device")
            return
        }

        // Build the payload: "sessionCode|studentID" as raw bytes
        val payload = "$sessionCode|$studentId".toByteArray(Charsets.UTF_8)
        // Note: BLE advertising payload is limited to ~23-27 usable bytes
        // for manufacturer data after UUID + settings overhead, so keep
        // session codes and IDs short (e.g. 4-6 chars each).

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // broadcast fast & often
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)     // stronger signal, better range
            .setConnectable(false)  // IMPORTANT: no connection, just broadcast
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addManufacturerData(0x1234, payload)  // 0x1234 = arbitrary app ID
            .setIncludeDeviceName(false)  // keep packet small
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                android.util.Log.i("Attendance", "Broadcasting started: $sessionCode|$studentId")
            }
            override fun onStartFailure(errorCode: Int) {
                android.util.Log.e("Attendance", "Broadcast failed, error code: $errorCode")
            }
        }

        advertiser?.startAdvertising(settings, data, callback)
    }

    fun stopAdvertising() {
        advertiser?.stopAdvertising(object : AdvertiseCallback() {})
    }
}

// ------------------------------------------------------------
// USAGE (in your Activity/Fragment):
//
//   val advertiser = AttendanceAdvertiser(context)
//   advertiser.startAdvertising(sessionCode = "4F2A", studentId = "STU2245")
//
//   // when class ends or teacher closes attendance:
//   advertiser.stopAdvertising()
// ------------------------------------------------------------
