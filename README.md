# AssistMe

Kişisel asistan mobil uygulaması (Android öncelikli).

**İlk modül: Hatırlatmalar.** Zamanı geldiğinde ekrana bildirim basar, bildirimin
üzerinden **ertele** veya **tamam** yapılabilir. Hatırlatmalar tek seferlik veya
**tekrarlı** (saatlik / günlük / haftalık / aylık / özel dakika aralığı) olabilir.

## Özellikler

- ⏰ Tam zamanlı alarm (`AlarmManager.setExactAndAllowWhileIdle`) — Doze modunda da çalışır.
- 🔔 Yüksek öncelikli bildirim (heads-up), başlık + not gösterimi.
- 💤 Bildirim üzerinden **erteleme** (varsayılan 10 dk, ayarlanabilir).
- 🔁 Tekrarlı hatırlatmalar; tetiklendikçe bir sonraki tekrar otomatik kurulur.
- 🔄 Cihaz yeniden başladığında / saat değiştiğinde alarmlar otomatik yeniden kurulur.
- 💾 Room veritabanında kalıcı saklama.
- 🎨 Jetpack Compose + Material 3 (Android 12+ dinamik renk).

## Teknoloji

| Katman | Seçim |
|--------|-------|
| Dil | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Veri | Room (KSP) |
| Zamanlama | AlarmManager + BroadcastReceiver |
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 35 |

## Proje yapısı

```
app/src/main/java/com/artsistem/assistme/
├── AssistMeApp.kt              # Application; bildirim kanalı + repository kabı
├── MainActivity.kt            # Compose host, navigasyon, izin istekleri
├── data/                      # Room: Reminder, DAO, Database, Repository
└── reminder/
    ├── ReminderScheduler.kt   # Alarm kurma / iptal
    ├── Recurrence.kt          # Bir sonraki tekrar zamanını hesaplama
    ├── Notifications.kt       # Bildirim + ertele/tamam aksiyonları
    ├── ReminderReceiver.kt    # Alarm tetiklenince bildirim + sonraki tekrar
    ├── NotificationActionReceiver.kt  # Ertele / Tamam işleme
    ├── BootReceiver.kt        # Reboot sonrası yeniden kurma
    └── Settings.kt            # Varsayılan erteleme süresi (SharedPreferences)
└── ui/                        # Compose ekranları + ViewModel + tema
```

## İzinler

Uygulama ilk açılışta şunları ister:

- `POST_NOTIFICATIONS` (Android 13+) — bildirim gösterebilmek için.
- Tam alarm izni (Android 12+) — ayarlar ekranına yönlendirir; tam dakikasında
  tetikleme için gereklidir. Verilmezse yaklaşık alarma düşülür.

## Derleme

> Bu repo tam bir Android Studio projesidir. Derlemek için **Android SDK**
> gerekir (bu container'da SDK kurulu değildir, bu yüzden burada APK üretilmedi).

Android Studio (Ladybug+) ile açın **veya** SDK'lı bir ortamda:

```bash
# local.properties içine SDK yolunu yazın (Android Studio otomatik yapar):
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug      # APK üretir
./gradlew installDebug       # bağlı cihaza kurar
```

## VS Code ile geliştirme

Bu projeyi VS Code'da geliştirmek için:

**Gereksinimler (yerel makinede):**
- **JDK 17+** (Android Studio yoksa ayrı kur)
- **Android SDK** (komut satırı araçları yeterli) — `ANDROID_HOME` / `ANDROID_SDK_ROOT` ortam değişkeni ayarlı olmalı
- **adb** (platform-tools) PATH'te olmalı

**Adımlar:**
1. Klasörü VS Code'da aç. Açılışta önerilen eklentiler sorulur (`.vscode/extensions.json`):
   Gradle for Java, Kotlin, Java Extension Pack.
2. SDK yolunu bildir (Android Studio kurulu değilse elle):
   ```bash
   echo "sdk.dir=/path/to/Android/sdk" > local.properties
   ```
3. `Ctrl/Cmd + Shift + P → Tasks: Run Task` ile hazır görevleri kullan:
   - **Android: Debug APK derle** (varsayılan build — `Ctrl/Cmd+Shift+B`)
   - **Android: Cihaza kur** (bağlı cihaz/emülatör gerekir)
   - **Android: Çalıştır** (kur + uygulamayı başlat)
   - **Android: Temizle / Testler**
4. Emülatör başlatmak için (SDK içindeki) `emulator -list-avds` ve `emulator -avd <ad>` kullan;
   ya da fiziksel cihazda USB hata ayıklamasını aç.

> Not: Tam Android araçları (görsel layout, profiler, AVD yöneticisi) için **Android Studio**
> daha rahattır; VS Code daha hafif bir editör deneyimi sunar. İkisi de aynı Gradle projesini kullanır.

## Yol haritası (sonraki modüller)

- Hatırlatma için ses/titreşim profili seçimi
- Liste içinde tamamlananlar geçmişi
- Notlar / görev listesi modülü
- Takvim entegrasyonu
