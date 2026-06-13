package app.salvage.data.repository

import app.salvage.data.mapper.toDomain
import app.salvage.data.mapper.toPricePoints
import app.salvage.data.remote.api.SteamApiService
import app.salvage.domain.model.Game
import app.salvage.domain.model.Item
import app.salvage.domain.model.PricePoint
import app.salvage.domain.repository.ItemFilter
import app.salvage.domain.repository.ItemRepository

class ItemRepositoryImpl(
    private val api: SteamApiService
) : ItemRepository {

    override suspend fun getItems(params: ItemFilter): Result<List<Item>> =
        runCatching {
            api.searchItems(
                appId = params.game.appId,
                start = params.start,
                count = params.count
            ).results
                .map { it.toDomain(params.game) }
                .filter { item ->
                    (params.itemType == null     || item.type == params.itemType) &&
                            (params.minPrice == null      || item.currentPrice >= params.minPrice) &&
                            (params.maxPrice == null      || item.currentPrice <= params.maxPrice) &&
                            (params.minDealScore == null  || item.dealScore >= params.minDealScore)
                }
        }

    override suspend fun getPriceHistory(
        game: Game,
        hashName: String
    ): Result<List<PricePoint>> =
        runCatching {
            api.getPriceHistory(game.appId, hashName).toPricePoints()
        }
}