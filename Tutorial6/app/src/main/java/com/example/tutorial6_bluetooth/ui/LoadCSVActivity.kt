package com.example.tutorial6_bluetooth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutorial6_bluetooth.csv.readCsv
import com.example.tutorial6_bluetooth.databinding.ActivityLoadCsvBinding
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadCsvActivity : ComponentActivity() {
    private lateinit var binding: ActivityLoadCsvBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileName = intent.getStringExtra("fileName") ?: ""

        binding = ActivityLoadCsvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonToMain.setOnClickListener {
            // Go back to whatever is underneath (MainActivity in your flow)
            finish()
        }

//        binding.buttonToStats.setOnClickListener {
//            val newIntent = Intent(this, StatsActivity::class.java)
//            // Make sure that the back button will return to the main Activity.
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//            startActivity(newIntent)
//        }

        val half = (resources.displayMetrics.heightPixels * 0.5f).toInt()
        (binding.chartViewCsv.layoutParams as LinearLayout.LayoutParams).apply {
            height = half
            weight = 0f
            binding.chartViewCsv.layoutParams = this
        }

        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { readCsv(this@LoadCsvActivity) }
            val timeSec: List<Float> = data.rows.map { it.timeSec }
            val accX: List<Float> = data.rows.map { it.accX }
            val accY: List<Float> = data.rows.map { it.accY }
            val accZ: List<Float> = data.rows.map { it.accZ }
            val gyroX: List<Float> = data.rows.map { it.gyroX }
            val gyroY: List<Float> = data.rows.map { it.gyroY }
            val gyroZ: List<Float> = data.rows.map { it.gyroZ }

            val model = CartesianChartModel(
                LineCartesianLayerModel.build {
                    series(x=timeSec, y=accX)
                    series(x=timeSec, y=accY)
                    series(x=timeSec, y=accZ)
                    series(x=timeSec, y=gyroX)
                    series(x=timeSec, y=gyroY)
                    series(x=timeSec, y=gyroZ)
                }
            )
            binding.chartViewCsv.model = model
        }
    }
}
