package com.gomaa.healthy.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashExceptionHandler @Inject constructor(
    private val fileWriter: LogFileWriter
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun initialize() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        fileWriter.write("E", "CRASH", "Uncaught exception on thread: ${thread.name}", throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }
}