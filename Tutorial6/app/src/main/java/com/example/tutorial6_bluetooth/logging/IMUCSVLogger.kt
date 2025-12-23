package com.example.tutorial6_bluetooth.logging

import android.content.Context
import com.example.tutorial6_bluetooth.parser.IMU_Reading
import java.io.File
import java.io.FileOutputStream

class IMUCSVLogger {

    private var logFileOutputStream: FileOutputStream? = null
    private var filename: String? = null

    fun start(context: Context, prefix: String = "imu_data"): String {
        val fileName = LogUtils.generateFilename(prefix, "CSV")
        val file = File(context.getExternalFilesDir(null), fileName)

        try {
            logFileOutputStream = FileOutputStream(file, true)

            // Write CSV header
            val header = "timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ\n"
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

    fun stop() {
        try {
            logFileOutputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logFileOutputStream = null
        filename = null
    }

    fun getFilename(): String? = filename
}
