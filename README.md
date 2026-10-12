# TilkiSkyblock

TilkiCraft için Spigot 1.21.x / Java 21 SkyBlock eklentisi. Ada, çiftçi deposu ve menüler bağımsız bir projedir; AuraCosmetics dosyalarına bağlı değildir.

## Kurulum

1. Java 21 ile çalışan Spigot/Paper 1.21.x sunucusuna `dist/TilkiSkyblock-1.0.0.jar` dosyasını `plugins/` altına kopyalayın.
2. Oyun parasıyla yükseltmeler ve otomatik satış için Vault ile Vault uyumlu bir ekonomi eklentisi kurun. Vault yoksa ada ve depo çalışır, ücretli işlemler kapalı kalır.
3. Sunucuyu başlatın. `plugins/TilkiSkyblock/config.yml` içinden fiyatları ve kademe sınırlarını düzenleyebilirsiniz. Veri `plugins/TilkiSkyblock/islands.yml` içinde saklanır.

## Kullanım

- İlk `/is` veya `/is create` komutu, isim/biom seçimi olmadan klasik tek tip adayı kurar. Sonraki `/is` ana menüyü açar; `/is home` adaya götürür.
- `/is invite <oyuncu>`, `/is join`, `/is leave`, `/is kick <oyuncu>` üyelik içindir. Üye menüsünde ada sahibi sol tıkla yetkiyi Üye → Yönetici, sağ tıkla Yönetici → Üye değiştirir.
- Ana menüden çiftçi deposu, yönetim paneli, eklentiler, yükseltmeler ve blok dönüştürmeye girilir.
- Çiftçi deposu toplam kapasitesi 10.000 → 50.000 → 250.000 → 1.000.000 eşyadır. Depodaki bir ürüne sol tık 64, sağ tık 1 adet çeker. Sandık oto toplama açıkken adada yere çıkan düz eşyalar depoya alınır. Özel meta taşıyan eşyalar güvenli biçimde yerde kalır.
- Eklentiler: dolu depodaki fiyatı tanımlı ürünleri otomatik satma, toplanan uygun ürünleri pişirme ve kırılan blokların düşenlerini doğrudan depolama.
- Yükseltmeler: ada sınırı (25–100 blok yarıçap), jeneratör cevher şansı, üye (2–10), piston (500–3000). Fiyatlar oyun parasından düşer.
- Blok dönüştürme: depodaki 10 cevher/külçeden 1 blok üretir (9 malzeme + %10 işlem vergisi, tam sayıya yukarı yuvarlama). Tek tıkta en çok 64 blok verir. Ham bakır ve bakır külçesi ayrı seçeneklerdir.
- Ada seviyesi sohbetin solunda kırmızı `[Lv.N]` olarak gösterilir. Bu ilk sürümde seviye başarılı blok kırma sayısıdır.

## Derleme

```powershell
mvn package
```

Çıktı: `target/TilkiSkyblock-1.0.0.jar`.
