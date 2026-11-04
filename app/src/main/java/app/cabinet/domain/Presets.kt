package app.cabinet.domain

/** Ready halls to start from: a name, the first cabinets and the card fields that kind of collection needs. */
data class Preset(val key: String, val ru: Names, val en: Names) {
    data class Names(val hall: String, val cabinets: List<String>, val fields: List<String>)
    fun names(ru: Boolean) = if (ru) this.ru else en
}

object Presets {
    val ALL = listOf(
        Preset("cans",
            Preset.Names("Банки и бутылки", listOf("Пиво", "Лимонады", "Чай и кофе", "Редкости"), listOf("Бренд", "Страна", "Объём", "Серия", "Год")),
            Preset.Names("Cans & bottles", listOf("Beer", "Soda", "Tea & coffee", "Rarities"), listOf("Brand", "Country", "Volume", "Series", "Year"))),
        Preset("figures",
            Preset.Names("Фигурки", listOf("Керамика", "Игрушки", "Брелоки", "Дерево"), listOf("Материал", "Размер", "Мастер или фабрика")),
            Preset.Names("Figurines", listOf("Ceramics", "Toys", "Keyrings", "Wood"), listOf("Material", "Size", "Maker"))),
        Preset("magnets",
            Preset.Names("Магниты", listOf("Города", "Страны", "Подарки"), listOf("Город", "Страна", "Материал")),
            Preset.Names("Magnets", listOf("Cities", "Countries", "Gifts"), listOf("City", "Country", "Material"))),
        Preset("coins",
            Preset.Names("Монеты и банкноты", listOf("Монеты", "Банкноты"), listOf("Страна", "Год", "Номинал", "Металл")),
            Preset.Names("Coins & notes", listOf("Coins", "Banknotes"), listOf("Country", "Year", "Denomination", "Metal"))),
        Preset("paper",
            Preset.Names("Марки и открытки", listOf("Марки", "Открытки", "Билеты"), listOf("Страна", "Год", "Серия")),
            Preset.Names("Stamps & postcards", listOf("Stamps", "Postcards", "Tickets"), listOf("Country", "Year", "Series"))),
        Preset("records",
            Preset.Names("Пластинки и кассеты", listOf("Винил", "Кассеты", "Диски"), listOf("Исполнитель", "Год", "Лейбл")),
            Preset.Names("Records & tapes", listOf("Vinyl", "Tapes", "CDs"), listOf("Artist", "Year", "Label"))),
        Preset("cars",
            Preset.Names("Модели машин", listOf("Легковые", "Грузовики", "Гоночные"), listOf("Марка", "Модель", "Масштаб", "Производитель")),
            Preset.Names("Model cars", listOf("Cars", "Trucks", "Racing"), listOf("Make", "Model", "Scale", "Maker"))),
        Preset("own",
            Preset.Names("Моя коллекция", listOf("Главный шкаф"), emptyList()),
            Preset.Names("My collection", listOf("Main cabinet"), emptyList())),
    )
}
