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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DealItem(
    val name: String,
    val hashName: String,
    val currentPrice: Double,
    val dealScore: Double,
    val imageUrl: String,
    val volume24h: Int
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

fun Application.configureRouting(httpClient: HttpClient) {
    routing {
        get("/api/deals") {
            try {
                val response: SteamSearchResponse = httpClient.get(
                    "https://steamcommunity.com/market/search/render/"
                ) {
                    parameter("appid", 730)
                    parameter("count", 50)
                    parameter("sort_column", "popular")
                    parameter("sort_dir", "desc")
                    parameter("norender", 1)
                }.body()

                val deals = response.results.mapNotNull { item ->
                    val price = item.sellPrice / 100.0
                    DealItem(
                        name = item.name,
                        hashName = item.hashName,
                        currentPrice = price,
                        dealScore = 0.0,
                        imageUrl = "https://community.akamai.steamstatic.com/economy/image/${item.assetDescription?.iconUrl}",
                        volume24h = item.sellListings
                    )
                }

                call.respond(deals)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }
    }
}
