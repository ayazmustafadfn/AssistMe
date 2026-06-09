package com.artsistem.assistme.mail.graph

import com.artsistem.assistme.mail.MailSource
import com.artsistem.assistme.mail.RawMail
import com.artsistem.assistme.mail.auth.MsalAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Microsoft Graph üzerinden gerçek mailleri çeker. MSAL'den token alır,
 * /me/messages uç noktasını çağırır ve [RawMail] listesine dönüştürür.
 *
 * Sadece metaveri + bodyPreview çekilir (gizlilik: gövde tam metni alınmaz).
 */
class GraphMailSource(private val auth: MsalAuth) : MailSource {

    override suspend fun fetchRecent(): List<RawMail> = withContext(Dispatchers.IO) {
        val result = auth.acquireTokenSilent()
            ?: throw IllegalStateException("Microsoft hesabına giriş yapılmamış")
        val token = result.accessToken
        val me = result.account?.username?.lowercase().orEmpty()

        val url = URL(
            "https://graph.microsoft.com/v1.0/me/messages" +
                "?\$top=50" +
                "&\$select=id,conversationId,subject,bodyPreview,from,receivedDateTime,isRead,flag" +
                "&\$orderby=receivedDateTime%20desc"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 20000
            readTimeout = 20000
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Graph hatası ($code): ${err.take(300)}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parse(body, me)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String, me: String): List<RawMail> {
        val arr = JSONObject(body).optJSONArray("value") ?: return emptyList()
        val out = ArrayList<RawMail>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val emailAddr = o.optJSONObject("from")?.optJSONObject("emailAddress")
            val fromAddress = emailAddr?.optString("address").orEmpty()
            val fromName = emailAddr?.optString("name").orEmpty().ifBlank { fromAddress }
            val fromMe = fromAddress.isNotBlank() && fromAddress.equals(me, ignoreCase = true)
            val received = o.optString("receivedDateTime", "")
            val receivedMillis = runCatching { Instant.parse(received).toEpochMilli() }
                .getOrDefault(System.currentTimeMillis())
            val flagStatus = o.optJSONObject("flag")?.optString("flagStatus", "notFlagged")
            out.add(
                RawMail(
                    id = o.getString("id"),
                    conversationId = o.optString("conversationId", ""),
                    subject = o.optString("subject", "(konu yok)"),
                    fromName = fromName,
                    fromAddress = fromAddress,
                    preview = o.optString("bodyPreview", ""),
                    receivedAtMillis = receivedMillis,
                    isRead = o.optBoolean("isRead", true),
                    isFlagged = flagStatus == "flagged",
                    fromMe = fromMe,
                    // MVP yaklaşımı: kendi gönderdiğin mailde "son söz sende" varsay.
                    lastMessageFromMe = fromMe
                )
            )
        }
        return out
    }
}
