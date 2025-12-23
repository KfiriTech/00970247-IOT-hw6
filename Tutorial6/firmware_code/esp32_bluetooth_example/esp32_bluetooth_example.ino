#include "BluetoothSerial.h"
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

// Check Serial Port Profile
#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Port Profile for Bluetooth is not available or not enabled. It is only available for the ESP32 chip.
#endif

BluetoothSerial SerialBT;
Adafruit_MPU6050 mpu;

String device_name = "ESP32-BT-Slave22";

bool flagBTConnected = false;
bool isSending = false; // Start with sending disabled until START command

// Sampling rate variables
const int SAMPLE_RATE_HZ = 10;  // 10Hz as required (minimum)
const int SAMPLE_PERIOD_MS = 1000 / SAMPLE_RATE_HZ;  // 100ms
unsigned long lastSampleTime = 0;

void setup()
{
  Serial.begin(115200);

  // Initialize I2C
  Wire.begin();

  // Initialize MPU6050
  Serial.println("Initializing MPU6050...");
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip!");
    while (1) {
      delay(10);
    }
  }
  Serial.println("MPU6050 Found!");

  // Configure MPU6050 settings
  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);  // ±8g range
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);       // ±500 deg/s range
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);    // Low-pass filter

  Serial.print("Accelerometer range set to: ±");
  switch (mpu.getAccelerometerRange()) {
    case MPU6050_RANGE_2_G:  Serial.println("2G"); break;
    case MPU6050_RANGE_4_G:  Serial.println("4G"); break;
    case MPU6050_RANGE_8_G:  Serial.println("8G"); break;
    case MPU6050_RANGE_16_G: Serial.println("16G"); break;
  }

  Serial.print("Gyro range set to: ±");
  switch (mpu.getGyroRange()) {
    case MPU6050_RANGE_250_DEG:  Serial.println("250 deg/s"); break;
    case MPU6050_RANGE_500_DEG:  Serial.println("500 deg/s"); break;
    case MPU6050_RANGE_1000_DEG: Serial.println("1000 deg/s"); break;
    case MPU6050_RANGE_2000_DEG: Serial.println("2000 deg/s"); break;
  }

  // Initialize Bluetooth
  SerialBT.begin(device_name);
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());
  Serial.println("Waiting for START command...");
}

void loop()
{
  // Handle incoming Bluetooth commands
  if (SerialBT.available()) {
    delay(20); // Wait for the full message to arrive
    String incoming = "";
    while(SerialBT.available()) {
      incoming += (char)SerialBT.read();
    }
    incoming.trim();
    Serial.println("Received: " + incoming);

    if (incoming == "STOP") {
      isSending = false;
      SerialBT.println("Stopped");
      Serial.println("Data transmission stopped");
    } else if (incoming == "START") {
      isSending = true;
      lastSampleTime = millis(); // Reset timing
      SerialBT.println("Started");
      Serial.println("Data transmission started");
    }
  }

  // Check Bluetooth connection status
  if (SerialBT.hasClient()) {
    if (flagBTConnected == false) {
      Serial.println("Bluetooth client connected!");
      flagBTConnected = true;
    }

    // Send IMU data at fixed sample rate
    if (isSending) {
      unsigned long currentTime = millis();

      // Check if it's time for the next sample
      if (currentTime - lastSampleTime >= SAMPLE_PERIOD_MS) {
        lastSampleTime = currentTime;

        // Read sensor data
        sensors_event_t accel, gyro, temp;
        mpu.getEvent(&accel, &gyro, &temp);

        // Acceleration values are already in m/s²
        float acc_x = accel.acceleration.x;
        float acc_y = accel.acceleration.y;
        float acc_z = accel.acceleration.z;

        // Gyroscope values are already in rad/s
        float gyro_x = gyro.gyro.x;
        float gyro_y = gyro.gyro.y;
        float gyro_z = gyro.gyro.z;

        // Format: acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z
        // This matches the format expected by IMUDataParser.kt
        String dataString = String(acc_x, 2) + "," +
                           String(acc_y, 2) + "," +
                           String(acc_z, 2) + "," +
                           String(gyro_x, 2) + "," +
                           String(gyro_y, 2) + "," +
                           String(gyro_z, 2);

        // Send via Bluetooth
        SerialBT.println(dataString);

        // Debug output to Serial Monitor
        Serial.println(dataString);
      }
    } else {
      delay(100); // Small delay when not sending
    }
  } else {
    if (flagBTConnected == true) {
      flagBTConnected = false;
      isSending = false; // Stop sending if client disconnects
      Serial.println("Bluetooth client disconnected");
    }
    delay(1000); // Wait 1 second between connection checks
  }
}
