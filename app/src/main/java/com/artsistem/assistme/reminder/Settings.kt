package com.artsistem.assistme.reminder

import android.content.Context

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
