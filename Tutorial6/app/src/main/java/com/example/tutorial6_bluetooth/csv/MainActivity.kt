package com.example.tutorial6_bluetooth.csv

//import android.content.Intent
//import android.os.Bundle
//import android.widget.LinearLayout
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.lifecycle.lifecycleScope
//import com.example.tutorial6_bluetooth.databinding.ActivityMainBinding
//import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
//import com.patrykandpatrick.vico.core.cartesian.Scroll
//import com.patrykandpatrick.vico.core.cartesian.Zoom
//import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
//import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
//import com.patrykandpatrick.vico.views.cartesian.ScrollHandler
//import com.patrykandpatrick.vico.views.cartesian.ZoomHandler
//import java.util.Random
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import kotlin.collections.mutableListOf
//import kotlin.math.min
//
//class MainActivity : ComponentActivity() {
//    private lateinit var binding: ActivityMainBinding
//
//    val yScale = 80
//
//    // we’ll use implicit x=index; Vico will place points at x=[0,1,2,...]
//    private val displaySeries = CsvSeries(0, mutableListOf(), mutableListOf())
//    private var csvIndex = 0
//    private val random = Random()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Make sure the chart never exceeds half of the screen height.
//        val half = (resources.displayMetrics.heightPixels * 0.5f).toInt()
//        (binding.chartView.layoutParams as LinearLayout.LayoutParams).apply {
//            // if XML gave a fixed height, keep the smaller between that and 'half'
//            height = min(if (height > 0) height else half, half)
//            weight = 0f // prevent LinearLayout from stretching it beyond our height
//            binding.chartView.layoutParams = this
//        }
//
//        // First render without animations (we’re using a synchronous model now).
//        val initialData = readCsv(this@MainActivity)
//        csvIndex += initialData.series.index
//        updateChart()
//
//        // Periodic updates
//        lifecycleScope.launch {
//            while (isActive) {
//                val u = (0..yScale).random()
//                val n = ((random.nextGaussian() / 2 + 1) * yScale / 2).toFloat()
//
//                displaySeries.uniform += u.toFloat()
//                displaySeries.normal += n
//                saveToCsv(this@MainActivity, csvIndex, u, n)
//                displaySeries.index++
//                csvIndex++
//
//                updateChart()
//                delay(500)
//            }
//        }
//
//        // UI bindings
//
//        binding.buttonStats.setOnClickListener {
//            val newIntent = Intent(this, StatsActivity::class.java)
//            // Make sure that the back button will return to this Activity.
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//            startActivity(newIntent)
//        }
//
//        binding.buttonCsvShow.setOnClickListener {
//            val newIntent = Intent(this, LoadCsvActivity::class.java)
//            // Make sure that the back button will return to this Activity.
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//            startActivity(newIntent)
//        }
//
//        binding.buttonClear.setOnClickListener {
//            displaySeries.uniform.clear()
//            displaySeries.normal.clear()
//            displaySeries.index = 0
//            updateChart()
//            Toast.makeText(this, "Chart cleared.", Toast.LENGTH_SHORT).show()
//        }
//
//        // Delete the most recently created CSV and reset the series.
//        binding.buttonDeleteCsv.setOnClickListener {
//            val deleted = deleteLastCsv(this)
//            displaySeries.uniform.clear()
//            displaySeries.normal.clear()
//            displaySeries.index = 0
//            csvIndex = 0
//            updateChart()
//            Toast.makeText(
//                this,
//                if (deleted) "CSV deleted, starting fresh." else "No CSV found.",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//
//        // nice to have for visualization. you can read more at vico's documentation. feel free to remove
//        binding.chartView.scrollHandler = ScrollHandler(
//            initialScroll = Scroll.Absolute.End,            // open pinned to the end
//            autoScroll = Scroll.Absolute.End,               // keep pinning to end…
//            autoScrollCondition = AutoScrollCondition.OnModelGrowth // …only when data grows
//        )
//
//        binding.chartView.zoomHandler = ZoomHandler(
//            initialZoom = Zoom.max(
//                Zoom.fixed(.5F),
//                Zoom.Content
//            ),   // show 20 x-units (i.e., ~last 20 samples)
//            minZoom = Zoom.Content,       // allow zooming in to ~10 visible samples
//            maxZoom = Zoom.max(
//                Zoom.fixed(50F),
//                Zoom.Content
//            )        // allow zooming out to see all content
//        )
//    }
//
//    private fun updateChart() {
//        // Only update the chart if there is data to show.
//        if (displaySeries.uniform.isNotEmpty() || displaySeries.normal.isNotEmpty()) {
//            val model = CartesianChartModel(
//                LineCartesianLayerModel.build {
//                    series(displaySeries.uniform)
//                    series(displaySeries.normal)
//                }
//            )
//            binding.chartView.model = model
//
//            val data = CsvData(displaySeries)
//            binding.statsDisplay.text = """
//                Uniform: μ=%.2f σ=%.2f | Normal: μ=%.2f σ=%.2f | New Samples: %d
//            """.trimIndent().format(
//                data.uniform.mean,
//                data.uniform.std,
//                data.normal.mean,
//                data.normal.std,
//                data.series.index
//            )
//        } else {
//            // If the list is empty, clear the chart.
//            binding.chartView.model = null
//            binding.statsDisplay.text = "Statistics will appear here..."
//        }
//        binding.statsDisplayTotal.text = """Total Samples: %d""".format(csvIndex)
//    }
//}
