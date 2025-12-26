package com.example.tutorial6_bluetooth.csv

//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.lifecycle.lifecycleScope
//import com.example.tutorial6_bluetooth.databinding.ActivityStatsBinding
//import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
//import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class StatsActivity : ComponentActivity() {
//    private lateinit var binding: ActivityStatsBinding
//    private var updateJob: Job? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityStatsBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.buttonToMain.setOnClickListener {
//            finish()
//        }
//
//        binding.buttonToLoadCsv.setOnClickListener {
//            val newIntent = Intent(this, LoadCsvActivity::class.java)
//            // Make sure that the back button will return to the main Activity.
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//            startActivity(newIntent)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        startAutoUpdate()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        stopAutoUpdate()
//    }
//
//    private fun startAutoUpdate() {
//        stopAutoUpdate()
//        updateJob = lifecycleScope.launch {
//            while (isActive) {
//                loadAndDisplayStats()
//                delay(500) // Update twice every second
//            }
//        }
//    }
//
//    private fun stopAutoUpdate() {
//        updateJob?.cancel()
//        updateJob = null
//    }
//
//    private suspend fun loadAndDisplayStats() {
//        val data = withContext(Dispatchers.IO) { readCsv(this@StatsActivity) }
//
//        // Display stats info
//        if (data.uniform.values.isEmpty() || data.normal.values.isEmpty()) {
//            binding.statsInfo.text = "No data available. Generate some data in Main activity first."
//        } else {
//            binding.statsInfo.text = """
//                Uniform: μ=%.2f σ=%.2f | Normal: μ=%.2f σ=%.2f | Samples: %d
//            """.trimIndent().format(data.uniform.mean, data.uniform.std, data.normal.mean, data.normal.std, data.uniform.values.size)
//
//            // Create column chart with 4 separate series for different colors
//            val model = CartesianChartModel(
//                ColumnCartesianLayerModel.build {
//                    series(data.uniform.mean)      // Series 0: Uniform Mean (dark blue)
//                    series(data.uniform.std)    // Series 1: Uniform StdDev (light blue)
//                    series(data.normal.mean)       // Series 2: Normal Mean (dark orange)
//                    series(data.normal.std)     // Series 3: Normal StdDev (light orange)
//                }
//            )
//            binding.chartViewStats.model = model
//        }
//    }
//}
