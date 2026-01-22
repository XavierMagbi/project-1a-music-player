package com.epfl.esl.musicplayer.presentation
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt
class WristFlickGyroDetector(
    private val onFlick: (Direction) -> Unit,
    private val magnitudeThreshold: Float = 4.0f, // rad/s (tune)
    private val axisThreshold: Float = 5f,      // rad/s (tune)
    private val cooldownMs: Long = 2000L           // prevents multiple triggers
) : SensorEventListener {
    //add averaging later
    enum class Direction { LEFT, RIGHT,UP,DOWN, UNKNOWN }
    private var lastTriggerTime = 0L
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val wx = event.values[0]
        val wy = event.values[1]
        val wz = event.values[2]
        val magnitude = sqrt(wx * wx + wy * wy + wz * wz)
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < cooldownMs) return
        if (magnitude >= magnitudeThreshold) {
            // Optional: infer direction from one axis (device-dependent!)
            // We use wx to Play/Pause and wz to switch music
            val direction = when {
                wx >= axisThreshold -> Direction.RIGHT
                wx <= -axisThreshold -> Direction.LEFT
                wy >= axisThreshold -> Direction.UP
                wy <= -axisThreshold -> Direction.DOWN
                else -> Direction.UNKNOWN
            }
            if (direction != Direction.UNKNOWN){
                lastTriggerTime = now
            }
            onFlick(direction)
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}