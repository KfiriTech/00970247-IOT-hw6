package com.example.tutorial6_bluetooth.parser

data class IMU_Reading(
    val timestamp: Float,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)

object IMUDataParser {
    fun parse(line: String, timestamp: Float): IMU_Reading? {
        val values = line.split(",").map { it.trim().toFloatOrNull() }
        if (values.any { it == null }) return null

        // Handle both formats:
        // Format 1 (7 values): timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ
        // Format 2 (6 values): accX,accY,accZ,gyroX,gyroY,gyroZ (use calculated timestamp)
        return when (values.size) {
            7 -> IMU_Reading(
                timestamp = values[0]!!,  // Use timestamp from data
                accX = values[1]!!,
                accY = values[2]!!,
                accZ = values[3]!!,
                gyroX = values[4]!!,
                gyroY = values[5]!!,
                gyroZ = values[6]!!
            )
            6 -> IMU_Reading(
                timestamp = timestamp,  // Use calculated timestamp
                accX = values[0]!!,
                accY = values[1]!!,
                accZ = values[2]!!,
                gyroX = values[3]!!,
                gyroY = values[4]!!,
                gyroZ = values[5]!!
            )
            else -> null
        }
    }
}
