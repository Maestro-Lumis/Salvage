package app.salvage.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SteamSearchResponseDto(
    val success: Boolean,
    val start: Int = 0,
    @SerialName("pagesize") val pageSize: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    val results: List<SteamItemDto> = emptyList()
)

@Serializable
data class SteamItemDto(
    val name: String,
    @SerialName("hash_name") val hashName: String,
    @SerialName("sell_listings") val sellListings: Int = 0,
    @SerialName("sell_price") val sellPrice: Int = 0,
    @SerialName("sell_price_text") val sellPriceText: String = "",
    @SerialName("asset_description") val assetDescription: SteamAssetDescriptionDto? = null
)

@Serializable
data class SteamAssetDescriptionDto(
    val appid: Int = 0,
    @SerialName("icon_url") val iconUrl: String = "",
    @SerialName("icon_url_large") val iconUrlLarge: String = "",
    val type: String = "",
    @SerialName("market_hash_name") val marketHashName: String = "",
    val tags: List<SteamTagDto> = emptyList()
)

@Serializable
data class SteamTagDto(
    val category: String = "",
    @SerialName("internal_name") val internalName: String = "",
    @SerialName("localized_category_name") val categoryName: String = "",
    @SerialName("localized_tag_name") val tagName: String = "",
    val color: String = ""
)

@Serializable
data class SteamPriceHistoryResponseDto(
    val success: Boolean,
    @SerialName("price_prefix") val pricePrefix: String = "",
    val prices: List<List<String>> = emptyList()
)