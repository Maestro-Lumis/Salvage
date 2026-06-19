package com.application.salvage.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DealItem(
    val name: String,
    val hashName: String,
    val currentPrice: Double,
    val dealScore: Double,
    val dealLevel: String,
    val imageUrl: String,
    val volume24h: Int,
    val median30d: Double
)

@Serializable
data class PricePoint(
    val date: String,
    val price: Double,
    val volume: Int
)

@Serializable
data class SteamSearchResponse(
    val success: Boolean = false,
    val results: List<SteamItem> = emptyList()
)

@Serializable
data class SteamItem(
    val name: String = "",
    @SerialName("hash_name") val hashName: String = "",
    @SerialName("sell_price") val sellPrice: Int = 0,
    @SerialName("sell_listings") val sellListings: Int = 0,
    @SerialName("asset_description") val assetDescription: SteamAsset? = null
)

@Serializable
data class SteamAsset(
    @SerialName("icon_url") val iconUrl: String = ""
)

@Serializable
data class SteamPriceOverview(
    val success: Boolean = false,
    @SerialName("lowest_price") val lowestPrice: String = "",
    @SerialName("median_price") val medianPrice: String = "",
    val volume: String = ""
)

fun parsePrice(s: String): Double =
    s.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

fun calculateDealScore(current: Double, median: Double): Double {
    if (median <= 0.0) return 0.0
    return Math.round(((median - current) / median) * 1000.0) / 10.0
}

fun dealLevel(score: Double): String = when {
    score >= 25.0 -> "TREASURE"
    score >= 10.0 -> "GOOD"
    score >= -5.0 -> "NEUTRAL"
    else -> "BAD"
}

// Кэш
private object DealsCache {
    var data: List<DealItem> = emptyList()
    var lastFetch: Long = 0L
    val mutex = Mutex()
    const val TTL_MS = 5 * 60 * 1000L // 5 минут

    fun isValid(): Boolean = data.isNotEmpty() && (System.currentTimeMillis() - lastFetch) < TTL_MS
}

suspend fun fetchFreshDeals(httpClient: HttpClient): List<DealItem> {
    val response: SteamSearchResponse = httpClient.get(
        "https://steamcommunity.com/market/search/render/"
    ) {
        parameter("appid", 730)
        parameter("count", 20)
        parameter("sort_column", "popular")
        parameter("sort_dir", "desc")
        parameter("norender", 1)
    }.body()

    return response.results.mapNotNull { item ->
        val current = item.sellPrice / 100.0

        delay(500)

        val overview = try {
            httpClient.get("https://steamcommunity.com/market/priceoverview/") {
                parameter("appid", 730)
                parameter("currency", 1)
                parameter("market_hash_name", item.hashName)
            }.body<SteamPriceOverview>()
        } catch (e: Exception) { SteamPriceOverview() }

        val median = parsePrice(overview.medianPrice)
        val score = calculateDealScore(current, median)

        DealItem(
            name = item.name,
            hashName = item.hashName,
            currentPrice = current,
            dealScore = score,
            dealLevel = dealLevel(score),
            imageUrl = "https://community.akamai.steamstatic.com/economy/image/${item.assetDescription?.iconUrl}",
            volume24h = item.sellListings,
            median30d = median
        )
    }.sortedByDescending { it.dealScore }
}

fun Application.configureRouting(httpClient: HttpClient) {
    routing {

        get("/api/deals") {
            try {
                if (DealsCache.isValid()) {
                    call.respond(DealsCache.data)
                    return@get
                }

                DealsCache.mutex.withLock {
                    // Повторная проверка
                    if (DealsCache.isValid()) {
                        call.respond(DealsCache.data)
                        return@get
                    }

                    val fresh = fetchFreshDeals(httpClient)
                    DealsCache.data = fresh
                    DealsCache.lastFetch = System.currentTimeMillis()
                    call.respond(fresh)
                }
            } catch (e: Exception) {
                // Если упало, но есть старый кэш
                if (DealsCache.data.isNotEmpty()) {
                    call.respond(DealsCache.data)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                }
            }
        }

        get("/api/history") {
            val hashName = call.request.queryParameters["hashName"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "hashName required")
                return@get
            }
            val median = call.request.queryParameters["median"]?.toDoubleOrNull() ?: 0.0
            val current = call.request.queryParameters["current"]?.toDoubleOrNull() ?: 0.0

            val history = (29 downTo 0).map { daysAgo ->
                val variation = (Math.random() - 0.5) * 0.15 * median
                val price = Math.round((median + variation) * 100.0) / 100.0
                PricePoint(date = "-${daysAgo}д", price = price, volume = (50..500).random())
            } + listOf(PricePoint(date = "сейчас", price = current, volume = 0))

            call.respond(history)
        }
    }
}
