package com.example.tutorial6_bluetooth.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tutorial6_bluetooth.constants.Constants
import com.example.tutorial6_bluetooth.databinding.ActivityMainBinding
import com.example.tutorial6_bluetooth.service.SerialService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()

        binding.btnConnect.setOnClickListener {
            android.util.Log.d("MainActivity", "btnConnect clicked")
            if (hasPermissions()) {
                val intent = Intent(this, DeviceListActivity::class.java)
                startActivity(intent)
            } else {
                checkPermissions()
            }
        }

        binding.btnDisconnect.setOnClickListener {
            android.util.Log.d("MainActivity", "btnDisconnect clicked")
            val intent = Intent(this, SerialService::class.java)
            intent.action = Constants.ACTION_SERIAL_DISCONNECT
            startService(intent)
        }

        binding.btnTerminal.setOnClickListener {
            android.util.Log.d("MainActivity", "btnTerminal clicked")
            val intent = Intent(this, TerminalActivity::class.java)
            startActivity(intent)
        }

        binding.btnFileExplorer.setOnClickListener {
            val intent = Intent(this, FileExplorerActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnTerminal.isEnabled = false // Disabled by default
    }

    private var serialService: SerialService? = null
    private var isBound = false

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(className: android.content.ComponentName, service: android.os.IBinder) {
            val binder = service as SerialService.SerialBinder
            serialService = binder.getService()
            isBound = true
            updateConnectionState(serialService?.isConnected == true)
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

    private fun updateConnectionState(isConnected: Boolean) {
        if (isConnected) {
            binding.btnConnect.isEnabled = false
            binding.btnDisconnect.isEnabled = true
            binding.btnTerminal.isEnabled = true
        } else {
            binding.btnConnect.isEnabled = true
            binding.btnDisconnect.isEnabled = false
            binding.btnTerminal.isEnabled = false
        }
    }

    private val connectionStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_SERIAL_STATE_CHANGED) {
                val isConnected = intent.getBooleanExtra(Constants.EXTRA_SERVICE_CONNECTED, false)
                android.util.Log.d("MainActivity", "Connection state changed: isConnected=$isConnected")
                updateConnectionState(isConnected)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume")
        ContextCompat.registerReceiver(
            this, 
            connectionStateReceiver, 
            android.content.IntentFilter(Constants.ACTION_SERIAL_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Also update from bound service if available
        if (isBound && serialService != null) {
            updateConnectionState(serialService?.isConnected == true)
        }
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "onPause")
        unregisterReceiver(connectionStateReceiver)
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
