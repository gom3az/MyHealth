package com.gomaa.healthy.logging

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppLogger = staticCompositionLocalOf<AppLogger> {
    error("No AppLogger provided")
}
