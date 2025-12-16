package com.example.tutorial6_bluetooth.logging

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class TextFileLogger : DataLogger {

    private var logFileOutputStream: FileOutputStream? = null

    override fun start(context: Context, prefix: String, type: String): String {
        val fileName = LogUtils.generateFilename(prefix, type)
        val file = File(context.getExternalFilesDir(null), fileName)
        try {
            logFileOutputStream = FileOutputStream(file, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return fileName
    }

    override fun log(data: String) {
        // TODO: The incoming data might be fragmented (e.g. "12", "3.45", "\n").
        // To ensure we write complete lines to the file, use a StringBuilder buffer and LogUtils.processBuffer().
        // Example:
        // val lines = LogUtils.processBuffer(buffer, data)
        // for (line in lines) {
        //     logFileOutputStream?.write((line + "\n").toByteArray())
        // }
        
        try {
            logFileOutputStream?.write(data.toByteArray())
            logFileOutputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        try {
            logFileOutputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logFileOutputStream = null
    }
}
