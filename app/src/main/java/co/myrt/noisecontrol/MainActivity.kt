package co.myrt.noisecontrol

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.myrt.noisecontrol.ui.theme.NoiseControlTheme
import co.myrt.noisecontrol.R

val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 1

class MainActivity : ComponentActivity() {
    private lateinit var settings: Settings
    private lateinit var device: Device
    private val requestBluetoothConnectPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Разрешение предоставлено, можно использовать Bluetooth
        } else {
            // Разрешение не предоставлено, вы не можете использовать Bluetooth
        }
    }

    public fun setLevel(level: String) {
        val intent = Intent(this, SendService::class.java)
        intent.putExtra("level", level)
        SendService.enqueueWork(applicationContext, intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings.getInstance(applicationContext)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothConnectPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        // Get bonded devices
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        try {
            val bondedDevices = bluetoothManager.adapter?.bondedDevices.orEmpty()
            bondedDevices.forEach { bluetoothDevice ->
                // Assert bose vendor code
                if (bluetoothDevice.address.startsWith("C8:7B:23") && settings.devices[bluetoothDevice.address] == null) {
                    // TODO: add support for other models
                    settings.addDevice(DeviceType.NC700, bluetoothDevice.address, bluetoothDevice.name.orEmpty())
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(applicationContext,"Problem adding device -- check permission given: Bluetooth Connect", Toast.LENGTH_SHORT).show()
        }
        enableEdgeToEdge()
        setContent {
            NoiseControlTheme {
                if (settings.findConnectedDevices().isEmpty()) DeviceNotFoundScreen() else NoiseControlScreen(this)
            }
        }
    }
}

@Composable
fun DeviceNotFoundScreen() {
    val darkerSurface = lerp(
        MaterialTheme.colorScheme.surface,
        Color.Black,
        fraction = 0.05f
    )
    Surface(
        color = darkerSurface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column {
            Text(
                text = "Noise Control",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 50.dp, top = 160.dp, start = 24.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Text(
                    text = "Наушники не найдены",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text(
                    text = "Подключите поддерживаемые наушники, чтобы управлять режимами шумопадвления..",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Default,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.size(60.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_information),
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Приложение поддерживает следующие наушники:\n" +
                            "Bose NC 700\n" +
                            "Bose QC 35.",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Default,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
fun NoiseControlScreen(activity: MainActivity) {
    var selectedMode by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val ncOnSelected = {
        selectedMode = 0
        activity.setLevel("10")
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val ncOffSelected = {
        selectedMode = 1
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        activity.setLevel("off")
    }
    val transparencySelected = {
        selectedMode = 2
        activity.setLevel("0")
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val darkerSurface = lerp(
        MaterialTheme.colorScheme.surface,
        Color.Black,
        fraction = 0.05f
    )

    Surface(
        color = darkerSurface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column {
            Text(
                text = "Noise Control",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 24.dp, top = 160.dp, start = 24.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_headphones), // Replace with your image resource
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(16.dp)
                )
                Text(
                    text = "Bose NC 700 HP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Батарея 40%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                ModeSelector(selectedMode, ncOnSelected, ncOffSelected, transparencySelected)

                Spacer(modifier = Modifier.height(40.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_information),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Функции управления звуком могут улучшить восприятие звука, но сократить время автономной работы.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelector(
    selectedMode: Int,
    ncOnSelected: () -> Unit,
    ncOffSelected: () -> Unit,
    transparencySelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f, false)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModeSelectButton(
                selected = selectedMode == 0,
                onClick = { ncOnSelected() },
                icon = R.drawable.ic_nc_on,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    bottomStart = 16.dp,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                )
            )
            ModeSelectLabel("Шумоподавление")
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModeSelectButton(
                selected = selectedMode == 1,
                onClick = { ncOffSelected() },
                icon = R.drawable.ic_nc_off,
                shape = RoundedCornerShape(4.dp)
            )
            ModeSelectLabel("Отключено")
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModeSelectButton(
                selected = selectedMode == 2,
                onClick = { transparencySelected() },
                icon = R.drawable.ic_nc_transparency,
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    bottomStart = 4.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp
                )
            )
            ModeSelectLabel("Прозрачность")
        }
    }
}

@Composable
fun ModeSelectButton(selected: Boolean, onClick: () -> Unit, icon: Int, shape: Shape) {
    Button(
        onClick = { onClick() },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = shape,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .width(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun ModeSelectLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontFamily = FontFamily.Default,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

//
//@Preview(showBackground = true)
//@Composable
//fun PreviewNoiseControlScreen() {
//    NoiseControlTheme {
//        NoiseControlScreen()
//    }
//}