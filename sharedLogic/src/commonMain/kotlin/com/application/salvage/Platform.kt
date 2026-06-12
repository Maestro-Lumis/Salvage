package com.application.salvage

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform