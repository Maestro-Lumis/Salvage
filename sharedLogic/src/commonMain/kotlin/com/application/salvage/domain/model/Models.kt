package app.salvage.domain.model

data class Item(
    val id: String,
    val name: String,
    val hashName: String,
    val game: Game,
    val type: ItemType,
    val imageUrl: String,
    val currentPrice: Double,
    val median30d: Double,
    val volume24h: Int,
    val dealScore: Double,
    val dealLevel: DealLevel,
    val wearLevel: WearLevel?,
    val floatValue: Double?,
    val patternIndex: Int?,
    val stickers: List<Sticker>,
    val rarity: String,
    val collection: String?,
    val isStatTrak: Boolean,
    val priceHistory: List<PricePoint>
)

data class Sticker(
    val name: String,
    val imageUrl: String,
    val slot: Int
)

data class PricePoint(
    val date: String,
    val price: Double,
    val volume: Int
)

enum class Game(val appId: Int, val label: String) {
    CS2(730, "CS2"),
    DOTA2(570, "Dota 2"),
    RUST(252490, "Rust"),
    TF2(440, "TF2")
}

enum class ItemType(val label: String) {
    KNIFE("Нож"),
    RIFLE("Винтовка"),
    PISTOL("Пистолет"),
    SNIPER("Снайперка"),
    SMG("SMG"),
    SHOTGUN("Дробовик"),
    HEAVY("Тяжёлое"),
    GLOVES("Перчатки"),
    STICKER("Стикер"),
    CASE("Кейс"),
    OTHER("Другое")
}

enum class WearLevel(
    val label: String,
    val shortLabel: String,
    val floatMin: Double,
    val floatMax: Double
) {
    FACTORY_NEW("Factory New",       "FN", 0.00, 0.07),
    MINIMAL_WEAR("Minimal Wear",     "MW", 0.07, 0.15),
    FIELD_TESTED("Field-Tested",     "FT", 0.15, 0.38),
    WELL_WORN("Well-Worn",           "WW", 0.38, 0.45),
    BATTLE_SCARRED("Battle-Scarred", "BS", 0.45, 1.00);

    companion object {
        fun fromFloat(float: Double): WearLevel =
            entries.first { float >= it.floatMin && float < it.floatMax }

        fun fromLabel(label: String): WearLevel? =
            entries.firstOrNull { it.label == label }
    }
}

enum class DealLevel(val label: String, val minScore: Double) {
    TREASURE("Клад",      25.0),
    GOOD("Выгодно",       10.0),
    NEUTRAL("Нейтрально", -5.0),
    BAD("Переплата",      Double.NEGATIVE_INFINITY)
}

fun dealLevelFromScore(score: Double): DealLevel =
    DealLevel.entries.first { score >= it.minScore }

fun calculateDealScore(currentPrice: Double, median30d: Double): Double {
    if (median30d <= 0.0) return 0.0
    return ((median30d - currentPrice) / median30d) * 100.0
}