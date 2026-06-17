# 🎮 Bir Kelime Bir İşlem

Klasik TV yarışmasından esinlenen, **Türkçe**, metin tabanlı mobil zekâ oyunu.
React Native (Expo) ile yazıldı, modern **pastel** bir tasarımı var.

İki tur:

- 🔤 **Bir Kelime** — Seçtiğin 9 harften en uzun anlamlı Türkçe kelimeyi bul.
- 🔢 **Bir İşlem** — Verilen 6 sayı ile `+ − × ÷` kullanarak hedef sayıya ulaş.
- 🏆 **Düello** — Önce kelime, sonra işlem; puanlar toplanır.

İki oyun modu:

- ⏱ **Yarışma** — Her tur süreli, doğru cevaba puan.
- 🌿 **Rahat** — Süre yok, dilediğin kadar düşün.

Ayrıca:

- 🔢 **Oturum uzunluğu** — 5 veya 10 soruluk seri; sonunda toplam skor.
- 📊 **Skor geçmişi + en yüksek skor** — sonuçlar telefonda kalıcı saklanır
  (oyun türü ve soru sayısı kırılımında rekor takibi).
- 🌍 **İki dil** — Türkçe ve İngilizce; hem arayüz hem kelime tabanı değişir
  (TR: 33.913 kelime, EN: 202.565 kelime). Ana menüden TR/EN ile değiştir.
- 👥 **Kod ile meydan okuma (multiplayer, backend yok)** — Bir tur oyna, çıkan
  16 haneli kodu arkadaşına gönder. O da kodu girip **birebir aynı soruları**
  oynar; skorlar karşılaştırılır. Sorular bir **seed**'den deterministik
  üretildiği için sunucuya gerek kalmaz; kodun içinde oyun türü, mod, dil,
  soru sayısı, seed ve senin skorun saklıdır.

## ✨ Öne çıkanlar

- **Her işlem sorusu kesinlikle çözülebilir.** Hedef, verilen sayılardan
  gerçekten ulaşılabilen bir değerden üretilir (önce çözüm bulunur, soru ona
  göre kurulur). Klasik kurallar geçerli: ara sonuçlar daima pozitif tam sayı,
  bölmeler tam bölünür. Oyun ayrıca **örnek çözümü** gösterir.
- **33.913 kelimelik** TDK tabanlı Türkçe sözlük (özel adlar ve çok kelimeli
  ifadeler ayıklandı). Oyun, harflerinden bulunabilecek **en uzun kelimeyi**
  de gösterir.
- Türkçe harf frekansına uygun sesli/sessiz dağılımı.
- Bağımlılığı az, hızlı ve tamamen çevrimdışı çalışır.

## 📱 APK'yı indir (kurulum)

Sunucuda derleme kurmana gerek yok — APK'yı GitHub otomatik üretir:

1. Bu depoda **Actions** sekmesine git.
2. **"Android APK Derle"** çalışmasını aç (her push'ta otomatik çalışır;
   istersen `Run workflow` ile elle de tetikleyebilirsin).
3. Çalışma bitince alttaki **Artifacts** bölümünden
   `bir-kelime-bir-islem-apk` dosyasını indir, zip'ten çıkar.
4. `bir-kelime-bir-islem.apk` dosyasını Android telefonuna at ve kur
   (Ayarlar'dan "bilinmeyen kaynaklara izin ver" gerekebilir).

> APK, hata ayıklama (debug) anahtarıyla imzalanır; kişisel kullanım ve test
> için kurulabilir. Google Play'e yüklemek istersen kendi release anahtarınla
> imzalaman gerekir.

## 🛠 Geliştirme

```bash
npm install
npx expo start      # Expo Go ile telefonda anında dene (QR okut)
```

APK'yı yerelde derlemek için (Android SDK + JDK 17 gerekir):

```bash
npx expo prebuild --platform android --no-install
cd android && ./gradlew assembleRelease
# çıktı: android/app/build/outputs/apk/release/app-release.apk
```

## 📂 Proje yapısı

```
App.js                     # yönlendirme + düello akışı + sonuç ekranı
src/
  theme.js                 # pastel renk paleti
  components/
    ui.js                  # Button, Header, Card
    useCountdown.js        # geri sayım kancası
  i18n.js                  # TR/EN çeviriler + dil context'i
  storage.js               # skor geçmişi + rekor (AsyncStorage)
  game/
    numbers.js             # işlem üreteci + çözüm (çözülebilirlik garantisi)
    letters.js             # harf çekimi + dağılım (TR + EN)
    dictionary.js          # dil-duyarlı sözlük + en uzun kelime bulucu
    challenge.js           # seed'li PRNG, deterministik soru üretimi, kod encode/decode
  data/
    words.json             # 33.913 Türkçe kelime
    words_en.json          # 202.565 İngilizce kelime
  screens/
    HomeScreen.js
    NumbersScreen.js
    LettersScreen.js
    ResultScreen.js        # oturum/düello/meydan okuma sonuç ekranı
    ScoresScreen.js        # skor geçmişi + rekorlar
    ChallengeScreen.js     # kod oluştur / kodla oyna
.github/workflows/
  build-apk.yml            # otomatik APK derleme
```

## 📜 Lisans

Kelime listesi: TDK tabanlı, [mertemin/turkish-word-list](https://github.com/mertemin/turkish-word-list) üzerinden derlendi.
Oyun kodu MIT lisansıyla paylaşılabilir.
