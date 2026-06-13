package app.salvage.domain.usecase

import app.salvage.domain.model.Game
import app.salvage.domain.model.Item
import app.salvage.domain.model.PricePoint
import app.salvage.domain.repository.ItemFilter
import app.salvage.domain.repository.ItemRepository

class GetItemsUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(params: ItemFilter): Result<List<Item>> =
        repository.getItems(params)
}

class GetPriceHistoryUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(
        game: Game,
        hashName: String
    ): Result<List<PricePoint>> =
        repository.getPriceHistory(game, hashName)
}

class GetTopDealsUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(
        game: Game = Game.CS2,
        minScore: Double = 10.0,
        count: Int = 50
    ): Result<List<Item>> = repository.getItems(
        ItemFilter(game = game, minDealScore = minScore, count = count)
    ).map { items ->
        items.sortedByDescending { it.dealScore }
    }
}