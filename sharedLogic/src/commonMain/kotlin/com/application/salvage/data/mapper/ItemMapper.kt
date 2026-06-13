package app.salvage.data.mapper

import app.salvage.data.remote.api.SteamApiService
import app.salvage.data.remote.dto.SteamItemDto
import app.salvage.data.remote.dto.SteamPriceHistoryResponseDto
import app.salvage.domain.model.*

fun SteamItemDto.toDomain(
    game: Game,
    priceHistory: List<PricePoint> = emptyList()
): Item {
    val currentPrice = sellPrice / 100.0
    val median30d = priceHistory.median30d() ?: currentPrice
    val score = calculateDealScore(currentPrice, median30d)
    val tags = assetDescription?.tags ?: emptyList()

    return Item(
        id = "${game.appId}_$hashName",
        name = name,
        hashName = hashName,
        game = game,
        type = parseItemType(
            tags.firstOrNull { it.category == "Type" }?.tagName
                ?: assetDescription?.type ?: ""
        ),
        imageUrl = buildImageUrl(
            assetDescription?.iconUrlLarge ?: assetDescription?.iconUrl ?: ""
        ),
        currentPrice = currentPrice,
        median30d = median30d,
        volume24h = sellListings,
        dealScore = score,
        dealLevel = dealLevelFromScore(score),
        wearLevel = WearLevel.fromLabel(parseWearFromName(name)),
        floatValue = null,
        patternIndex = null,
        stickers = emptyList(),
        rarity = tags.firstOrNull { it.category == "Rarity" }?.tagName ?: "",
        collection = tags.firstOrNull { it.category == "ItemSet" }?.tagName,
        isStatTrak = name.startsWith("StatTrak™"),
        priceHistory = priceHistory
    )
}

fun SteamPriceHistoryResponseDto.toPricePoints(): List<PricePoint> =
    prices.mapNotNull { row ->
        if (row.size < 3) return@mapNotNull null
        PricePoint(
            date = row[0].take(11).trim(),
            price = row[1].toDoubleOrNull() ?: 0.0,
            volume = row[2].toIntOrNull() ?: 0
        )
    }

private fun List<PricePoint>.median30d(): Double? {
    val sorted = takeLast(30).map { it.price }.sorted()
    if (sorted.isEmpty()) return null
    return if (sorted.size % 2 == 0)
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    else
        sorted[sorted.size / 2]
}

private fun buildImageUrl(iconUrl: String): String =
    if (iconUrl.isBlank()) "" else "${SteamApiService.IMAGE_BASE_URL}$iconUrl"

private fun parseWearFromName(name: String): String =
    Regex("\\((Factory New|Minimal Wear|Field-Tested|Well-Worn|Battle-Scarred)\\)")
        .find(name)?.groupValues?.get(1) ?: ""

private fun parseItemType(raw: String): ItemType = when {
    raw.contains("Knife", true) || raw.contains("Karambit", true) -> ItemType.KNIFE
    raw.contains("Rifle", true)   -> ItemType.RIFLE
    raw.contains("Pistol", true)  -> ItemType.PISTOL
    raw.contains("Sniper", true)  -> ItemType.SNIPER
    raw.contains("SMG", true)     -> ItemType.SMG
    raw.contains("Shotgun", true) -> ItemType.SHOTGUN
    raw.contains("Gloves", true)  -> ItemType.GLOVES
    raw.contains("Sticker", true) -> ItemType.STICKER
    raw.contains("Case", true)    -> ItemType.CASE
    else                           -> ItemType.OTHER
}