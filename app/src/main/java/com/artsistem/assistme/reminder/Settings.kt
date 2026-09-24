package com.artsistem.assistme.reminder

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

/**
 * Basit uygulama ayarları (şimdilik sadece varsayılan erteleme süresi).
 * Broadcast alıcılarında senkron okunabilsin diye SharedPreferences kullanır.
 */
object Settings {

    private const val PREFS = "assistme_prefs"
    private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
    private const val KEY_LAST_CUSTOM_SNOOZE = "last_custom_snooze_minutes"
    const val DEFAULT_SNOOZE_MINUTES = 10

    /** Sabit hızlı erteleme süreleri (dakika). */
    const val SNOOZE_SHORT_MINUTES = 15
    const val SNOOZE_LONG_MINUTES = 60

    fun getSnoozeMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE_MINUTES, minutes.coerceAtLeast(1)).apply()
    }

    /** Kullanıcının "Diğer…" ile seçtiği son özel erteleme süresi (0 = yok). */
    fun getLastCustomSnoozeMinutes(context: Context): Int =
        prefs(context).getInt(KEY_LAST_CUSTOM_SNOOZE, 0)

    fun setLastCustomSnoozeMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_LAST_CUSTOM_SNOOZE, minutes.coerceAtLeast(1)).apply()
    }

    // --- Alarm sesi ---
    private const val KEY_ALARM_SOUND = "alarm_sound_uri"
    private const val KEY_ALARM_CHANNEL_VERSION = "alarm_channel_version"
    private const val KEY_PICKUP_MUTE = "pickup_mute"

    /** Seçili alarm melodisi; seçilmemişse sistemin varsayılan alarm sesi. */
    fun getAlarmSoundUri(context: Context): Uri? {
        val saved = prefs(context).getString(KEY_ALARM_SOUND, null)
        if (!saved.isNullOrEmpty()) return Uri.parse(saved)
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    /**
     * Melodiyi değiştirir. Android bildirim kanalının sesi oluşturulduktan sonra
     * değiştirilemediği için kanal sürümü artırılır (yeni kanal = yeni ses).
     */
    fun setAlarmSoundUri(context: Context, uri: Uri?) {
        prefs(context).edit()
            .putString(KEY_ALARM_SOUND, uri?.toString())
            .putInt(KEY_ALARM_CHANNEL_VERSION, getAlarmChannelVersion(context) + 1)
            .apply()
    }

    fun getAlarmChannelVersion(context: Context): Int =
        prefs(context).getInt(KEY_ALARM_CHANNEL_VERSION, 0)

    /** Alarm çalarken telefon ele alınınca ses kısılsın mı (ekran açık kalır). */
    fun isPickupMuteEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PICKUP_MUTE, true)

    fun setPickupMuteEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PICKUP_MUTE, enabled).apply()
    }

    // --- Ana ekran kutu sırası ---
    private const val KEY_HOME_ORDER = "home_module_order"

    /** Kaydedilmiş kutu sırası (modül anahtarları); kayıt yoksa boş liste. */
    fun getHomeOrder(context: Context): List<String> =
        prefs(context).getString(KEY_HOME_ORDER, null)
            ?.split(',')?.filter { it.isNotBlank() }.orEmpty()

    fun setHomeOrder(context: Context, keys: List<String>) {
        prefs(context).edit().putString(KEY_HOME_ORDER, keys.joinToString(",")).apply()
    }

    // --- Mail asistanı (Faz 1: sahte bağlantı durumu) ---
    private const val KEY_MAIL_CONNECTED = "mail_connected"

    fun isMailConnected(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MAIL_CONNECTED, false)

    fun setMailConnected(context: Context, connected: Boolean) {
        prefs(context).edit().putBoolean(KEY_MAIL_CONNECTED, connected).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
