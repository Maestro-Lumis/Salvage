package app.salvage.presentation.viewmodel

import app.salvage.domain.model.DealLevel
import app.salvage.domain.model.Game
import app.salvage.domain.model.Item
import app.salvage.domain.model.ItemType
import app.salvage.domain.repository.ItemFilter
import app.salvage.domain.usecase.GetItemsUseCase
import app.salvage.domain.usecase.GetPriceHistoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemListState(
    val items: List<Item> = emptyList(),
    val selectedItem: Item? = null,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val error: String? = null,
    val selectedGame: Game = Game.CS2,
    val selectedType: ItemType? = null,
    val selectedDealLevel: DealLevel? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null
)

class ItemListViewModel(
    private val getItemsUseCase: GetItemsUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(ItemListState())
    val state: StateFlow<ItemListState> = _state.asStateFlow()

    init { loadItems() }

    fun loadItems() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getItemsUseCase(buildFilter())
                .onSuccess { items ->
                    _state.update { it.copy(items = items, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Ошибка")
                    }
                }
        }
    }

    fun selectItem(item: Item) {
        _state.update { it.copy(selectedItem = item) }
        if (item.priceHistory.isEmpty()) loadPriceHistory(item)
    }

    fun clearSelection() = _state.update { it.copy(selectedItem = null) }

    fun selectGame(game: Game) {
        _state.update { it.copy(selectedGame = game) }
        loadItems()
    }

    fun selectType(type: ItemType?) {
        _state.update { it.copy(selectedType = type) }
        loadItems()
    }

    fun selectDealLevel(level: DealLevel?) {
        _state.update { it.copy(selectedDealLevel = level) }
        loadItems()
    }

    fun setPriceRange(min: Double?, max: Double?) {
        _state.update { it.copy(minPrice = min, maxPrice = max) }
        loadItems()
    }

    private fun loadPriceHistory(item: Item) {
        scope.launch {
            _state.update { it.copy(isLoadingDetail = true) }
            getPriceHistoryUseCase(item.game, item.hashName)
                .onSuccess { history ->
                    val updated = item.copy(priceHistory = history)
                    _state.update { s ->
                        s.copy(
                            isLoadingDetail = false,
                            selectedItem = updated,
                            items = s.items.map { if (it.id == updated.id) updated else it }
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingDetail = false) }
                }
        }
    }

    private fun buildFilter() = _state.value.let { s ->
        ItemFilter(
            game = s.selectedGame,
            itemType = s.selectedType,
            minPrice = s.minPrice,
            maxPrice = s.maxPrice,
            minDealScore = s.selectedDealLevel?.minScore
        )
    }
}