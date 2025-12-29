package com.example.tutorial6_bluetooth.algorithm

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

/**
 * Kotlin wrapper for Python-based step counter algorithm.
 * Uses Chaquopy to interface with Python implementation.
 */
class StepCounter(private val activityType: String = "Walking") {

    private var pythonCounter: PyObject? = null
    private val python: Python by lazy {
        if (!Python.isStarted()) {
            throw IllegalStateException("Python not initialized. Call StepCounter.initPython(context) first.")
        }
        Python.getInstance()
    }

    init {
        initializePython()
    }

    private fun initializePython() {
        try {
            val module = python.getModule("step_counter")
            pythonCounter = module.callAttr("init_counter", activityType)
        } catch (e: Exception) {
            android.util.Log.e("StepCounter", "Failed to initialize Python counter", e)
        }
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
        return try {
            val module = python.getModule("step_counter")
            val result = module.callAttr(
                "process_sample",
                pythonCounter,
                timestamp.toDouble(),
                accX.toDouble(),
                accY.toDouble(),
                accZ.toDouble()
            )
            result.toBoolean()
        } catch (e: Exception) {
            android.util.Log.e("StepCounter", "Error processing sample", e)
            false
        }
    }

    /**
     * Get current step count.
     *
     * @return Number of steps detected
     */
    fun getStepCount(): Int {
        return try {
            val module = python.getModule("step_counter")
            val result = module.callAttr("get_count", pythonCounter)
            result.toInt()
        } catch (e: Exception) {
            android.util.Log.e("StepCounter", "Error getting step count", e)
            0
        }
    }

    /**
     * Reset step counter to zero.
     */
    fun reset() {
        try {
            val module = python.getModule("step_counter")
            module.callAttr("reset_counter", pythonCounter)
        } catch (e: Exception) {
            android.util.Log.e("StepCounter", "Error resetting counter", e)
        }
    }

    companion object {
        /**
         * Initialize Python environment. Must be called once before using StepCounter.
         * Usually called from Application.onCreate() or Activity.onCreate().
         */
        fun initPython(context: Context) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
        }

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
            timestamps: FloatArray,
            accX: FloatArray,
            accY: FloatArray,
            accZ: FloatArray,
            activityType: String = "Walking"
        ): Int {
            return try {
                val python = Python.getInstance()
                val module = python.getModule("step_counter")

                // Convert FloatArrays to Python lists
                val timestampList = timestamps.map { it.toDouble() }
                val accXList = accX.map { it.toDouble() }
                val accYList = accY.map { it.toDouble() }
                val accZList = accZ.map { it.toDouble() }

                val result = module.callAttr(
                    "count_steps_batch",
                    timestampList,
                    accXList,
                    accYList,
                    accZList,
                    activityType
                )
                result.toInt()
            } catch (e: Exception) {
                android.util.Log.e("StepCounter", "Error counting steps from data", e)
                0
            }
        }

        /**
         * Count steps from lists (for easier Kotlin integration).
         *
         * @param timestamps List of timestamps in seconds
         * @param accX List of X-axis accelerations in m/s²
         * @param accY List of Y-axis accelerations in m/s²
         * @param accZ List of Z-axis accelerations in m/s²
         * @param activityType "Walking" or "Running"
         * @return Total number of steps detected
         */
        fun countStepsFromLists(
            timestamps: List<Float>,
            accX: List<Float>,
            accY: List<Float>,
            accZ: List<Float>,
            activityType: String = "Walking"
        ): Int {
            return countStepsFromData(
                timestamps.toFloatArray(),
                accX.toFloatArray(),
                accY.toFloatArray(),
                accZ.toFloatArray(),
                activityType
            )
        }
    }
}
