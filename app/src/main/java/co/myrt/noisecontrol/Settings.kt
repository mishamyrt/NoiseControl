package co.myrt.noisecontrol

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.MutableLiveData
import java.lang.IllegalArgumentException

enum class DeviceType(val label: String, val fullName: String) {
    NC700("700", "Bose NC 700"),
    QC35("qc35", "Bose QC 35");

    override fun toString(): String {
        return this.label
    }

    companion object {
        fun fromString(string: String): DeviceType {
            return when(string) {
                NC700.label -> NC700
                QC35.label -> QC35
                else -> NC700
            }
        }
    }
}

data class Device(val type: DeviceType, val address: String, val name: String) {
    override fun toString(): String {
        return listOf(type.toString(), address, name).joinToString(separator = "\t")
    }

    companion object {
        fun fromString(input: String): Device {
            val parts = input.split("\t", ignoreCase = false, limit = 3)
            if (parts.count() < 3) throw IllegalArgumentException("Device string not well formed")
            val deviceType = DeviceType.fromString(parts[0])
            val address = parts[1]
            val name = parts[2]
            return Device(deviceType, address, name)
        }
    }
}

class Settings(private val applicationContext: Context) {

    private var sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private var sharedPreferencesListener: OnSharedPreferenceChangeListener? = null

    var devicesData = MutableLiveData<List<Device>>()
    var devices = mutableMapOf<String, Device>()

    // Determine that the disclaimer must have been agreed to if a device has been added
    fun agreedDisclaimer(): Boolean {
        return devices.isNotEmpty()
    }

    fun addDevice(deviceType: DeviceType, address: String, name: String) {
        val device = Device(deviceType, address, name)
        devices[device.address] = device
        savePreferences()
    }

    fun removeDevice(address: String) {
        devices.remove(address)
        savePreferences()
    }

    private fun savePreferences() {
        val deviceStrings = devices.map { it.value.toString() }.toSet()

        val editor = sharedPreferences.edit()
        editor.putStringSet("devices", deviceStrings)
        editor.apply()
    }

    private fun preferencesUpdated() {
        // Resetting list from scratch
        devices.clear()

        // Add all devices from preferences
        val deviceStrings = sharedPreferences.getStringSet("devices", setOf<String>()).orEmpty()
        for (deviceString in deviceStrings) {
            try {
                val device = Device.fromString(deviceString)
                devices[device.address] = device
            } catch(e: Throwable) {
                // Problem parsing device string, ignore entry
                println("ERROR: Problem parsing device entry: $deviceString")
                continue
            }
        }

        // Set live data
        devicesData.postValue(devices.values.toList())
    }

    fun findConnectedDevices(): Set<Device> {
        val connected = mutableSetOf<Device>()
        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        // Ignore everything if Bluetooth is not enabled
        if (bluetoothAdapter?.isEnabled == true) {
            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val audioDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (devInfo in audioDeviceInfo) {
                if (devices.containsKey(devInfo.address)) {
                    connected.add(devices[devInfo.address]!!)
                }
            }
        }
        return connected
    }

    // Singleton
    companion object {
        private var singleInstance: Settings? = null
        fun getInstance(applicationContext: Context): Settings {
            synchronized(this) {
                if (singleInstance == null) {
                    singleInstance = Settings(applicationContext)
                }
                return singleInstance!!
            }
        }
    }


    init {
        preferencesUpdated()
        sharedPreferencesListener = OnSharedPreferenceChangeListener { _, _ ->
            preferencesUpdated()
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

}