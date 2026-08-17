package com.ozyern.exhale.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozyern.exhale.LocalPlayerConnection
import com.ozyern.exhale.R
import com.ozyern.exhale.ui.component.LiquidGlassSheet
import com.ozyern.exhale.ui.component.liquid.LiquidSlider

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun V8DeviceSelector(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeviceSheet by remember { mutableStateOf(false) }

    val availableDevices = remember {
        getAvailableDevices(context)
    }

    val activeDevice = remember(availableDevices) {
        getActiveDevice(context, availableDevices)
    }

    val deviceIcon = when {
        activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                activeDevice?.type == AudioDeviceInfo.TYPE_BLE_HEADSET -> R.drawable.bluetooth

        else -> R.drawable.airplay
    }

    val isBluetooth = activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            activeDevice?.type == AudioDeviceInfo.TYPE_BLE_HEADSET

    // AirPlay-style trigger button
    Surface(
        onClick = { showDeviceSheet = true },
        shape = CircleShape,
        color = if (isBluetooth) textBackgroundColor.copy(alpha = 0.15f) else Color.Transparent,
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(deviceIcon),
                contentDescription = "AirPlay",
                tint = textBackgroundColor.copy(alpha = if (isBluetooth) 1f else 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = availableDevices,
            activeDevice = activeDevice,
            onDeviceSelected = { showDeviceSheet = false },
        )
    }
}

/**
 * The output picker, rebuilt on the app's own frosted sheet.
 *
 * It used to be a flat Material `Dialog` + `Surface` — an opaque slab with a title, a list of
 * rows and a "Close" button, which was the only sheet in the app that did not look like the app.
 * The structure now follows the reference the user gave (Spotify's Connect panel): the device you
 * are *on* is a hero card with its output quality, the track it is carrying and its own volume
 * control, and the devices you are *not* on are a quiet list underneath.
 *
 * One honest limitation, stated in the UI rather than hidden: Android gives an ordinary app no way
 * to route audio to an arbitrary output. Selecting a row cannot move the stream — that switch
 * belongs to the system. So the other-device rows open the system output picker instead of
 * pretending to have done something, and the "Bluetooth settings" tile is the real escape hatch.
 */
@Composable
fun DeviceSelectionBottomSheet(
    onDismiss: () -> Unit,
    availableDevices: List<AudioDeviceInfo>,
    activeDevice: AudioDeviceInfo?,
    onDeviceSelected: (AudioDeviceInfo) -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    LiquidGlassSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.device_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                color = onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            // ── Current output: the hero card ──────────────────────────────
            ActiveDeviceCard(
                device = activeDevice,
                accent = accent,
                playerConnection = playerConnection,
            )

            val others = availableDevices.filter { it != activeDevice }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.device_sheet_other_outputs),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )

                others.forEach { device ->
                    DeviceRow(
                        device = device,
                        onClick = {
                            openSystemOutputPicker(context)
                            onDeviceSelected(device)
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Action tile ────────────────────────────────────────────────
            GlassActionTile(
                iconRes = R.drawable.bluetooth,
                label = stringResource(R.string.device_sheet_bluetooth_settings),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    onDismiss()
                },
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.device_sheet_routing_note),
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ActiveDeviceCard(
    device: AudioDeviceInfo?,
    accent: Color,
    playerConnection: com.ozyern.exhale.playback.PlayerConnection?,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val metadata by (playerConnection?.mediaMetadata
        ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val format by (playerConnection?.currentFormat
        ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.07f),
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f),
                    )
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device?.let { deviceLabel(it) }
                            ?: stringResource(R.string.device_speaker),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    // Quality chip, driven by the format actually playing rather than by the
                    // user's *preference* — a "Max" label on a 128 kbps stream would be a lie.
                    val bitrate = format?.bitrate
                    if (bitrate != null && bitrate > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${bitrate / 1000} kbps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(onSurface.copy(alpha = 0.10f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.equalizer),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = metadata?.let { meta ->
                            val artists = meta.artists.joinToString { it.name }
                            if (artists.isBlank()) meta.title else "${meta.title} — $artists"
                        } ?: stringResource(R.string.device_sheet_nothing_playing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(device?.let { deviceIcon(it) } ?: R.drawable.airplay),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // ── Volume ─────────────────────────────────────────────────────────
        if (playerConnection != null) {
            Spacer(Modifier.height(18.dp))
            val volume by playerConnection.service.playerVolume.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.volume_off),
                    contentDescription = null,
                    tint = onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(12.dp))
                LiquidSlider(
                    value = volume,
                    onValueChange = { playerConnection.service.playerVolume.value = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = null,
                    tint = onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: AudioDeviceInfo,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "deviceRowPress",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 6.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(deviceIcon(device)),
            contentDescription = null,
            tint = onSurface.copy(alpha = 0.72f),
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = deviceLabel(device),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = onSurface.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.chevron_right),
            contentDescription = null,
            tint = onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun GlassActionTile(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "actionTilePress",
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(20.dp))
            .background(onSurface.copy(alpha = 0.08f))
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 15.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = onSurface.copy(alpha = 0.9f),
        )
    }
}

/** Opens the platform's own output switcher, which is the only thing that can actually reroute. */
private fun openSystemOutputPicker(context: Context) {
    val intents = listOf(
        Intent("com.android.settings.panel.action.MEDIA_OUTPUT"),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
    )
    for (intent in intents) {
        val launched = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (launched) return
    }
}

@Composable
private fun deviceLabel(device: AudioDeviceInfo): String {
    val product = device.productName?.toString()?.takeIf { it.isNotBlank() }
    return product ?: when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> stringResource(R.string.device_speaker)
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> stringResource(R.string.device_wired_headphones)
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> stringResource(R.string.device_wired_headset)
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> stringResource(R.string.device_bluetooth)
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> stringResource(R.string.device_bluetooth_sco)
        AudioDeviceInfo.TYPE_BLE_HEADSET -> stringResource(R.string.device_ble_headset)
        else -> stringResource(R.string.device_generic)
    }
}

private fun deviceIcon(device: AudioDeviceInfo): Int = when (device.type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET -> R.drawable.bluetooth

    else -> R.drawable.airplay
}

// Returns the available audio output devices
private fun getAvailableDevices(context: Context): List<AudioDeviceInfo> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { device ->
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        .sortedBy { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET -> 0

                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> 1

                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 2
                else -> 3
            }
        }
}

// Returns the currently active output device
private fun getActiveDevice(context: Context, devices: List<AudioDeviceInfo>): AudioDeviceInfo? {
    // Priority: Bluetooth > Wired > Speaker
    val bluetoothDevices = devices.filter {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    if (bluetoothDevices.isNotEmpty()) {
        return bluetoothDevices.firstOrNull()
    }

    val wiredDevices = devices.filter {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
    }

    if (wiredDevices.isNotEmpty()) {
        return wiredDevices.firstOrNull()
    }

    return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}
