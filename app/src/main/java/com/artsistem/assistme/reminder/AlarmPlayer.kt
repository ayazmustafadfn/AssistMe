package com.artsistem.assistme.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Alarm sesini (sistem alarm zil sesi) DÖNGÜLÜ çalan ve titreşimi sürdüren
 * tekil oynatıcı. Alarm ekranı açılınca [start], "Ertele/Tamam" ya da zaman
 * aşımında [stop] çağrılır.
 *
 * Ses, bildirim kanalından bağımsız olarak ALARM ses akışında (USAGE_ALARM)
 * çalar; böylece telefon sessizde olsa bile alarm sesi duyulur.
 */
object AlarmPlayer {

    private const val TAG = "AlarmPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun start(context: Context) {
        // Zaten çalıyorsa tekrar başlatma.
        if (mediaPlayer != null) return

        val appContext = context.applicationContext
        startSound(appContext)
        startVibration(appContext)
    }

    @Synchronized
    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ses durdurulamadı", e)
        } finally {
            mediaPlayer = null
        }
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Titreşim durdurulamadı", e)
        } finally {
            vibrator = null
        }
    }

    fun isPlaying(): Boolean = mediaPlayer != null

    private fun startSound(context: Context) {
        // Önce alarm zil sesi; yoksa bildirim/zil sesine düş.
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alarm sesi başlatılamadı", e)
            mediaPlayer = null
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
