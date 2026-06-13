package app.salvage.domain.repository

import app.salvage.domain.model.Game
import app.salvage.domain.model.Item
import app.salvage.domain.model.ItemType
import app.salvage.domain.model.PricePoint

interface ItemRepository {
    suspend fun getItems(params: ItemFilter): Result<List<Item>>
    suspend fun getPriceHistory(game: Game, hashName: String): Result<List<PricePoint>>
}

data class ItemFilter(
    val game: Game = Game.CS2,
    val itemType: ItemType? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minDealScore: Double? = null,
    val start: Int = 0,
    val count: Int = 20
)