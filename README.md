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

## Yol haritası (sonraki modüller)

- Hatırlatma için ses/titreşim profili seçimi
- Liste içinde tamamlananlar geçmişi
- Notlar / görev listesi modülü
- Takvim entegrasyonu
