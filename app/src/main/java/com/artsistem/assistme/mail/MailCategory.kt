package com.artsistem.assistme.mail

/** Mail asistanı kategorileri. */
enum class MailCategory(val label: String) {
    NEEDS_REPLY("Yanıt bekleyen"),
    CRITICAL("Kritik"),
    DEADLINE("Son tarih"),
    FOLLOW_UP("Takip"),
    OTHER("Diğer")
}
