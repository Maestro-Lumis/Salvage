package app.salvage.data.remote.api

import app.salvage.data.remote.dto.SteamPriceHistoryResponseDto
import app.salvage.data.remote.dto.SteamSearchResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class SteamApiService(private val httpClient: HttpClient) {

    companion object {
        private const val BASE_URL = "https://steamcommunity.com"
        const val IMAGE_BASE_URL =
            "https://community.akamai.steamstatic.com/economy/image/"
    }

    suspend fun searchItems(
        appId: Int,
        start: Int = 0,
        count: Int = 20,
        sortColumn: String = "popular",
        sortDir: String = "desc"
    ): SteamSearchResponseDto {
        return httpClient.get("$BASE_URL/market/search/render/") {
            parameter("appid", appId)
            parameter("start", start)
            parameter("count", count)
            parameter("sort_column", sortColumn)
            parameter("sort_dir", sortDir)
            parameter("norender", 1)
        }.body()
    }

    suspend fun getPriceHistory(
        appId: Int,
        marketHashName: String
    ): SteamPriceHistoryResponseDto {
        return httpClient.get("$BASE_URL/market/pricehistory/") {
            parameter("appid", appId)
            parameter("market_hash_name", marketHashName)
        }.body()
    }
}