package com.epfl.esl.musicplayer.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

class WristFlickGyroDetector(
    private val onFlick: (Direction) -> Unit,
    private val magnitudeThreshold: Float = 4.0f,   // rad/s
    private val axisThreshold: Float = 5.0f,        // rad/s
    private val cooldownMs: Long = 700L,            // block repeated flicks
    private val windowMs: Long = 120L               // "movement burst" max duration to average
) : SensorEventListener {

    enum class Direction { LEFT, RIGHT, UNKNOWN }

    private var lastTriggerTime = 0L

    // Window accumulation state (one "averaging movement")
    private var windowStartMs = 0L
    private var sumWx = 0f
    private var sumWy = 0f
    private var sumWz = 0f
    private var samples = 0
    private var windowActive = false

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val wx = event.values[0] // Circular speed in x-axis
        val wy = event.values[1] // Circular speed in y-axis
        val wz = event.values[2] // Circular speed in z-axis
        val magnitude = sqrt(wx * wx + wy * wy + wz * wz)

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < cooldownMs) return

        // Start a window when we first see a "strong enough" motion
        if (!windowActive) {
            if (magnitude >= magnitudeThreshold) {
                windowActive = true
                windowStartMs = now
                sumWx = wx
                sumWy = wy
                sumWz = wz
                samples = 1
            }
            return
        }

        // Window is active: keep accumulating as long as we're within the time window
        // and the signal is still "in the movement".
        val elapsed = now - windowStartMs
        val stillMoving = magnitude >= (magnitudeThreshold * 0.6f)

        if (elapsed <= windowMs && stillMoving) {
            sumWx += wx
            sumWy += wy
            sumWz += wz
            samples += 1
            return
        }

        // Window ended: decide using the averaged angular velocity of the burst
        val avgWx = sumWx / samples
        val avgWy = sumWy / samples
        val avgWz = sumWz / samples

        val direction = when {
            avgWx >= axisThreshold -> Direction.RIGHT
            avgWx <= -axisThreshold -> Direction.LEFT
            else -> Direction.UNKNOWN
        }

        // Reset window state
        windowActive = false
        samples = 0

        if (direction != Direction.UNKNOWN) {
            lastTriggerTime = now
            onFlick(direction)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}