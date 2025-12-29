package com.example.tutorial6_bluetooth.algorithm

import kotlin.math.sqrt

/**
 * Pure Kotlin implementation of step counter algorithm.
 * No Python/Chaquopy dependencies required.
 * Uses peak detection on acceleration magnitude to count steps.
 */
class StepCounterKotlin(private val activityType: String = "Walking") {

    private var stepCount = 0
    private var lastStepTime = 0f

    // Activity-specific thresholds
    private val magnitudeThreshold = if (activityType == "Running") 13.0f else 11.5f
    private val minStepInterval = if (activityType == "Running") 0.25f else 0.35f

    // Peak detection state
    private var previousMagnitude = 0f
    private var wasAboveThreshold = false

    /**
     * Calculate acceleration magnitude from 3-axis data.
     */
    private fun calculateMagnitude(accX: Float, accY: Float, accZ: Float): Float {
        return sqrt(accX * accX + accY * accY + accZ * accZ)
    }

    /**
     * Process a single IMU sample and detect steps in real-time.
     *
     * @param timestamp Sample timestamp in seconds
     * @param accX Acceleration X-axis in m/s²
     * @param accY Acceleration Y-axis in m/s²
     * @param accZ Acceleration Z-axis in m/s²
     * @return true if a step was detected, false otherwise
     */
    fun addSample(timestamp: Float, accX: Float, accY: Float, accZ: Float): Boolean {
        val magnitude = calculateMagnitude(accX, accY, accZ)
        var stepDetected = false

        // Peak detection: looking for local maxima above threshold
        if (wasAboveThreshold && magnitude < previousMagnitude) {
            // We have a peak (was going up, now going down)
            val timeSinceLastStep = timestamp - lastStepTime

            // Check if peak meets criteria
            if (previousMagnitude >= magnitudeThreshold && timeSinceLastStep >= minStepInterval) {
                stepCount++
                lastStepTime = timestamp
                stepDetected = true
            }
        }

        // Update state for next iteration
        wasAboveThreshold = magnitude >= magnitudeThreshold
        previousMagnitude = magnitude

        return stepDetected
    }

    /**
     * Get current step count.
     */
    fun getStepCount(): Int = stepCount

    /**
     * Reset counter to zero.
     */
    fun reset() {
        stepCount = 0
        lastStepTime = 0f
        previousMagnitude = 0f
        wasAboveThreshold = false
    }

    companion object {
        /**
         * Count steps from a complete dataset (for CSV file processing).
         *
         * @param timestamps List of timestamps in seconds
         * @param accX List of X-axis accelerations in m/s²
         * @param accY List of Y-axis accelerations in m/s²
         * @param accZ List of Z-axis accelerations in m/s²
         * @param activityType "Walking" or "Running"
         * @return Total number of steps detected
         */
        fun countStepsFromData(
            timestamps: List<Float>,
            accX: List<Float>,
            accY: List<Float>,
            accZ: List<Float>,
            activityType: String = "Walking"
        ): Int {
            val counter = StepCounterKotlin(activityType)

            for (i in timestamps.indices) {
                counter.addSample(timestamps[i], accX[i], accY[i], accZ[i])
            }

            return counter.getStepCount()
        }

        /**
         * Count steps from arrays (for CSV file processing).
         */
        fun countStepsFromArrays(
            timestamps: FloatArray,
            accX: FloatArray,
            accY: FloatArray,
            accZ: FloatArray,
            activityType: String = "Walking"
        ): Int {
            return countStepsFromData(
                timestamps.toList(),
                accX.toList(),
                accY.toList(),
                accZ.toList(),
                activityType
            )
        }
    }
}
