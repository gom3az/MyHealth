package com.gomaa.healthy.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogFileWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logFile: File by lazy {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        File(logDir, "myhealth-debug-log.html")
    }

    private fun getLevelColor(level: String): String = when (level) {
        "E" -> "#FF5252"
        "W" -> "#FFD740"
        "I" -> "#448AFF"
        "D" -> "#E040FB"
        else -> "#FFFFFF"
    }

    @Synchronized
    fun write(level: String, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val timestamp = dateFormat.format(Date())
            val color = getLevelColor(level)

            val logEntry = buildString {
                append("<p style=\"margin:0;padding:0;\">")
                append("<span style=\"color:#888;\">[$timestamp]</span> ")
                append("<span style=\"color:$color;font-weight:bold;\">$level</span>")
                append("/<span style=\"color:#CCC;\">$tag</span>: ")
                append("<span style=\"color:#FFF;\">$message</span>")
                throwable?.let {
                    append("<pre style=\"color:#FF5252;margin:4px 0;padding:4px;background:#222;border-radius:4px;\">")
                    append(getStackTrace(it).replace("<", "&lt;").replace(">", "&gt;"))
                    append("</pre>")
                }
                append("</p>")
            }

            if (logFile.length() == 0L) {
                FileWriter(logFile, true).use { writer ->
                    writer.append(
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width,initial-scale=1.0">
                            <style>
                                body { background:#000; color:#FFF; font-family:monospace; font-size:12px; padding:8px; }
                                pre { white-space:pre-wrap; word-wrap:break-word; }
                            </style>
                        </head>
                        <body>
                    """.trimIndent()
                    )
                }
            }

            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry)
            }
        } catch (e: Exception) {
            android.util.Log.e("LogFileWriter", "Failed to write log: ${e.message}")
        }
    }

    fun getLogFilePath(): File = logFile

    private fun getStackTrace(throwable: Throwable): String {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
    }
}
