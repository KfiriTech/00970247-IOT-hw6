package com.example.tutorial6_bluetooth.csv

import android.content.Context
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.collections.mutableListOf


enum class ActivityType {
    Walking,
    Running
}

data class IMUReading(
    var timeSec: Float,
    var accX: Float,
    var accY: Float,
    var accZ: Float,
    var gyroX: Float,
    var gyroY: Float,
    var gyroZ: Float,
) {
    fun toArray(): Array<String> {
        return arrayOf(timeSec.toString(), accX.toString(), accY.toString(), accZ.toString(),
            gyroX.toString(), gyroY.toString(), gyroZ.toString())
    }
}

data class CsvData(
    var name: String,
    var experimentTime: String,
    var activityType: ActivityType,
    var stepsCount: Int,
    val rows: MutableList<IMUReading>
) {
    val size: Int
        get() = rows.size
}


fun readCsv(context: Context): CsvData {
    val f = csvFile(context)
    var name = "Unavailable"
    var experimentTime = "N/A"
    var activityType: ActivityType = ActivityType.Walking
    var stepsCount = 0
    val rowsData = mutableListOf<IMUReading>()
    if (f == null) {
        return CsvData(name, experimentTime, activityType, stepsCount, rowsData)
    }
    var parsingHeaders = true

    CSVReader(FileReader(f)).use { reader ->
        var row = reader.readNext()
        while (row != null) {
            if (parsingHeaders && row.size >= 2) {
                // Parse Headers.
                if (row[0].uppercase() == "NAME") {
                    name = row[1]
                } else if (row[0].uppercase() == "EXPERIMENT TIME") {
                    experimentTime = row[1]
                } else if (row[0].uppercase() == "COUNT OF ACTUAL STEPS") {
                    val stepsCountRead = row[1].toIntOrNull()
                    if (stepsCountRead != null) stepsCount = stepsCountRead
                    else {
                        // TODO: log error COUNT OF ACTUAL STEPS type.
                    }
                } else if (row[0].uppercase() == "ACTIVITY TYPE") {
                    try {
                        activityType = ActivityType.valueOf(row[1])
                    } catch (_: IllegalArgumentException) {}
                } else if (row[0].uppercase().startsWith("TIME")) {
                    // The first Data Header
                    parsingHeaders = false
                } else {
                    // TODO: log error unknown header.
                }
            }
            else if (row.size >= 7) {
                // Parse Data Rows.
                val timeSec = row[0].toFloatOrNull()
                val accX = row[1].toFloatOrNull()
                val accY = row[2].toFloatOrNull()
                val accZ = row[3].toFloatOrNull()
                val gyroX = row[4].toFloatOrNull()
                val gyroY = row[5].toFloatOrNull()
                val gyroZ = row[6].toFloatOrNull()
                if (timeSec != null && accX != null && accY != null && accZ != null
                    && gyroX != null && gyroY != null && gyroZ != null) {
                    val rowData = IMUReading(timeSec, accX, accY, accZ, gyroX, gyroY, gyroZ)
                    rowsData.add(rowData)
                } else {
                    // TODO: log error row.
                }
            } else {
                // TODO: log error row based on `parsingHeaders`.
            }
            row = reader.readNext()
        }
    }
    return CsvData(name, experimentTime, activityType, stepsCount, rowsData)
}

fun csvFile(context: Context, fileName: String = ""): File? {
    val directory = csvDir(context)
    val f = File(directory, if (fileName == "") "data.csv" else fileName)
    return if (f.exists()) f else null
}

fun csvDir(context: Context): File =
    File(context.getExternalFilesDir(null), "csv_dir").also { it.mkdirs() }

fun saveToCsv(context: Context, csvData: CsvData) {
    val file = csvFile(context)
    CSVWriter(FileWriter(file, /* append = */ false)).use { w ->
        w.writeNext(arrayOf("NAME", csvData.name, "", "", "", "", ""))
        w.writeNext(arrayOf("EXPERIMENT TIME", csvData.experimentTime, "", "", "", "", ""))
        w.writeNext(arrayOf("ACTIVITY TYPE", csvData.activityType.name, "", "", "", "", ""))
        w.writeNext(arrayOf("COUNT OF ACTUAL STEPS", csvData.stepsCount.toString(), "", "", "", "", ""))
        w.writeNext(arrayOf("", "", "", "", "", "", ""))
        w.writeNext(arrayOf("index", "uniform", "normal"))
        csvData.rows.forEach { rowData ->
            w.writeNext(rowData.toArray())
        }
    }
}

//fun lastCsvFile(context: Context): File? =
//    csvDir(context).listFiles { _, name -> name.lowercase().endsWith(".csv") }
//        ?.maxByOrNull { it.lastModified() }
//
//fun deleteCsv(context: Context, fileName: String): Boolean =
//    csvFile(context, fileName)?.delete() == true
