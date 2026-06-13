package com.application.salvage.di

import app.salvage.data.remote.api.SteamApiService
import app.salvage.data.repository.ItemRepositoryImpl
import app.salvage.domain.repository.ItemRepository
import app.salvage.domain.usecase.GetItemsUseCase
import app.salvage.domain.usecase.GetPriceHistoryUseCase
import app.salvage.domain.usecase.GetTopDealsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(Logging) { level = LogLevel.HEADERS }
        }
    }
    single { SteamApiService(get()) }
}

val dataModule = module {
    single<ItemRepository> { ItemRepositoryImpl(get()) }
}

val domainModule = module {
    factory { GetItemsUseCase(get()) }
    factory { GetPriceHistoryUseCase(get()) }
    factory { GetTopDealsUseCase(get()) }
}

val sharedLogicModules = listOf(networkModule, dataModule, domainModule)