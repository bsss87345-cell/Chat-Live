package com.example.model

enum class GameType(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val iconEmoji: String,
    val description: String,
    val playersCount: String,
    val tagColorHex: Long
) {
        DOMINO(
        id = "domino",
        titleAr = "دومينو",
        subtitleAr = "طابق النقاط وأغلق الطاولة",
        iconEmoji = "🀄",
        description = "اللعبة التكتيكية التقليدية. طابق أطراف قطع الدومينو وتخلص من جميع أحجارك قبل خصمك لتحقيق الفوز!",
        playersCount = "لاعبان",
        tagColorHex = 0xFF009688
    )
}

enum class GameMatchMode(val titleAr: String, val subtitleAr: String, val iconEmoji: String) {
    WITH_FRIEND("العب مع صديق", "دعوة صديق من المحادثة أو إنشاء رابط تحدي مباشر", "👥"),
    RANDOM_OPPONENT("ابحث عن خصم", "مطابقة سريعة مع لاعب متاح أو خوض جولة فورية", "⚡")
}
