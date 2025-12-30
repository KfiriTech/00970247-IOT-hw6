package com.example.tutorial6_bluetooth.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutorial6_bluetooth.algorithm.StepCounter
import com.example.tutorial6_bluetooth.databinding.ActivityCsvPlotterBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CSVMetadataDisplay(
    val filename: String?,
    val timestamp: String?,
    val activityType: String?,
    val stepCount: String?
)

data class CSVData(
    val metadata: CSVMetadataDisplay?,
    val readings: List<IMUReading>
)

data class IMUReading(
    val timestamp: Float,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)

class CSVPlotterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCsvPlotterBinding
    private lateinit var chartAcc: LineChart
    private lateinit var chartGyro: LineChart
    private var currentCSVData: CSVData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCsvPlotterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chartAcc = binding.chartAccelerometer
        chartGyro = binding.chartGyroscope

        binding.btnClose.setOnClickListener { finish() }

        binding.btnCalculateSteps.setOnClickListener {
            calculateSteps()
        }

        // Get file path from intent
        val filePath = intent.getStringExtra("FILE_PATH")
        if (filePath != null) {
            loadAndPlotCSV(File(filePath))
        }
    }

    private fun loadAndPlotCSV(file: File) {
        lifecycleScope.launch {
            val csvData = withContext(Dispatchers.IO) {
                parseCSV(file)
            }

            // Store the data for step calculation
            currentCSVData = csvData

            // Display metadata
            if (csvData.metadata != null) {
                binding.tvFilename.text = "File: ${csvData.metadata.filename ?: file.name}"
                binding.tvTimestamp.text = "Time: ${csvData.metadata.timestamp ?: "N/A"}"
                binding.tvActivity.text = "Activity: ${csvData.metadata.activityType ?: "N/A"}"
                binding.tvSteps.text = "Steps: ${csvData.metadata.stepCount ?: "N/A"}"
            } else {
                binding.tvFilename.text = "File: ${file.name}"
                binding.llMetadata.visibility = View.GONE
            }

            // Plot data
            plotAccelerometer(csvData.readings)
            plotGyroscope(csvData.readings)
        }
    }

    private fun parseCSV(file: File): CSVData {
        val lines = file.readLines()
        var metadata: CSVMetadataDisplay? = null
        val dataStartIndex: Int

        // Check for metadata header
        if (lines.firstOrNull()?.startsWith("NAME,") == true) {
            metadata = CSVMetadataDisplay(
                filename = lines.getOrNull(0)?.split(",")?.getOrNull(1),
                timestamp = lines.getOrNull(1)?.split(",")?.getOrNull(1),
                activityType = lines.getOrNull(2)?.split(",")?.getOrNull(1),
                stepCount = lines.getOrNull(3)?.split(",")?.getOrNull(1)
            )
            dataStartIndex = lines.indexOfFirst { it.contains("Time [sec]") || it.contains("timestamp") } + 1
        } else {
            dataStartIndex = 1 // Skip legacy header
        }

        val readings = lines.subList(dataStartIndex.coerceIn(0, lines.size), lines.size)
            .mapNotNull { line ->
                val parts = line.split(",").mapNotNull { it.trim().toFloatOrNull() }
                if (parts.size == 7) {
                    IMUReading(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6])
                } else null
            }

        return CSVData(metadata, readings)
    }

    private fun plotAccelerometer(readings: List<IMUReading>) {
        setupChart(chartAcc, "Accelerometer (m/s²)")

        val dataX = readings.map { Entry(it.timestamp, it.accX) }
        val dataY = readings.map { Entry(it.timestamp, it.accY) }
        val dataZ = readings.map { Entry(it.timestamp, it.accZ) }

        val dataSetX = LineDataSet(dataX, "Acc X").apply {
            color = Color.RED
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val dataSetY = LineDataSet(dataY, "Acc Y").apply {
            color = Color.GREEN
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val dataSetZ = LineDataSet(dataZ, "Acc Z").apply {
            color = Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        chartAcc.data = LineData(dataSetX, dataSetY, dataSetZ)
        chartAcc.invalidate()
    }

    private fun plotGyroscope(readings: List<IMUReading>) {
        setupChart(chartGyro, "Gyroscope (rad/s)")

        val dataX = readings.map { Entry(it.timestamp, it.gyroX) }
        val dataY = readings.map { Entry(it.timestamp, it.gyroY) }
        val dataZ = readings.map { Entry(it.timestamp, it.gyroZ) }

        val dataSetX = LineDataSet(dataX, "Gyro X").apply {
            color = Color.RED
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val dataSetY = LineDataSet(dataY, "Gyro Y").apply {
            color = Color.GREEN
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        val dataSetZ = LineDataSet(dataZ, "Gyro Z").apply {
            color = Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
        }

        chartGyro.data = LineData(dataSetX, dataSetY, dataSetZ)
        chartGyro.invalidate()
    }

    @SuppressLint("SetTextI18n")
    private fun setupChart(chart: LineChart, title: String) {
        chart.description.text = title
        chart.description.textSize = 12f
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = Color.BLACK
        chart.axisLeft.textColor = Color.BLACK
        chart.axisRight.isEnabled = false

        val legend = chart.legend
        legend.isEnabled = true
        legend.textColor = Color.BLACK
        legend.form = Legend.LegendForm.LINE
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSteps() {
        val csvData = currentCSVData ?: return
        val readings = csvData.readings

        if (readings.isEmpty()) {
            binding.tvCalculatedSteps.text = "0"
            return
        }

        lifecycleScope.launch {
            // Disable button during calculation
            binding.btnCalculateSteps.isEnabled = false
            binding.btnCalculateSteps.text = "Calculating..."

            val calculatedSteps = withContext(Dispatchers.IO) {
                // Extract data arrays
                val timestamps = readings.map { it.timestamp }.toFloatArray()
                val accX = readings.map { it.accX }.toFloatArray()
                val accY = readings.map { it.accY }.toFloatArray()
                val accZ = readings.map { it.accZ }.toFloatArray()

                // Determine activity type from metadata or default to Walking
                val activityType = csvData.metadata?.activityType ?: "Walking"

                // Try Python implementation first, fallback to Kotlin if it fails
                try {
                    // Initialize Python if needed
                    StepCounter.initPython(this@CSVPlotterActivity)

                    // Calculate steps using Python algorithm via Chaquopy
                    val result = StepCounter.countStepsFromData(timestamps, accX, accY, accZ, activityType)

                    // If result is 0 but we have significant data, try Kotlin fallback
                    if (result == 0 && readings.size > 10) {
                        android.util.Log.w("CSVPlotter", "Python returned 0, trying Kotlin fallback")
                        com.example.tutorial6_bluetooth.algorithm.StepCounterKotlin.countStepsFromArrays(
                            timestamps, accX, accY, accZ, activityType
                        )
                    } else {
                        result
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CSVPlotter", "Python step counter failed, using Kotlin fallback", e)
                    // Fallback to pure Kotlin implementation
                    com.example.tutorial6_bluetooth.algorithm.StepCounterKotlin.countStepsFromArrays(
                        timestamps, accX, accY, accZ, activityType
                    )
                }
            }

            // Display calculated steps
            binding.tvCalculatedSteps.text = calculatedSteps.toString()

            // Show comparison if recorded steps are available
            val recordedStepsStr = csvData.metadata?.stepCount
            if (!recordedStepsStr.isNullOrEmpty() && recordedStepsStr != "N/A" && recordedStepsStr != "PENDING") {
                val recordedSteps = recordedStepsStr.toIntOrNull()
                if (recordedSteps != null && recordedSteps > 0) {
                    binding.llComparison.visibility = View.VISIBLE
                    binding.tvRecordedSteps.text = recordedSteps.toString()

                    // Calculate accuracy
                    val accuracy = (calculatedSteps.toFloat() / recordedSteps.toFloat() * 100).coerceIn(0f, 200f)
                    val accuracyText = String.format("%.1f%%", accuracy)
                    binding.tvAccuracy.text = accuracyText

                    // Color code accuracy
                    when {
                        accuracy >= 90f && accuracy <= 110f -> binding.tvAccuracy.setTextColor(Color.parseColor("#4CAF50")) // Green
                        accuracy >= 80f && accuracy <= 120f -> binding.tvAccuracy.setTextColor(Color.parseColor("#FF9800")) // Orange
                        else -> binding.tvAccuracy.setTextColor(Color.parseColor("#F44336")) // Red
                    }
                }
            }

            // Re-enable button
            binding.btnCalculateSteps.isEnabled = true
            binding.btnCalculateSteps.text = "Calculate Steps"

            android.util.Log.d("CSVPlotter", "Calculated steps: $calculatedSteps from ${readings.size} readings")
        }
    }
}