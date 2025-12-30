package encaps.rc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import encaps.rc.ui.theme.RcTheme
import java.util.UUID
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build

import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    var bluetoothGatt: BluetoothGatt? = null
    private var logText = mutableStateOf("Log started...\n")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        setContent {
            RcTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // PASS THE FUNCTION HERE
                    ControlPanel(
                        modifier = Modifier.padding(innerPadding),
                        receivedLog = logText.value,
                        onConnectRequested = { address ->
                            connectToDevice(address, this, bluetoothAdapter)
                        },
                        onSendRequested = { command ->
                            sendData(command)
                        }
                    )
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Connected!")
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // IMPORTANT: You must discover services before you can get bytes
                    gatt.discoverServices()
                }
            }
        }

        // ADD THIS: This runs after gatt.discoverServices() finishes
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "Services discovered!")

                // 1. Define the IDs (Replace these if your device uses different ones)
                val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
                val CHAR_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

                // 2. Find the Service and Characteristic
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHAR_UUID)

                if (characteristic != null) {
                    // 3. Enable notifications locally in the Android app
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        gatt.setCharacteristicNotification(characteristic, true)

                        // 4. Tell the REMOTE DEVICE to start sending data (Update the Descriptor)
                        // This is like flipping a switch on the hardware side
                        val descriptor =
                            characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                            Log.i("BLE", "Notification subscription successful!")
                        }
                    }
                } else {
                    Log.e("BLE", "Characteristic not found!")
                }
            }
        }

        // ADD THIS: This is where the bytes arrive from the device
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val dataString = value.decodeToString()
            // Use runOnUiThread to ensure UI state and services are handled correctly
            runOnUiThread {
                logText.value += "R<: $dataString\n"

                // We iterate through the byte array to see if any byte is 33
//            if (value.contains(33.toByte())) {
                if (dataString.contains("!"))
                {
                    vibratePhone()
                }
            }
        }

        private fun vibratePhone() {
            // Explicitly use the MainActivity context (this@MainActivity)
            val vibrator = this@MainActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (vibrator.hasVibrator()) { // Check if hardware actually exists
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } else {
                Log.e("Vibration", "Device does not have a vibrator")
            }
        }
    }

    private fun connectToDevice(address: String, context: Context, adapter: BluetoothAdapter?) {
        val device = adapter?.getRemoteDevice(address)
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt = device?.connectGatt(context, false, gattCallback)
        }
    }

    private fun sendData(data: String) {
        val gatt = bluetoothGatt ?: return
        val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val CHAR_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        val service = gatt.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_UUID)

        if (characteristic != null) {
            val bytes = data.toByteArray()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
                == PackageManager.PERMISSION_GRANTED
            ) {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    // Modern Way (API 33+)
                    gatt.writeCharacteristic(
                        characteristic,
                        bytes,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                } else {
                    // Legacy Way (Older phones)
                    characteristic.value = bytes
                    gatt.writeCharacteristic(characteristic)
                }
                Log.i("BLE", "Sent: $data")
                logText.value += "W>: $data\n"
            }
        }
    }

}

@Composable
fun ControlPanel(
    modifier: Modifier = Modifier,
    receivedLog: String,
    onConnectRequested: (String) -> Unit, // This is the "bridge" to MainActivity
    onSendRequested: (String) -> Unit
) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val scrollState = rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(receivedLog) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // 1. Local state for the slider position
    var sliderPosition by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (bluetoothAdapter == null) {
                Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            } else if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            } else {
                // CALL THE BRIDGE FUNCTION
                onConnectRequested("78:DB:2F:CD:F3:8A")
            }
        }) {
            Text("Connect")
        }

        Text(text = "Remote Control 1.0.3", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = { onSendRequested("Df") }) {
            Text("F")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
// 2. Nest a Row for B, L, and R
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onSendRequested("Dl") }) { Text("L") }
            Button(onClick = { onSendRequested("Db") }) { Text("B") }
            Button(onClick = { onSendRequested("Dr") }) { Text("R") }
        }


        // 3. The "TextArea" (Scrollable Log)
        Text(
            text = "Device Log:",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 32.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray.copy(alpha = 0.2f))
//                .verticalScroll(rememberScrollState()) // Makes it scrollable
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(text = receivedLog, color = Color.DarkGray)
        }

        // --- NEW: Slider Section ---
        Text(
            text = "Power: ${sliderPosition.roundToInt()*10}%",
            modifier = Modifier.padding(top = 24.dp)
        )
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..10f,
            onValueChangeFinished = {
                // Send the value over BLE when the user stops sliding
//                onSendRequested("S${sliderPosition.roundToInt()}")
                val intValue = sliderPosition.roundToInt()

                // 1. Convert integer to char (0 -> 'a', 1 -> 'b', etc.)
                val mappedChar = ('a' + intValue)

                // 2. Create the command string "S" + the single char
                val command = "S$mappedChar"

                onSendRequested(command)
            },
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}