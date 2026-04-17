package com.gomaa.healthy.logging

import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @param enableFileLogging Whether to write logs to file. Should be false in release builds.
 */
@Singleton
class AppLogger @Inject constructor(
    private val fileWriter: LogFileWriter,
    private val enableFileLogging: Boolean = false
) {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        if (enableFileLogging) {
            fileWriter.write("D", tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        if (enableFileLogging) {
            fileWriter.write("E", tag, message, throwable)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        if (enableFileLogging) {
            fileWriter.write("I", tag, message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        if (enableFileLogging) {
            fileWriter.write("W", tag, message, throwable)
        }
    }

    fun getLogFile(): File = fileWriter.getLogFilePath()
}
