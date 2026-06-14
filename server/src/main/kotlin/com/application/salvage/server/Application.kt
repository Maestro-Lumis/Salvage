package com.application.salvage.server

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json

fun main() {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    embeddedServer(Netty, port = 8081) {
        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowHeader(HttpHeaders.ContentType)
            anyHost()
        }
        configureRouting(httpClient)
    }.start(wait = true)
}
