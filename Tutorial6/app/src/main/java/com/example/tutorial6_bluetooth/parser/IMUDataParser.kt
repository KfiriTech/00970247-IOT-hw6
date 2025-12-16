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
        if (values.size == 6) {
            return IMU_Reading(
                timestamp = timestamp,
                accX = values[0]!!,
                accY = values[1]!!,
                accZ = values[2]!!,
                gyroX = values[3]!!,
                gyroY = values[4]!!,
                gyroZ = values[5]!!
            )
        }
        return null
    }
}
