# Çevirici

Android için varsayılan telefon (dialer) uygulaması. Kotlin, minSdk 26 (Android 8.0), targetSdk 34.

Özellikler: tuş takımı ve DTMF sesi, son aramalar (gelen/giden/cevapsız), rehberden arama ve arama kutusu, varsayılan telefon uygulaması olma isteği (RoleManager), InCallService ile gelen/giden arama ekranı, kilit ekranında tam ekran gelen arama bildirimi, bildirimden yanıtla/reddet/kapat, sessiz, hoparlör, beklet, aramalar arası geçiş, görüşme sırasında tuşlama, yakınlık sensörüyle ekran kapatma, görüşme süresi sayacı.

APK almak için: bu klasörün içeriğini yeni bir GitHub deposuna yükleyin (main dalına). Actions sekmesinde "APK derle" iş akışı kendiliğinden çalışır; bittiğinde çalıştırma sayfasının altındaki "Cevirici-apk" dosyasını indirin, içindeki app-debug.apk'yı telefona kurun. Elle tetiklemek için Actions > APK derle > Run workflow.

Android Studio ile: klasörü açın, Gradle senkronizasyonundan sonra Run'a basın.

İlk açılışta izinleri verin ve "Varsayılan yap" düğmesiyle Çevirici'yi varsayılan telefon uygulaması seçin. Varsayılan olmadan da tuşlayıp arama başlatabilir, ama arama ekranı sistemin uygulamasında açılır.

Kod yapısı (app/src/main/java/tr/afa/cevirici):
MainActivity tuş takımı, CallService sistemin aramaları teslim ettiği InCallService, CallManager arama listesi ve durum bildirimleri, InCallActivity arama ekranı, CallNotifier bildirimler, CallActionReceiver bildirim düğmeleri, RecentsActivity ve ContactsActivity listeler, Keypad ve CallUtil yardımcılar.

Not: release APK şimdilik debug anahtarıyla imzalanır. Play Store'a koymak için kendi imza anahtarınızı app/build.gradle.kts içinde tanımlayın.
