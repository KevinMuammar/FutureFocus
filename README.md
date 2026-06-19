# FutureFocus

FutureFocus adalah aplikasi Android untuk membangun kebiasaan fokus berbasis goal dan sesi terukur. Pengguna dapat membuat goal, memilih durasi sesi, menjalankan Focus Lock, menerima peringatan bertingkat saat mencoba meninggalkan sesi, serta memantau riwayat dan statistik progres.

## Status Saat Ini

Aplikasi berada pada tahap MVP yang dapat dikompilasi dan mencakup:

- Goal dan subtask berbasis Firestore.
- Sesi fokus dengan countdown dan daily goal.
- Focus Lock berbasis `AccessibilityService`, foreground service, dan recovery navigasi.
- Exit message 4 level untuk upaya keluar melalui Back, Home, Recent Apps, atau berpindah aplikasi.
- Quote motivasi dari ZenQuotes dengan cache Room dan fallback lokal.
- Screen Home, Duration, Permission, Focus Lock, Session Complete, History, Statistics, Create Goal, dan Goal Detail.

Teknologi utama:

- Kotlin dan Jetpack Compose Material 3.
- Navigation Compose dan `ViewModel`/`StateFlow`.
- Firebase Firestore dan Firebase Analytics.
- Room Database.
- Retrofit dan ZenQuotes API.
- Android `AccessibilityService` dan foreground service.

## Fitur

### Goal System

- Membuat goal dengan judul, deskripsi, target jam, dan daftar subtask.
- Menampilkan active goals beserta progress durasi.
- Mengubah status subtask dari Goal Detail.
- Memilih goal opsional ketika memulai sesi fokus.
- Mengurangi `remainingHours` setelah sesi berhasil selesai.
- Menandai goal selesai jika target waktu habis atau seluruh subtask selesai.

### Dashboard dan Statistik

Home menampilkan:

- Waktu fokus hari ini.
- Focus streak.
- Target fokus harian dan progress.
- Jumlah sesi sukses/gagal.
- Daftar active goals.
- Dialog sesi gagal dan dialog goal selesai.

Statistics menampilkan ringkasan total waktu fokus, streak, daily goal progress, serta hasil sesi. History menampilkan daftar sesi, status, durasi, exit attempt, dan goal terkait jika tersedia.

### Focus Session Flow

Alur memulai sesi:

1. Pengguna menekan `Start Focus` dari Home.
2. Pengguna memilih durasi dan, bila diperlukan, goal yang terkait.
3. Permission Screen memastikan layanan aksesibilitas dan izin overlay aktif.
4. Aplikasi membuat `FocusSession` dan membuka Focus Lock Screen.
5. Countdown berjalan sampai sesi selesai atau pengguna mengakhiri sesi pada exit message level 4.

Saat sesi selesai dengan sukses, durasi sesi mengurangi sisa durasi goal terkait dan pengguna diarahkan ke Session Complete Screen.

## Focus Lock Saat Ini

Komponen utama:

- `FocusLockScreen`: countdown, dialog exit, dan lifecycle sesi.
- `FocusLockService`: mendeteksi window di luar aplikasi dan menjalankan recovery.
- `FocusSessionTracker`: state lintas UI/service dan callback upaya navigasi.
- `FocusForegroundService`: notifikasi ongoing selama sesi.
- `FocusOverlayManager`: overlay visual ketika aplikasi lain terbuka.

### Exit Message

Semua usaha navigasi berikut dapat memicu exit message:

- Menekan tombol Back pada Focus Lock Screen.
- Menekan Home.
- Membuka Recent Apps/Overview.
- Berpindah ke aplikasi lain.

Dialog hanya menampilkan satu pesan pada satu waktu. Jika dialog sedang terbuka, event navigasi tambahan tidak menaikkan level hingga pengguna menekan `Lanjut Fokus`.

| Level | Judul | Aksi yang tersedia | Quote |
| --- | --- | --- | --- |
| 1 | Reflection Stage | Lanjut Fokus | Tidak ada |
| 2 | Motivation Stage | Lanjut Fokus | Motivation |
| 3 | Consequence Stage | Lanjut Fokus | Discipline/Hard Work |
| 4+ | Emotional Reflection | Lanjut Fokus, Akhiri Sesi | Hope/fallback |

`attemptCount` disimpan pada sesi. Setelah level 4, pesan tetap berada pada level 4 untuk attempt berikutnya.

### Back Button

Back manual ditangani oleh Compose `BackHandler` dan membuka exit message. Service juga menggunakan `GLOBAL_ACTION_BACK` secara internal untuk menutup Home/Overview atau aplikasi luar. Back internal tersebut ditandai dan disaring oleh `FocusSessionTracker` agar tidak membuat pesan dobel.

### Home dan Recent Apps

Saat pengguna meninggalkan Focus Lock:

- `MainActivity.onPause()` memberi sinyal bahwa aplikasi tidak lagi foreground.
- Accessibility events dari launcher/System UI juga dapat memicu recovery.
- Home dan Recent dipulihkan tanpa menampilkan overlay singkat.
- Service menjalankan `GLOBAL_ACTION_BACK` dan `bringAppToFront()`.
- Jika activity belum kembali foreground, service mengulang recovery.
- Event System UI tidak diblokir oleh cooldown, sehingga tap Recent berulang tetap diproses.

Selama sesi focus aktif, task aplikasi ditandai `setExcludeFromRecents(true)`. Tujuannya adalah mencegah kartu FutureFocus tersedia untuk diswipe dari Overview ketika sesi sedang berjalan. Flag tersebut dilepas ketika layar Focus Lock selesai dibuang.

### Membuka Aplikasi Lain

Jika pengguna mencapai aplikasi lain selama sesi:

- `FocusOverlayManager` menampilkan overlay fullscreen visual.
- Exit message dipicu selama sesi belum mencapai batas auto-fail.
- Service mengirim `GLOBAL_ACTION_BACK` dan mencoba membawa FutureFocus ke foreground.
- Perpindahan ke aplikasi lain dihitung; pada percobaan ketiga sesi dipaksa gagal dan aplikasi kembali ke Home.

Overlay tidak digunakan pada recovery Home/Recent agar transisi tidak terlihat berkedip.

### Batasan Android

Implementasi ini menggunakan API aplikasi biasa, bukan mode device-owner/kiosk. Karena itu:

- Redirect Home/Recent bergantung pada event aksesibilitas, lifecycle activity, `GLOBAL_ACTION_BACK`, dan background activity launch.
- Perilaku Overview dapat berbeda antar launcher atau vendor perangkat.
- Overlay `TYPE_APPLICATION_OVERLAY` bersifat visual dan bukan mekanisme pemblokiran System UI yang absolut.
- Accessibility Service dan izin display-over-other-apps harus diaktifkan manual oleh pengguna.

## Quote dan Exit Message

`MessageRepository` memuat quote dengan alur berikut:

1. Coba membaca quote tersimpan dari Room.
2. Jika Room kosong, ambil `GET https://zenquotes.io/api/quotes`.
3. Filter quote menggunakan keyword relevansi.
4. Kategorikan sebagai `MOTIVATION`, `DISCIPLINE`, `HARD_WORK`, atau `HOPE`.
5. Simpan hasil ke Room dan memory cache.

Saat exit message dibutuhkan, tidak ada request jaringan baru. Pesan lokal berasal dari `LocalExitMessages`, sedangkan quote diambil dari memory cache atau fallback berikut:

- `Keep going.`
- `Your future self will thank you.`
- `Do not stop now.`
- `Small discipline today becomes confidence tomorrow.`

## Data dan Penyimpanan

Firestore saat ini memakai user tetap `demo-user`; Firebase Authentication belum diimplementasikan.

Struktur Firestore:

```text
users/demo-user
  sessions/{sessionId}
    duration
    attempt_count
    status
    created_at
    completed_at
    goal_id
    goal_title
  daily_goals/{yyyy-MM-dd}
    target_minutes
    completed_minutes
    updated_at
  goals/{goalId}
    title
    description
    totalHours
    remainingHours
    isCompleted
    createdAt
    subtasks/{subtaskId}
      title
      isCompleted
      createdAt
```

Data quote disimpan lokal melalui Room pada tabel `quotes`.

## Navigasi

Routes aplikasi:

| Route | Screen | Fungsi |
| --- | --- | --- |
| `home` | `HomeScreen` | Dashboard dan entry point |
| `duration` | `DurationScreen` | Memilih sesi dan goal |
| `permission` | `PermissionScreen` | Validasi permission Focus Lock |
| `focus/{sessionId}` | `FocusLockScreen` | Sesi terkunci |
| `complete/{sessionId}` | `SessionCompleteScreen` | Hasil sesi sukses |
| `history` | `HistoryScreen` | Riwayat sesi |
| `statistics` | `StatisticsScreen` | Ringkasan statistik |
| `create_goal` | `CreateGoalScreen` | Membuat goal |
| `goal_detail/{goalId}` | `GoalDetailScreen` | Detail dan subtask goal |

## Struktur Kode

```text
app/src/main/java/com/example/futurefocus/
  data/
    LocalExitMessages.kt
    local/                         # Room quote cache
    remote/                        # ZenQuotes Retrofit API
  model/                           # Goal, FocusSession, ExitMessage
  navigation/                      # Screen routes dan NavHost
  repository/
    FocusRepository.kt             # Session dan daily goal Firestore
    GoalRepository.kt              # Goal dan subtask Firestore
    MessageRepository.kt           # Exit message dan quote cache
  service/
    FocusForegroundService.kt      # Notification ongoing
    FocusLockService.kt            # Accessibility recovery
    FocusOverlayManager.kt         # Overlay app lain
    FocusSessionTracker.kt         # Bridge state UI/service
  ui/
    component/
    screen/
    theme/
  utils/
    PermissionHelper.kt
    TimeFormatter.kt
  FutureFocusApplication.kt
  MainActivity.kt
```

## Permission dan Konfigurasi Android

Permission yang dideklarasikan:

- `INTERNET`
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `SYSTEM_ALERT_WINDOW`

Manifest juga mendaftarkan:

- `FocusForegroundService`.
- `FocusLockService` sebagai accessibility service.
- Query untuk launcher Home agar service dapat mengenali package launcher pada Android modern.

Minimum SDK aplikasi adalah 31 dan target SDK adalah 36.

## Build

Compile Kotlin:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon --offline --console plain
```

Build APK debug:

```powershell
.\gradlew.bat assembleDebug --no-daemon --offline --console plain
```

Output APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Known Gaps

- Firebase Authentication belum tersedia; Firestore menggunakan `demo-user`.
- Firestore security rules production belum disediakan.
- Testing masih berupa template dasar dan belum mencakup flow Focus Lock.
- Notifikasi foreground belum memperbarui countdown secara realtime.
- Redirect Home/Recent adalah mitigasi berbasis accessibility, bukan lock tingkat sistem.
- Statistics belum menyediakan chart kompleks.

## File Penting Untuk Pengembangan Lanjutan

- `app/src/main/java/com/example/futurefocus/service/FocusLockService.kt`: recovery navigasi dan batasan perangkat.
- `app/src/main/java/com/example/futurefocus/ui/screen/FocusLockScreen.kt`: timer dan exit dialog.
- `app/src/main/java/com/example/futurefocus/service/FocusSessionTracker.kt`: komunikasi UI/service.
- `app/src/main/java/com/example/futurefocus/repository/FocusRepository.kt`: session dan daily goal.
- `app/src/main/java/com/example/futurefocus/repository/GoalRepository.kt`: goal/subtask.
- `app/src/main/java/com/example/futurefocus/repository/MessageRepository.kt`: cache quote dan mapping level message.
- `app/src/main/java/com/example/futurefocus/navigation/FutureFocusNavHost.kt`: alur layar aplikasi.
