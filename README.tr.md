# Iconfy

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.bilecen.iconfy)](https://plugins.gradle.org/plugin/io.github.bilecen.iconfy)
[![CI](https://github.com/Bilecen/composeiconfy/actions/workflows/ci.yml/badge.svg)](https://github.com/Bilecen/composeiconfy/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[🇬🇧 English](README.md) · **🇹🇷 Türkçe**

[Iconify](https://iconify.design)'ın 200.000+ ikonundan (Material Design Icons, Tabler, Lucide,
Phosphor, Font Awesome, Carbon, …) herhangi birini **Android Jetpack Compose**'da kullan — Gradle
build'inde bildir, derleme sırasında projeye gömülsün, sonrası tamamen **offline**.

[icon-sets.iconify.design](https://icon-sets.iconify.design/) üzerinden beğendiğin ikonu seç,
`prefix:name` olarak `iconfy { }` bloğuna ekle; Gradle plugin onu bir kez indirir, tip-güvenli bir
`ImageVector`'a dönüştürür ve temiz bir erişimci üretir:

```kotlin
Icon(
    imageVector = Iconfy.Mdi.Home,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary,
)
```

Runtime'da ağ çağrısı yok, elle SVG kopyalama yok, 200k ikonluk bağımlılık şişkinliği yok — yalnızca
gerçekten bildirdiğin ikonlar gömülür.

## Kurulum

Plugin'i uygulama modülünde uygula ve ikonları bildir:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.github.bilecen.iconfy") version "0.2.4"
}

iconfy {
    packageName.set("com.example.app.icons")  // üretilen `Iconfy` objesinin paketi
    icons {
        add("mdi:home")
        add("tabler:user")
        prefix("lucide") {          // tek bir set için kısayol
            add("heart")
            add("star")
        }
    }
}
```

Plugin, `com.android.tools:sdk-common`'a (yalnızca build-time) bağlıdır; bu yüzden plugin çözümleme
depolarında **`google()`** bulunduğundan emin ol:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Üretilen kod yalnızca `androidx.compose.ui`'ye bağlıdır — Compose uygulamanda zaten var.

## Kullanım

İkonlar, set prefix'ine göre gruplanmış, tembel (lazy) oluşturulup önbelleklenen `ImageVector`'lar
olarak sunulur. Tek bir import her şeyi kapsar:

```kotlin
import com.example.app.icons.Iconfy

Icon(Iconfy.Mdi.Home, contentDescription = "Ana sayfa")
Icon(Iconfy.Tabler.User, contentDescription = "Profil", tint = Color.Red)
Icon(Iconfy.Lucide.Heart, contentDescription = null)
```

İsimler PascalCase'e çevrilir: `arrow-left` → `ArrowLeft`, `mdi:github` → `Iconfy.Mdi.Github`,
`material-symbols:settings` → `Iconfy.MaterialSymbols.Settings`. Prefix'e göre iç içe olması sayesinde
farklı setlerdeki aynı isimli ikonlar asla çakışmaz.

### Kategoriler

Bildirimleri `category("Ad")` içine alarak farklı setleri karıştıran dış bir semantik küme
oluşturabilirsin. Prefix grubu içeride korunur, yani `Iconfy.<Kategori>.<Prefix>.<İkon>` olur:

```kotlin
iconfy {
    icons { add("mdi:home") }              // → Iconfy.Mdi.Home
    category("Dashboard") {
        prefix("mdi") { add("home") }      // → Iconfy.Dashboard.Mdi.Home
        add("tabler:settings")             // → Iconfy.Dashboard.Tabler.Settings
    }
}
```

Aynı ikon hem üst seviyede hem de istediğin kadar kategoride görünebilir (yalnızca bir kez indirilir).
`Iconfy.Dashboard.` yazdığın anda IDE otomatik tamamlaması tüm ağacı gösterir.

Semantik kategori içinde `Mdi` tuhaf duruyorsa ortadaki prefix segmentini `prefix("mdi", named = "Nav")`
ile yeniden adlandır:

```kotlin
category("Dashboard") {
    prefix("mdi", named = "Nav") { add("home") }   // → Iconfy.Dashboard.Nav.Home
}
```

**Farklı setlerden** ikonları tek bir grupta toplamak için `prefix(...)` bloklarını tekrarlamadan, orta
grubu her ikon için `into` ile ver (set koordinattan gelir):

```kotlin
icons {
    add("hugeicons:gpu",              named = "Gpu",    into = "Cards")
    add("clarity:hard-disk-line",     named = "Disk",   into = "Cards")
    add("iconoir:multi-mac-os-window", named = "OsType", into = "Cards")
}
// → Iconfy.Cards.Gpu, Iconfy.Cards.Disk, Iconfy.Cards.OsType
```

### Özel erişimci adı

Üretilen erişimci adını değiştirmek için `named` ver (uzun/kullanışsız ikon adları için pratik):

```kotlin
icons {
    add("lucide:chart-bar", named = "Chart")   // → Iconfy.Lucide.Chart
    prefix("mdi") { add("home", named = "Main") }  // → Iconfy.Mdi.Main
}
```

### Renklendirme (tint)

Monokrom ikonlar (`fill="currentColor"`, çoğunluğu) opak siyah dolguyla üretilir ve `Icon`
composable'ının `tint`'i ile normal şekilde renklenir. Gerçekten çok renkli bir ikon için ise
renklerinin korunması adına `Image(imageVector = …)` kullan.

## Yapılandırma

| Seçenek | Varsayılan | Açıklama |
|---|---|---|
| `packageName` | `iconfy.generated` | Üretilen `Iconfy` objesinin paketi |
| `accessorName` | `Iconfy` | Kök obje adı (`Iconfy.Mdi.Home`) |
| `apiUrl` | `https://api.iconify.design` | Self-host edilen Iconify API için override |
| `failOnMissing` | `true` | Bilinmeyen ikonda build'i fail et (yoksa uyar & atla) |

## Nasıl çalışır

İki build-time task, ikisi de incremental:

1. **`iconfyFetch`** — set başına tek istekle Iconify API'sine gider, her ikonun gövdesini kendi
   kendine yeten bir SVG'ye sarar ve `<rootDir>/.iconfy/cache/<prefix>/<name>.svg`'ye yazar.
   Cache-first: diskte olan ikon asla tekrar indirilmez, yani `--offline` build'ler sıfır ağ I/O yapar.
2. **`iconfyGenerate<Variant>`** — her SVG'yi Android'in `Svg2Vector`'ü ile normalize eder (şekiller,
   transformlar ve `currentColor` dahil) ve tek bir Kotlin dosyası `ImageVector` erişimcisi üretir.
   Üretilen kod ham path verisini saklar ve runtime'da Compose'un `addPathNodes(...)`'ı parse eder.
   Varsayılan görüntü boyutu 24dp'ye normalize edilir; böylece farklı viewBox'lı setler tutarlı çizilir.

### Reproducible / offline CI

Önbellek `build/`'in **dışında** `.iconfy/cache/`'te durur, yani `clean` asla yeniden indirmeye
zorlamaz. Tamamen ağsız, reproducible CI build'leri için **`.iconfy/cache/`'i commit et** — ya da
bir kez ağ ile çalıştır, sonra her yerde `--offline` ile derle.

## Yayınlama (maintainer'lar için)

Plugin, `iconfy-gradle-plugin/` altında bağımsız bir build'dir.

```bash
# Yerel test
./gradlew -p iconfy-gradle-plugin publishToMavenLocal

# Gradle Plugin Portal (com.gradle.plugin-publish + kimlik bilgileri gerekir)
./gradlew -p iconfy-gradle-plugin publishPlugins
```

Yayından önce `iconfy-gradle-plugin/build.gradle.kts` içindeki `website`/`vcsUrl`'yi güncelle.

## Kısıtlar

- VectorDrawable'ın ifade edemediği SVG özelliklerini kullanan ikonlar (filtreler, gömülü raster
  görseller, bazı mask'ler) ikon adı belirtilerek uyarıyla atlanır.
- Çok renkli ikonlar tint'lenen `Icon` yerine `Image` ile kullanılmalı.

## Gereksinimler

JDK 17–21 ile derle (Android Gradle Plugin henüz JDK 25'i desteklemiyor). AGP 8.7+, Kotlin 2.0+.

## Lisans

MIT — bkz. [LICENSE](LICENSE).
