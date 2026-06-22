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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DealItem(
    val name: String,
    val hashName: String,
    val type: String,
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

private val KNIVES = listOf("Karambit", "Bayonet", "M9", "Butterfly", "Falchion", "Flip", "Gut", "Huntsman", "Shadow Daggers", "Bowie", "Talon", "Ursus", "Navaja", "Stiletto", "Nomad", "Paracord", "Survival", "Skeleton", "Classic Knife", "Daggers")
private val GLOVES = listOf("Gloves", "Wraps")
private val RIFLES = listOf("AK-47", "M4A4", "M4A1", "FAMAS", "Galil", "SG 553", "AUG")
private val SNIPERS = listOf("AWP", "SSG 08", "SCAR-20", "G3SG1")
private val SMGS = listOf("MP9", "MAC-10", "UMP-45", "P90", "PP-Bizon", "MP7", "MP5-SD")
private val PISTOLS = listOf("Glock-18", "USP-S", "P250", "Desert Eagle", "Five-SeveN", "Tec-9", "CZ75", "Dual Berettas", "R8 Revolver", "P2000")
private val SHOTGUNS = listOf("Nova", "XM1014", "MAG-7", "Sawed-Off")
private val HEAVY = listOf("Negev", "M249")
private val CASES = listOf("Case")
private val CAPSULES = listOf("Capsule")
private val STICKERS = listOf("Sticker")
private val MUSIC = listOf("Music Kit")
private val AGENTS = listOf("Agent")

fun parseItemType(name: String): String = when {
    KNIVES.any { name.contains(it) } -> "KNIFE"
    GLOVES.any { name.contains(it) } -> "GLOVES"
    RIFLES.any { name.contains(it) } -> "RIFLE"
    SNIPERS.any { name.contains(it) } -> "SNIPER"
    SMGS.any { name.contains(it) } -> "SMG"
    PISTOLS.any { name.contains(it) } -> "PISTOL"
    SHOTGUNS.any { name.contains(it) } -> "SHOTGUN"
    HEAVY.any { name.contains(it) } -> "HEAVY"
    CASES.any { name.contains(it) } -> "CASE"
    CAPSULES.any { name.contains(it) } -> "CAPSULE"
    STICKERS.any { name.contains(it) } -> "STICKER"
    MUSIC.any { name.contains(it) } -> "MUSIC_KIT"
    AGENTS.any { name.contains(it) } -> "AGENT"
    else -> "OTHER"
}

// Кэш с фоновым обновлением
private object DealsCache {
    @Volatile var data: List<DealItem> = emptyList()
    @Volatile var lastFetch: Long = 0L
    const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L
}

private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

suspend fun fetchFreshDeals(httpClient: HttpClient): List<DealItem> {
    val allItems = mutableListOf<SteamItem>()
    for (start in listOf(0, 10, 20, 30, 40, 50)) {
        val response: SteamSearchResponse = httpClient.get(
            "https://steamcommunity.com/market/search/render/"
        ) {
            parameter("query", "")
            parameter("start", start)
            parameter("count", 10)
            parameter("search_descriptions", 0)
            parameter("sort_column", "popular")
            parameter("sort_dir", "desc")
            parameter("appid", 730)
            parameter("norender", 1)
        }.body()

        allItems += response.results
        delay(300)
    }

    return allItems.mapNotNull { item ->
        val current = item.sellPrice / 100.0

        delay(400)

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
            type = parseItemType(item.name),
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

    // Фоновая задача
    backgroundScope.launch {
        while (true) {
            try {
                val fresh = fetchFreshDeals(httpClient)
                DealsCache.data = fresh
                DealsCache.lastFetch = System.currentTimeMillis()
            } catch (e: Exception) {
            }
            delay(DealsCache.REFRESH_INTERVAL_MS)
        }
    }

    routing {

        get("/api/deals") {
            if (DealsCache.data.isNotEmpty()) {
                call.respond(DealsCache.data)
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    "Данные загружаются, обновите страницу через 30 секунд"
                )
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
