package com.artsistem.assistme.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Alarm sesini (seçili melodi) DÖNGÜLÜ çalan ve titreşimi sürdüren tekil
 * oynatıcı. Alarmın TEK ses/titreşim kaynağıdır: bildirim kanalı sessizdir.
 * Böylece [mute] gerçekten her şeyi susturur ve "sadece titreşim" mümkün olur.
 *
 * Hatırlatma tetiklenince [Notifications.show] içinden başlatılır (alarm ekranı
 * açılmasa, yalnızca heads-up düşse de çalar). Aksiyonlar / zaman aşımı [stop] çağırır.
 */
object AlarmPlayer {

    private const val TAG = "AlarmPlayer"

    /** Kimse dokunmazsa bu süre sonunda kendiliğinden sus. */
    private const val AUTO_STOP_MS = 2 * 60 * 1000L

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var active = false
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }

    /**
     * @param vibrateOnly true ise ses çalınmaz, yalnızca titreşir.
     */
    @Synchronized
    fun start(context: Context, vibrateOnly: Boolean = false) {
        // Zaten çalıyorsa (ya da sessize alınmışsa) tekrar başlatma.
        if (active) return
        active = true

        val appContext = context.applicationContext
        if (!vibrateOnly) startSound(appContext)
        startVibration(appContext)
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(autoStop)
        releaseSound()
        cancelVibration()
        active = false
    }

    fun isPlaying(): Boolean = active

    /**
     * Sesi ve titreşimi tamamen keser ama alarmı KAPATMAZ: ekran açık kalır,
     * kullanıcı yine Tamamlandı / Ertele seçer. Tekrar [start] çağrısı sesi
     * başlatmaz ([stop] gelene kadar).
     */
    @Synchronized
    fun mute() {
        releaseSound()
        cancelVibration()
    }

    private fun releaseSound() {
        try {
            mediaPlayer?.apply {
                runCatching { if (isPlaying) stop() }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ses durdurulamadı", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun cancelVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Titreşim durdurulamadı", e)
        } finally {
            vibrator = null
        }
    }

    private fun startSound(context: Context) {
        // Kullanıcının seçtiği melodi; yoksa sistem alarm sesi (Settings içinde).
        val uri = Settings.getAlarmSoundUri(context) ?: return
        if (startSoundFrom(context, uri)) return
        // Seçili melodi silinmiş/erişilemiyorsa sistem alarm sesine düş.
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (fallback != null && fallback != uri) {
            Log.w(TAG, "Seçili melodi açılamadı, varsayılana dönülüyor")
            startSoundFrom(context, fallback)
        }
    }

    private fun startSoundFrom(context: Context, uri: Uri): Boolean {
        return try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setDataSource(context, uri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            // Hazırlanırken sessize alındıysa (oynatıcı bırakıldıysa) başlatma.
            player.setOnPreparedListener { p -> synchronized(this) { if (mediaPlayer === p) p.start() } }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Alarm sesi başlatılamadı: $uri", e)
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
            false
        }
    }

    private fun startVibration(context: Context) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vib == null || !vib.hasVibrator()) return
        vibrator = vib

        // 1 sn titre, 1 sn dur — sürekli tekrar (index 0'dan).
        val pattern = longArrayOf(0, 1000, 1000)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Titreşim başlatılamadı", e)
        }
    }
}
