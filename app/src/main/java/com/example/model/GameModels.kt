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
    LUDO(
        id = "ludo",
        titleAr = "لودو",
        subtitleAr = "لعبة الحظ والاستراتيجية الكلاسيكية",
        iconEmoji = "🎲",
        description = "حرك أحجارك عبر المسار بعد رمي النرد، أسقط أحجار الخصم وكن أول من يصل بجميع أحجاره لمنطقة الأمان!",
        playersCount = "2 - 4 لاعبين",
        tagColorHex = 0xFFE91E63
    ),
    JACKAROO(
        id = "jackaroo",
        titleAr = "جاكارو",
        subtitleAr = "تحدي الأوراق والأحجار الحماسي",
        iconEmoji = "🃏",
        description = "لعبة الكروت والأحجار الجماعية الشهيرة. استخدم بطاقات اللعب المتميزة كـ (الشايب والقص والـ 4 والـ 7) لتصل بأحجارك أولاً!",
        playersCount = "2 - 4 لاعبين",
        tagColorHex = 0xFF7C4DFF
    ),
    DOMINO(
        id = "domino",
        titleAr = "دومينو",
        subtitleAr = "طابق النقاط وأغلق الطاولة",
        iconEmoji = "🀄",
        description = "اللعبة التكتيكية التقليدية. طابق أطراف قطع الدومينو وتخلص من جميع أحجارك قبل خصمك لتحقيق الفوز!",
        playersCount = "لاعبان",
        tagColorHex = 0xFF009688
    ),
    SNAKES_AND_LADDERS(
        id = "snakes_ladders",
        titleAr = "لعبة السلم",
        subtitleAr = "تسلق السلالم وتفادَ لدغات الثعابين",
        iconEmoji = "🪜",
        description = "تسابق للوصول للمربع النهائي! تسلق السلالم لتختصر الطريق وتجنب رؤوس الثعابين لئلا تهوي إلى الأسفل!",
        playersCount = "لاعبان",
        tagColorHex = 0xFFFF9800
    ),
    CHESS(
        id = "chess",
        titleAr = "الشطرنج",
        subtitleAr = "صراع العقول والملوك",
        iconEmoji = "♟️",
        description = "لعبة الملوك والاستراتيجية الخالدة. خطط لنقلاتك بدقة، احمِ ملكك وانصب فخ كش ملك لإسقاط خصمك!",
        playersCount = "لاعبان",
        tagColorHex = 0xFF3F51B5
    )
}

enum class GameMatchMode(val titleAr: String, val subtitleAr: String, val iconEmoji: String) {
    WITH_FRIEND("العب مع صديق", "دعوة صديق من المحادثة أو إنشاء رابط تحدي مباشر", "👥"),
    RANDOM_OPPONENT("ابحث عن خصم", "مطابقة سريعة مع لاعب متاح أو خوض جولة فورية", "⚡")
}
