package com.artsistem.assistme.mail

/**
 * Yerel (cihaz içi) kural-tabanlı mail sınıflandırıcı. Hiçbir veri dışarı çıkmaz.
 * Faz 2'de LLM ile isabeti artırılacak.
 *
 * Sınıflandırma için gereken alanları sade bir girdi olarak alır; böylece saf
 * Kotlin olarak test edilebilir (Android bağımlılığı yok).
 */
object MailClassifier {

    data class Input(
        val subject: String,
        val preview: String,
        val fromMe: Boolean,
        val isRead: Boolean,
        val ageDays: Int,
        /** Konuşmadaki son mesaj kullanıcıdan mı (yanıt verilmiş mi). */
        val lastMessageFromMe: Boolean
    )

    data class Result(val category: MailCategory, val reason: String)

    private val criticalWords = listOf(
        "lisans", "license", "fatura", "invoice", "ödeme", "odeme", "payment",
        "vade", "son ödeme", "son odeme", "due", "yenileme", "renewal",
        "abonelik", "subscription", "tahsilat", "dekont", "ödenmesi", "tutar"
    )
    private val deadlineWords = listOf(
        "deadline", "son tarih", "teslim", "kadar", "termin", "bitiş tarihi",
        "en geç", "en gec", "gün içinde", "gun icinde", "acil", "ivedi"
    )
    private val replyCues = listOf(
        "?", "rica", "lütfen", "lutfen", "görüş", "gorus", "onay", "bekliyoruz",
        "bekliyorum", "geri dönüş", "geri donus", "dönüş yap", "teyit", "yanıt",
        "yanit", "bilgi verir misiniz", "iletir misiniz", "ne zaman"
    )

    fun classify(input: Input): Result {
        val text = (input.subject + " " + input.preview).lowercase()

        // Öncelik: Kritik > Son tarih > Yanıt bekleyen > Takip > Diğer
        criticalWords.firstOrNull { text.contains(it) }?.let {
            return Result(MailCategory.CRITICAL, "Kritik içerik: \"$it\"")
        }

        deadlineWords.firstOrNull { text.contains(it) }?.let {
            return Result(MailCategory.DEADLINE, "Son tarih ifadesi: \"$it\"")
        }

        // Takip: senin gönderdiğin, yanıt gelmemiş, eskimiş.
        if (input.fromMe && input.lastMessageFromMe && input.ageDays >= 3) {
            return Result(MailCategory.FOLLOW_UP, "Gönderdin, ${input.ageDays} gündür yanıt yok")
        }

        // Yanıt bekleyen: sana gelmiş, son söz sende değil ve bir yanıt iması var.
        if (!input.fromMe && !input.lastMessageFromMe) {
            val cue = replyCues.firstOrNull { text.contains(it) }
            if (cue != null || !input.isRead) {
                val reason = cue?.let { "Yanıt bekliyor: \"$it\"" } ?: "Okunmadı, yanıt bekliyor"
                return Result(MailCategory.NEEDS_REPLY, reason)
            }
        }

        return Result(MailCategory.OTHER, "")
    }
}
