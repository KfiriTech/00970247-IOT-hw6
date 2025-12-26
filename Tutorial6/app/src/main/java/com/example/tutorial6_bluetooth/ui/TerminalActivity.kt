package com.example.tutorial6_bluetooth.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutorial6_bluetooth.R
import com.example.tutorial6_bluetooth.constants.Constants
import com.example.tutorial6_bluetooth.databinding.ActivityTerminalBinding
import com.example.tutorial6_bluetooth.service.SerialService
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding
    private val messages = ArrayList<Message>()
    private lateinit var adapter: MessageAdapter
    private val buffer = StringBuilder()

    // Chart for real-time accelerometer plotting
    private lateinit var chart: LineChart
    private val chartDataX = ArrayList<Entry>()
    private val chartDataY = ArrayList<Entry>()
    private val chartDataZ = ArrayList<Entry>()
    private val maxDataPoints = 100 // Rolling window

    data class Message(val text: String, val isIncoming: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("TerminalActivity", "onCreate")
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilename()
        setupSlowMode()
        setupChart()

        binding.btnReturn.setOnClickListener {
            finish()
        }

        binding.btnRecord.setOnClickListener {
            val isRecording = binding.btnRecord.text == "Stop Recording"
            android.util.Log.d("TerminalActivity", "btnRecord clicked: isRecording=$isRecording")
            if (isRecording) {
                // Stop - Show step count dialog
                showStepCountDialog()
            } else {
                // Start - Capture metadata
                val prefix = binding.etLogPrefix.text.toString()
                val type = if (binding.rbCsv.isChecked) "CSV" else "TXT"
                val activityType = if (binding.rbRunning.isChecked) "Running" else "Walking"
                // Format: dd/MM/yyyy  HH:mm (with 2 spaces before time as per assignment)
                val timestamp = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()).format(Date())

                val intent = Intent(this, SerialService::class.java)
                intent.action = Constants.ACTION_START_LOGGING
                intent.putExtra(Constants.EXTRA_LOG_FILENAME_PREFIX, prefix)
                intent.putExtra(Constants.EXTRA_LOG_TYPE, type)
                intent.putExtra(Constants.EXTRA_ACTIVITY_TYPE, activityType)
                intent.putExtra(Constants.EXTRA_RECORDING_TIMESTAMP, timestamp)
                startService(intent)

                // Disable activity type selection during recording
                binding.rbWalking.isEnabled = false
                binding.rbRunning.isEnabled = false
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etInput.text.clear()
            }
        }
    }

    private var serialService: SerialService? = null
    private var isBound = false

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(className: android.content.ComponentName, service: android.os.IBinder) {
            val binder = service as SerialService.SerialBinder
            serialService = binder.getService()
            isBound = true
            
            // Sync UI with current service state
            if (serialService?.isConnected == true) {
                updateRecordingUI(serialService?.getLogFilename())
                binding.etInput.isEnabled = true
                binding.btnSend.isEnabled = true
                binding.etInput.hint = "Type a message"
                binding.btnRecord.isEnabled = true
            } else {
                updateRecordingUI(null)
                binding.etInput.isEnabled = false
                binding.btnSend.isEnabled = false
                binding.etInput.hint = "Disconnected"
                binding.btnRecord.isEnabled = false
            }
        }

        override fun onServiceDisconnected(arg0: android.content.ComponentName) {
            isBound = false
            serialService = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to SerialService
        val intent = Intent(this, SerialService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("TerminalActivity", "onResume")
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_SERIAL_DATA_RECEIVED)
        filter.addAction(Constants.ACTION_SERIAL_STATE_CHANGED)
        filter.addAction(Constants.ACTION_IMU_DATA_RECEIVED)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("TerminalActivity", "onPause")
        unregisterReceiver(receiver)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.ACTION_SERIAL_DATA_RECEIVED -> {
                    val data = intent.getByteArrayExtra(Constants.EXTRA_DATA)
                    if (data != null) {
                        val text = String(data)
                        val lines = com.example.tutorial6_bluetooth.logging.LogUtils.processBuffer(buffer, text)
                        for (line in lines) {
                            addMessage(line, true)
                        }
                    }
                }
                Constants.ACTION_IMU_DATA_RECEIVED -> {
                    val timestamp = intent.getFloatExtra("timestamp", 0f)
                    val accX = intent.getFloatExtra("accX", 0f)
                    val accY = intent.getFloatExtra("accY", 0f)
                    val accZ = intent.getFloatExtra("accZ", 0f)
                    val gyroX = intent.getFloatExtra("gyroX", 0f)
                    val gyroY = intent.getFloatExtra("gyroY", 0f)
                    val gyroZ = intent.getFloatExtra("gyroZ", 0f)

                    updateIMUDisplay(timestamp, accX, accY, accZ, gyroX, gyroY, gyroZ)
                }
                Constants.ACTION_SERIAL_STATE_CHANGED -> {
                    val isConnected = intent.getBooleanExtra(Constants.EXTRA_SERVICE_CONNECTED, false)
                    val logFilename = intent.getStringExtra(Constants.EXTRA_LOG_FILENAME)
                    android.util.Log.d("TerminalActivity", "State changed: isConnected=$isConnected, logFilename=$logFilename")

                    updateRecordingUI(logFilename)

                    if (!isConnected) {
                        binding.etInput.isEnabled = false
                        binding.btnSend.isEnabled = false
                        binding.etInput.hint = "Disconnected"
                        binding.btnRecord.isEnabled = false
                    } else {
                        binding.etInput.isEnabled = true
                        binding.btnSend.isEnabled = true
                        binding.etInput.hint = "Type a message"
                        binding.btnRecord.isEnabled = true
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateIMUDisplay(timestamp: Float, accX: Float, accY: Float, accZ: Float, gyroX: Float, gyroY: Float, gyroZ: Float) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvTimestamp.text = String.format("%.2f", timestamp)
            binding.tvAccX.text = String.format("X: %.2f", accX)
            binding.tvAccY.text = String.format("Y: %.2f", accY)
            binding.tvAccZ.text = String.format("Z: %.2f", accZ)

            // Update real-time chart with accelerometer data
            updateChart(timestamp, accX, accY, accZ)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecordingUI(filename: String?) {
        if (filename != null) {
            // Recording
            binding.btnRecord.text = "Stop Recording"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
            binding.etLogPrefix.isEnabled = false
            binding.rbTxt.isEnabled = false
            binding.rbCsv.isEnabled = false
            binding.rbWalking.isEnabled = false
            binding.rbRunning.isEnabled = false
            binding.tvFilename.text = "Recording: $filename"
        } else {
            // Not Recording
            binding.btnRecord.text = "Record"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_blue_light)
            binding.etLogPrefix.isEnabled = true
            binding.rbTxt.isEnabled = true
            binding.rbCsv.isEnabled = true
            binding.rbWalking.isEnabled = true
            binding.rbRunning.isEnabled = true
            binding.tvFilename.text = "Not Recording"
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFilename() {
        // Initial state
        updateRecordingUI(null)
    }

    private var isSlowMode = false
    private val pendingMessages = ArrayList<Message>()
    private val updateHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (pendingMessages.isNotEmpty()) {
                val count = pendingMessages.size
                val start = messages.size
                messages.addAll(pendingMessages)
                pendingMessages.clear()
                adapter.notifyItemRangeInserted(start, count)
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
            if (isSlowMode) {
                updateHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun setupSlowMode() {
        binding.swSlowMode.setOnCheckedChangeListener { _, isChecked ->
            isSlowMode = isChecked
            if (isChecked) {
                updateHandler.post(updateRunnable)
            } else {
                updateHandler.removeCallbacks(updateRunnable)
                updateRunnable.run() // Flush remaining
            }
        }
    }

    private fun addMessage(text: String, isIncoming: Boolean) {
        if (isSlowMode) {
            pendingMessages.add(Message(text, isIncoming))
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                messages.add(Message(text, isIncoming))
                adapter.notifyItemInserted(messages.size - 1)
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage(text: String) {
        android.util.Log.d("TerminalActivity", "Sending message: $text")
        // Always add sent messages immediately for better UX, or follow slow mode?
        // Usually sent messages should be immediate. Let's keep them immediate.
        lifecycleScope.launch(Dispatchers.Main) {
            messages.add(Message(text, false))
            adapter.notifyItemInserted(messages.size - 1)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }

        // Send intent to service to write data
        val intent = Intent(this, SerialService::class.java)
        intent.action = Constants.ACTION_WRITE_DATA
        intent.putExtra(Constants.EXTRA_DATA, text.toByteArray())
        startService(intent)
    }

    private fun setupChart() {
        chart = binding.chartAccelerometer

        // Chart styling
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(false)
        chart.setNoDataText("Waiting for IMU data...")
        chart.setNoDataTextColor(Color.GRAY)

        // X-axis (Time)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = Color.BLACK
        chart.xAxis.setDrawGridLines(true)
        chart.xAxis.granularity = 1f
        chart.xAxis.labelCount = 5

        // Y-axis (Acceleration) - auto-scale to data
        chart.axisLeft.textColor = Color.BLACK
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.isGranularityEnabled = true
        chart.axisLeft.granularity = 0.5f
        chart.axisRight.isEnabled = false

        // Enable auto-scaling
        chart.isAutoScaleMinMaxEnabled = true

        // Legend
        val legend = chart.legend
        legend.isEnabled = true
        legend.textColor = Color.BLACK
        legend.textSize = 10f
        legend.form = Legend.LegendForm.LINE
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(true)

        // Don't initialize chart data yet - wait for first IMU data
        // This prevents NegativeArraySizeException when drawing empty datasets
    }

    private fun updateChart(timestamp: Float, accX: Float, accY: Float, accZ: Float) {
        // Initialize chart data on first data point
        if (chart.data == null) {
            val dataSetX = LineDataSet(chartDataX, "Acc X").apply {
                color = Color.RED
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
            }

            val dataSetY = LineDataSet(chartDataY, "Acc Y").apply {
                color = Color.GREEN
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
            }

            val dataSetZ = LineDataSet(chartDataZ, "Acc Z").apply {
                color = Color.BLUE
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
            }

            val lineData = LineData(dataSetX, dataSetY, dataSetZ)
            chart.data = lineData
        }

        // Rolling window: remove old data before adding new
        if (chartDataX.size >= maxDataPoints) {
            chart.data?.let { data ->
                // Use dataset's removeEntry method for proper chart updates
                (data.getDataSetByIndex(0) as? LineDataSet)?.removeFirst()
                (data.getDataSetByIndex(1) as? LineDataSet)?.removeFirst()
                (data.getDataSetByIndex(2) as? LineDataSet)?.removeFirst()
            }
        }

        // Add new data points
        chart.data?.let { data ->
            (data.getDataSetByIndex(0) as? LineDataSet)?.addEntry(Entry(timestamp, accX))
            (data.getDataSetByIndex(1) as? LineDataSet)?.addEntry(Entry(timestamp, accY))
            (data.getDataSetByIndex(2) as? LineDataSet)?.addEntry(Entry(timestamp, accZ))
        }

        // Notify chart of data change
        chart.data?.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(50f) // Show last 50 time units
        chart.moveViewToX(timestamp) // Auto-scroll
        chart.invalidate()
    }

    private fun showStepCountDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter step count"

        AlertDialog.Builder(this)
            .setTitle("Step Count")
            .setMessage("Enter the actual number of steps:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val steps = input.text.toString().ifEmpty { "N/A" }

                // Send stop intent with step count
                val intent = Intent(this, SerialService::class.java)
                intent.action = Constants.ACTION_STOP_LOGGING
                intent.putExtra(Constants.EXTRA_STEP_COUNT, steps)
                startService(intent)

                dialog.dismiss()
            }
            .setNegativeButton("Skip") { dialog, _ ->
                val intent = Intent(this, SerialService::class.java)
                intent.action = Constants.ACTION_STOP_LOGGING
                intent.putExtra(Constants.EXTRA_STEP_COUNT, "N/A")
                startService(intent)

                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    inner class MessageAdapter(private val messages: List<Message>) :
        RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_terminal_message, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val message = messages[position]
            holder.textView.text = message.text
            if (message.isIncoming) {
                holder.textView.setTextColor(Color.GREEN)
            } else {
                holder.textView.setTextColor(Color.BLUE)
            }
        }

        override fun getItemCount(): Int = messages.size
    }
}
