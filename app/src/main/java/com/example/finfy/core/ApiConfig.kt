package com.example.finfy.core

object ApiConfig {
    // Device via ADB reverse: adb reverse tcp:4000 tcp:4000
    // Emulator: http://10.0.2.2:4000/
    const val BASE_URL = "http://127.0.0.1:4000/"
    const val AUTH_BASE = "/api/bff/auth"
}
