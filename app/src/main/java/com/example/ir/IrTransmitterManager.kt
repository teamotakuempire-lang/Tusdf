package com.example.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.data.model.AirconState
import com.example.data.model.TransmitterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.concurrent.TimeUnit

data class TransmitResult(
    val success: Boolean,
    val hexCode: String,
    val modeUsed: TransmitterMode,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class IrTransmitterManager(private val context: Context) {
    private val TAG = "IrTransmitterManager"
    
    private val consumerIrManager: ConsumerIrManager? by lazy {
        try {
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Exception) {
            null
        }
    }

    private val vibrator: Vibrator? by lazy {
        try {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (e: Exception) {
            null
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun hasHardwareIrEmitter(): Boolean {
        return try {
            consumerIrManager?.hasIrEmitter() == true
        } catch (e: Exception) {
            false
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (e: Exception) {
            // Haptic fail-safe
        }
    }

    suspend fun transmit(
        state: AirconState,
        commandName: String = "STATE_UPDATE",
        isSensorFlushSequence: Boolean = false
    ): TransmitResult = withContext(Dispatchers.IO) {
        val packet = Glp09PortIrProtocol.buildPacket(state, isSensorFlushSequence)
        val hex = Glp09PortIrProtocol.toHexString(packet)
        val pattern = Glp09PortIrProtocol.packetToMicrosecondPattern(packet)

        triggerHapticFeedback()

        val activeMode = state.transmitterMode

        when (activeMode) {
            TransmitterMode.BUILTIN_IR -> {
                val hasEmitter = hasHardwareIrEmitter()
                if (hasEmitter && consumerIrManager != null) {
                    try {
                        consumerIrManager?.transmit(Glp09PortIrProtocol.CARRIER_FREQUENCY_HZ, pattern)
                        Log.d(TAG, "Hardware IR Transmitted: $hex ($commandName)")
                        TransmitResult(
                            success = true,
                            hexCode = hex,
                            modeUsed = TransmitterMode.BUILTIN_IR,
                            message = "Hardware IR 38kHz burst sent: $commandName"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Hardware IR transmit failed", e)
                        TransmitResult(
                            success = false,
                            hexCode = hex,
                            modeUsed = TransmitterMode.BUILTIN_IR,
                            message = "IR transmitter error: ${e.localizedMessage ?: "Unknown hardware fault"}"
                        )
                    }
                } else {
                    // Fallback to simulation mode if device lacks IR diode
                    TransmitResult(
                        success = true,
                        hexCode = hex,
                        modeUsed = TransmitterMode.SIMULATION,
                        message = "Hardware IR diode absent on device; simulated 38kHz pattern ($hex)"
                    )
                }
            }

            TransmitterMode.WIFI_GATEWAY -> {
                val payload = Glp09PortIrProtocol.toWifiGatewayPayload(state, isSensorFlushSequence)
                try {
                    val url = state.wifiGatewayUrl.trim()
                    if (url.isEmpty()) {
                        return@withContext TransmitResult(
                            success = false,
                            hexCode = hex,
                            modeUsed = TransmitterMode.WIFI_GATEWAY,
                            message = "Gateway URL is empty. Configure IP in settings."
                        )
                    }

                    val request = if (url.contains("?cmnd=")) {
                        // Tasmota style GET
                        val encodedCmd = "IRSend $payload"
                        val fullUrl = url + java.net.URLEncoder.encode(encodedCmd, "UTF-8")
                        Request.Builder().url(fullUrl).build()
                    } else {
                        // REST POST
                        val body = payload.toRequestBody("application/json".toMediaTypeOrNull())
                        Request.Builder().url(url).post(body).build()
                    }

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""
                    response.close()

                    TransmitResult(
                        success = response.isSuccessful,
                        hexCode = hex,
                        modeUsed = TransmitterMode.WIFI_GATEWAY,
                        message = if (response.isSuccessful) "Sent to Wi-Fi Gateway ($url)" else "Gateway HTTP ${response.code}: $responseBody"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Wi-Fi Gateway transmit failed", e)
                    TransmitResult(
                        success = false,
                        hexCode = hex,
                        modeUsed = TransmitterMode.WIFI_GATEWAY,
                        message = "Gateway unreachable: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }

            TransmitterMode.SIMULATION -> {
                TransmitResult(
                    success = true,
                    hexCode = hex,
                    modeUsed = TransmitterMode.SIMULATION,
                    message = "Simulated IR blast: $commandName ($hex)"
                )
            }
        }
    }
}
