package com.example.tutorial6_bluetooth.logging

import android.content.Context
import com.example.tutorial6_bluetooth.parser.IMU_Reading
import java.io.File
import java.io.FileOutputStream

data class CSVMetadata(
    val filename: String,
    val timestamp: String,
    val activityType: String,
    val stepCount: String = "PENDING"
)

class IMUCSVLogger {

    private var logFileOutputStream: FileOutputStream? = null
    private var filename: String? = null
    private var currentFile: File? = null

    fun start(context: Context, prefix: String = "imu_data", metadata: CSVMetadata): String {
        val fileName = LogUtils.generateFilename(prefix, "CSV")
        val file = File(context.getExternalFilesDir(null), fileName)
        currentFile = file

        try {
            logFileOutputStream = FileOutputStream(file, true)

            // Write CSV metadata header
            val header = buildString {
                appendLine("NAME,${metadata.filename},,,,")
                appendLine("EXPERIMENT_TIME,${metadata.timestamp},,,,,")
                appendLine("ACTIVITY_TYPE,${metadata.activityType},,,,")
                appendLine("COUNT_OF_ACTUAL_STEPS,${metadata.stepCount},,,,")
                appendLine(",,,,,,")
                appendLine("Time [sec],ACC_X,ACC_Y,ACC_Z,GYRO_X,GYRO_Y,GYRO_Z")
            }
            logFileOutputStream?.write(header.toByteArray())
            logFileOutputStream?.flush()

            filename = fileName
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

        return fileName
    }

    fun logIMUReading(reading: IMU_Reading) {
        try {
            val csvLine = "${reading.timestamp},${reading.accX},${reading.accY},${reading.accZ},${reading.gyroX},${reading.gyroY},${reading.gyroZ}\n"
            logFileOutputStream?.write(csvLine.toByteArray())
            logFileOutputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateStepCount(context: Context, filename: String, stepCount: String) {
        try {
            val file = File(context.getExternalFilesDir(null), filename)
            if (!file.exists()) return

            // Read all lines
            val lines = file.readLines()

            // Update the step count line (line index 3)
            if (lines.size > 3) {
                val updatedLines = lines.toMutableList()
                updatedLines[3] = "COUNT_OF_ACTUAL_STEPS,$stepCount,,,,"

                // Write back to file
                file.writeText(updatedLines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            logFileOutputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logFileOutputStream = null
        filename = null
        currentFile = null
    }

    fun getFilename(): String? = filename
}
