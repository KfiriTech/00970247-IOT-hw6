package com.example.tutorial6_bluetooth.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutorial6_bluetooth.R
import com.example.tutorial6_bluetooth.constants.Constants
import com.example.tutorial6_bluetooth.databinding.ActivityTerminalBinding
import com.example.tutorial6_bluetooth.service.SerialService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding
    private val messages = ArrayList<Message>()
    private lateinit var adapter: MessageAdapter
    private val buffer = StringBuilder()

    data class Message(val text: String, val isIncoming: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("TerminalActivity", "onCreate")
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilename()
        setupSlowMode()

        binding.btnReturn.setOnClickListener {
            finish()
        }

        binding.btnRecord.setOnClickListener {
            val isRecording = binding.btnRecord.text == "Stop Recording"
            android.util.Log.d("TerminalActivity", "btnRecord clicked: isRecording=$isRecording")
            if (isRecording) {
                // Stop
                val intent = Intent(this, SerialService::class.java)
                intent.action = Constants.ACTION_STOP_LOGGING
                startService(intent)
            } else {
                // Start
                val prefix = binding.etLogPrefix.text.toString()
                val type = if (binding.rbCsv.isChecked) "CSV" else "TXT"
                
                val intent = Intent(this, SerialService::class.java)
                intent.action = Constants.ACTION_START_LOGGING
                intent.putExtra(Constants.EXTRA_LOG_FILENAME_PREFIX, prefix)
                intent.putExtra(Constants.EXTRA_LOG_TYPE, type)
                startService(intent)
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
    private fun updateRecordingUI(filename: String?) {
        if (filename != null) {
            // Recording
            binding.btnRecord.text = "Stop Recording"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
            binding.etLogPrefix.isEnabled = false
            binding.rbTxt.isEnabled = false
            binding.rbCsv.isEnabled = false
            binding.tvFilename.text = "Recording: $filename"
        } else {
            // Not Recording
            binding.btnRecord.text = "Record"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_blue_light)
            binding.etLogPrefix.isEnabled = true
            binding.rbTxt.isEnabled = true
            binding.rbCsv.isEnabled = true
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
