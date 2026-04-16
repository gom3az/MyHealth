package com.gomaa.healthy.logging

import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    private val fileWriter: LogFileWriter
) {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        fileWriter.write("D", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        fileWriter.write("E", tag, message, throwable)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        fileWriter.write("I", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        fileWriter.write("W", tag, message, throwable)
    }

    fun getLogFile(): File = fileWriter.getLogFilePath()
}
