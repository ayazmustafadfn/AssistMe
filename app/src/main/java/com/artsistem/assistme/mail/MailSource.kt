package com.artsistem.assistme.mail

/** Sınıflandırma öncesi ham mail (Graph veya sahte kaynaktan gelir). */
data class RawMail(
    val id: String,
    val conversationId: String,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val preview: String,
    val receivedAtMillis: Long,
    val isRead: Boolean,
    val isFlagged: Boolean,
    val fromMe: Boolean,
    val lastMessageFromMe: Boolean
)

/** Mail kaynağı. Faz 1'de [FakeMailSource]; Azure gelince Graph implementasyonu. */
interface MailSource {
    suspend fun fetchRecent(): List<RawMail>
}

/**
 * Geliştirme için gerçekçi (Türkçe) sahte mailler. Azure/Graph hazır olunca
 * GraphMailSource ile değiştirilecek.
 */
class FakeMailSource : MailSource {

    override suspend fun fetchRecent(): List<RawMail> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val hour = 60L * 60 * 1000

        return listOf(
            RawMail("m1", "c1", "Adobe lisans yenileme — ödeme gerekiyor",
                "Adobe Faturalandırma", "billing@adobe.com",
                "Yıllık aboneliğinizin lisans yenilemesi için ödemenizi 5 gün içinde yapmanız gerekmektedir.",
                now - 6 * hour, isRead = false, isFlagged = true, fromMe = false, lastMessageFromMe = false),

            RawMail("m2", "c2", "Fatura #2026-0345",
                "Muhasebe", "muhasebe@tedarikci.com",
                "Mart ayı hizmet bedeli faturanız ektedir. Ödeme vadesi 15 Haziran. Tutar: 12.500 TL.",
                now - 1 * day, isRead = false, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m3", "c3", "Toplantı için uygun musunuz?",
                "Mehmet Yılmaz", "mehmet@musteri.com",
                "Merhaba, salı günü 14:00'te proje değerlendirme toplantısı yapabilir miyiz? Görüşünüzü bekliyorum.",
                now - 3 * hour, isRead = false, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m4", "c4", "Teklif hakkında",
                "Ayşe Demir", "ayse@partner.com",
                "Gönderdiğiniz teklifi inceledik, birkaç sorumuz var. Müsait olduğunuzda dönüş yapar mısınız?",
                now - 2 * day, isRead = true, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m5", "c5", "Proje teslimi — son tarih Cuma",
                "Proje Ofisi", "pmo@artsistem.com",
                "Hatırlatma: Faz 2 dokümantasyonunun teslimi için son tarih bu Cuma, en geç 17:00.",
                now - 5 * hour, isRead = true, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m6", "c6", "Re: Sözleşme taslağı",
                "Ben", "mustafa.ayaz@artsistem.com",
                "Taslağı gözden geçirmeniz için iletmiştim, geri dönüşünüzü rica ederim.",
                now - 5 * day, isRead = true, isFlagged = false, fromMe = true, lastMessageFromMe = true),

            RawMail("m7", "c7", "Sunucu bakımı bildirimi",
                "IT Destek", "it@artsistem.com",
                "Bu gece 02:00-04:00 arası planlı bakım yapılacaktır. Bilginize.",
                now - 8 * hour, isRead = true, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m8", "c8", "Haftalık teknoloji bülteni",
                "TechNews", "noreply@technews.com",
                "Bu haftanın öne çıkan haberleri ve makaleleri...",
                now - 1 * day - 2 * hour, isRead = true, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m9", "c9", "Microsoft 365 aboneliğiniz yakında yenilenecek",
                "Microsoft", "billing@microsoft.com",
                "Aboneliğinizin yenileme tarihi yaklaşıyor. Ödeme yönteminizi kontrol edin.",
                now - 12 * hour, isRead = false, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m10", "c10", "Re: Bütçe onayı",
                "Ben", "mustafa.ayaz@artsistem.com",
                "Bütçe kaleminin onayı için görüşünüzü bekliyorum, uygun olduğunuzda dönebilir misiniz?",
                now - 4 * day, isRead = true, isFlagged = false, fromMe = true, lastMessageFromMe = true),

            RawMail("m11", "c11", "Yeni eğitim programı duyurusu",
                "İK", "ik@artsistem.com",
                "Haziran ayı eğitim takvimi yayınlandı. Katılmak isteyenler kayıt olabilir.",
                now - 2 * day - 3 * hour, isRead = true, isFlagged = false, fromMe = false, lastMessageFromMe = false),

            RawMail("m12", "c12", "Acil: API erişim sorunu",
                "Canlı Destek", "destek@servis.com",
                "Sisteminizde tespit edilen sorun için en geç bugün dönüş yapmanızı rica ederiz.",
                now - 1 * hour, isRead = false, isFlagged = true, fromMe = false, lastMessageFromMe = false)
        )
    }
}
