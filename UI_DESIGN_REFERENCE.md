# FutureFocus - UI Design Reference

Dokumen ini menjadi rujukan untuk AI atau desainer yang akan membuat ulang, memperbaiki, atau mengembangkan desain UI aplikasi **FutureFocus**. Isi dokumen mengikuti fitur dan elemen yang saat ini benar-benar tersedia pada implementasi Android.

## 1. Ringkasan Produk

FutureFocus adalah aplikasi Android untuk membantu pengguna menjaga fokus belajar atau bekerja melalui sesi fokus terukur. Pengguna dapat membuat goal, memilih durasi sesi, menghubungkan sesi ke goal tertentu, menjalankan mode **Focus Lock**, serta memantau progres dari riwayat dan statistik.

Karakter pengalaman pengguna yang perlu dipertahankan:

- Sederhana dan tidak ramai agar tidak menambah distraksi.
- Memberi rasa tenang, disiplin, dan progres bertahap.
- Mengutamakan informasi penting: waktu fokus, target hari ini, streak, goal aktif, dan timer.
- Tetap tegas saat pengguna mencoba keluar dari sesi fokus.

## 2. Fitur Utama

### 2.1 Daily Focus Goal

- Menampilkan total waktu fokus yang berhasil diselesaikan hari ini.
- Menampilkan target fokus harian dalam menit.
- Menampilkan progress bar waktu fokus hari ini dibanding target.
- Target harian dapat diubah melalui dialog input angka.
- Nilai default target harian adalah 120 menit.

### 2.2 Goal Belajar atau Kerja

- Pengguna dapat membuat goal dengan judul, deskripsi opsional, dan total target jam.
- Pengguna dapat menambahkan checklist materi atau subtask.
- Goal aktif tampil pada Home.
- Detail goal menampilkan progress jam dan checklist materi.
- Checklist dapat dicentang langsung dari detail goal.
- Goal selesai jika seluruh subtask selesai atau sisa target jam mencapai nol.

### 2.3 Focus Session

- Pengguna dapat memilih preset durasi 25, 30, atau 60 menit.
- Pengguna juga dapat memasukkan custom duration dalam menit.
- Pengguna dapat memilih goal aktif secara opsional sebelum memulai sesi.
- Sesi tanpa goal tetap diperbolehkan sebagai sesi bebas.
- Setelah sesi berhasil, durasi sesi ditambahkan ke statistik dan mengurangi sisa jam goal terkait.

### 2.4 Focus Lock

- Screen timer fullscreen menjaga pengguna tetap berada dalam aplikasi selama sesi berjalan.
- Foreground notification memberi tahu bahwa sesi fokus sedang aktif.
- Aplikasi meminta izin Accessibility Service dan overlay sebelum sesi dimulai.
- Ketika pengguna menekan Back, Home, Recent Apps, atau mencoba membuka aplikasi lain, aplikasi mengarahkan pengguna kembali ke FutureFocus.
- Upaya keluar memunculkan dialog bertingkat dengan pesan refleksi dan quote motivasi.
- Pada percobaan tinggi, pengguna mendapat pilihan untuk mengakhiri sesi.
- Jika pengguna berpindah aplikasi terlalu sering, sesi dipaksa gagal.

### 2.5 History dan Statistics

- History menampilkan daftar sesi fokus beserta durasi, waktu, status, jumlah exit attempt, dan goal terkait jika ada.
- Statistics menampilkan total focus time, streak, progress daily goal, dan perbandingan sesi sukses atau gagal.
- Area chart statistik masih berupa placeholder dan perlu dipertahankan sebagai ruang pengembangan grafik fokus harian.

## 3. Bahasa Visual Saat Ini

### 3.1 Tema

Aplikasi memakai Material 3 dan mendukung light mode serta dark mode.

Light mode:

| Token | Warna | Penggunaan |
| --- | --- | --- |
| Primary | `#2E7D32` | Tombol utama, progress aktif, angka penting |
| Secondary | `#8BC34A` | Aksen sekunder |
| Tertiary | `#10231A` | Aksen gelap |
| Background | `#F7FBF4` | Latar belakang screen |
| Surface/Card | `#FFFFFF` | Card |
| Primary text | `#10231A` | Teks utama |

Dark mode:

| Token | Warna | Penggunaan |
| --- | --- | --- |
| Primary | `#B9F6CA` | Tombol utama, progress aktif |
| Secondary | `#DCE775` | Aksen sekunder |
| Tertiary | `#A7FFEB` | Aksen tambahan |
| Background | `#10231A` | Latar belakang screen |
| Surface/Card | `#183026` | Card |
| Primary text | `#EFF8EC` | Teks utama |

### 3.2 Bentuk dan Spacing

- Screen umumnya memakai padding horizontal `20dp` dan vertikal `24dp`.
- Card memakai radius `8dp`, elevation ringan `2dp`, dan padding internal `18dp`.
- Tombol utama fullscreen-width memiliki tinggi `52dp` dan radius `8dp`.
- Tombol sekunder memakai outlined button dengan tinggi `48dp`.
- Jarak antar bagian umumnya `12dp` sampai `22dp`.
- Progress track linear memiliki tinggi `10dp` dan sudut membulat.

### 3.3 Tipografi

- Gunakan font default Android atau sans-serif modern yang bersih.
- Heading utama memakai bobot bold.
- Judul bagian memakai medium atau semi-bold.
- Teks penjelas memakai body text dengan opacity sekitar 65% sampai 72%.
- Angka timer dan durasi harus menjadi elemen visual paling dominan pada screen terkait.

### 3.4 Komponen Bersama

| Komponen | Bentuk dan fungsi |
| --- | --- |
| `FocusCard` | Card putih atau surface gelap, radius `8dp`, elevation tipis, isi vertikal |
| `PrimaryAction` | Tombol primary full-width untuk aksi utama |
| `SecondaryAction` | Outlined button untuk aksi sekunder |
| `StatTile` | Card statistik dengan label kecil dan nilai besar bold |
| `TopBarTitle` | Judul screen di kiri dan tombol Back/Kembali di kanan |
| `ProgressTrack` | Progress bar linear dengan track primary ber-opacity rendah |

## 4. Struktur Navigasi

```text
Home
|- Duration
|  |- Create Goal
|  |- Permission
|     |- Focus Lock
|        |- Session Complete
|- Create Goal
|- Goal Detail
|- History
|- Statistics
```

Route teknis:

| Route | Screen |
| --- | --- |
| `home` | Home |
| `duration` | Set Duration |
| `permission` | Permission |
| `focus/{sessionId}` | Focus Lock |
| `complete/{sessionId}` | Session Complete |
| `history` | History |
| `statistics` | Statistics |
| `create_goal` | Create Goal |
| `goal_detail/{goalId}` | Goal Detail |

## 5. Detail Tiap Screen

### 5.1 Home Screen

Tujuan: dashboard utama dan titik masuk seluruh fitur.

Elemen yang tampil dari atas ke bawah:

1. Branding:
   - Judul besar: `FutureFocus`
   - Subtitle: `Halo, siap menjaga fokus hari ini?`

2. Card `Today's focus`:
   - Label `Today's focus`
   - Total fokus hari ini dalam format menit atau jam, tampil sangat besar dengan warna primary
   - Teks streak: `Streak {jumlah} hari`
   - Progress bar target harian
   - Teks: `Goal {waktu hari ini} / {target harian}`
   - Tombol outlined `Edit Goal`

3. Dua stat tile sejajar:
   - `Success` dengan jumlah sesi berhasil
   - `Failed` dengan jumlah sesi gagal

4. Dua tombol outlined sejajar:
   - `History`
   - `Statistics`

5. Tombol outlined:
   - `+ Buat Goal Baru`

6. Daftar goal aktif:
   - Heading `Active Goals`, hanya tampil jika terdapat goal aktif
   - Setiap card goal berisi judul goal dan progres jam: `{jam selesai} / {total jam} jam`
   - Card dapat ditekan untuk membuka Goal Detail

7. Tombol utama sticky di bagian bawah:
   - `Start Focus`

State tambahan:

- Jika tidak ada goal aktif, heading `Active Goals` dan daftar card tidak ditampilkan.
- Dialog `Daily goal` muncul ketika pengguna menekan `Edit Goal`.
- Dialog input memiliki field `Target fokus`, suffix `menit`, serta tombol `Simpan` dan `Batal`.
- Dialog `Sesi Gagal` dapat muncul setelah pengguna meninggalkan sesi secara paksa.
- Dialog `Goal Completed!` dapat muncul ketika target goal berhasil diselesaikan.

### 5.2 Create Goal Screen

Tujuan: membuat goal belajar atau kerja baru.

Elemen yang tampil:

1. Top bar:
   - Judul `Buat Goal Baru`
   - Tombol outlined `Back`

2. Card `Informasi Goal`:
   - Input `Judul goal`
   - Placeholder: `Contoh: Belajar Kotlin`
   - Input `Deskripsi (opsional)`
   - Input angka `Total target jam`
   - Suffix: `jam`

3. Card `Checklist Materi`:
   - Daftar input subtask dinamis
   - Label input: `Subtask 1`, `Subtask 2`, dan seterusnya
   - Placeholder: `Contoh: Dasar Kotlin`
   - Tombol teks `X` untuk menghapus subtask jika jumlah input lebih dari satu
   - Tombol teks full-width `+ Tambah Subtask`

4. Tombol utama:
   - `Simpan Goal`
   - Disabled sampai judul terisi dan total target jam lebih besar dari nol

### 5.3 Goal Detail Screen

Tujuan: melihat progres sebuah goal dan mengelola checklist materi.

Elemen yang tampil:

1. Top bar:
   - Judul menggunakan nama goal
   - Tombol outlined `Kembali`

2. Deskripsi goal:
   - Hanya tampil jika pengguna mengisi deskripsi

3. Card `Progress`:
   - Heading `Progress`
   - Linear progress bar
   - Teks `{jam selesai} / {total jam} jam`
   - Jika belum selesai: `Sisa {remaining hours} jam lagi`
   - Jika sudah selesai: `Selesai` dengan warna primary

4. Checklist materi:
   - Heading `Checklist Materi`, hanya tampil jika ada subtask
   - Checkbox untuk setiap subtask
   - Judul subtask
   - Teks subtask yang selesai menggunakan warna lebih redup

State error:

- Jika goal tidak ditemukan, tampil teks `Goal tidak ditemukan.` dan tombol `Kembali`.

### 5.4 Set Duration Screen

Tujuan: menentukan komitmen sesi fokus sebelum Focus Lock aktif.

Elemen yang tampil:

1. Top bar:
   - Judul `Set Duration`
   - Tombol outlined `Back`

2. Card `Pilih Goal (opsional)`:
   - Maksimal lima goal aktif ditampilkan
   - Setiap goal memakai outlined selectable card dengan tinggi `52dp`
   - Isi card: judul goal di kiri dan persentase progres di kanan
   - Goal terpilih memakai border primary lebih tebal dan teks semi-bold
   - Goal dapat ditekan kembali untuk membatalkan pilihan
   - Tombol teks `+ Buat Goal Baru`

3. Card `Pilih durasi fokus`:
   - Filter chip preset `25 menit`, `30 menit`, `60 menit`
   - Input angka `Custom duration`
   - Suffix: `menit`

4. Card ringkasan komitmen:
   - Jika goal dipilih: `Sesi untuk: {nama goal}`
   - Jika tanpa goal: `Tanpa goal - sesi bebas`
   - Durasi terpilih dalam angka besar primary: `{durasi} menit`
   - Penjelasan: `Komitmen ini akan masuk ke Focus Lock sampai timer selesai.`

5. Tombol utama:
   - `Start Session`

State kosong:

- Jika belum ada goal, card goal menampilkan:
  `Belum ada goal. Buat goal untuk melacak progres belajarmu.`

### 5.5 Permission Screen

Tujuan: memastikan permission wajib aktif sebelum memasuki sesi Focus Lock.

Layout menggunakan komposisi terpusat secara vertikal.

Elemen yang tampil:

1. Judul:
   - `Izinkan FutureFocus`

2. Penjelasan:
   - `FutureFocus membutuhkan akses berikut agar fitur Focus Lock berjalan maksimal`

3. Permission item `Layanan Aksesibilitas`:
   - Deskripsi: mendeteksi ketika pengguna membuka aplikasi lain dan membantu mengarahkan kembali
   - Status di kanan: `Aktif` berwarna primary atau `Nonaktif` berwarna error
   - Tombol outlined `Aktifkan` jika belum aktif

4. Permission item `Tampilan di Atas Aplikasi Lain`:
   - Deskripsi: menampilkan lapisan fokus di atas aplikasi lain selama sesi
   - Status di kanan: `Aktif` atau `Nonaktif`
   - Tombol outlined `Aktifkan` jika belum aktif

5. Tombol utama:
   - `Mulai Sesi Fokus`
   - Disabled sampai kedua permission aktif

6. Tombol outlined:
   - `Kembali`

### 5.6 Focus Lock Screen

Tujuan: menjaga pengguna tetap fokus sampai countdown selesai.

Layout fullscreen minimal dan terpusat.

Elemen utama:

1. Label:
   - `Focus Lock`

2. Circular progress indicator:
   - Stroke tebal `10dp`
   - Warna primary
   - Menunjukkan progres waktu yang sudah dilewati

3. Countdown di tengah lingkaran:
   - Format timer, misalnya `24:59`
   - Ukuran besar sekitar `44sp`
   - Bobot bold

4. Pesan motivasi:
   - `Tetap di sini. Satu sesi selesai lebih bernilai daripada banyak distraksi.`
   - Center aligned

State dialog exit attempt:

| Level | Judul dialog | Pesan | Quote | Aksi |
| --- | --- | --- | --- | --- |
| 1 | `Reflection Stage` | `Yakin ingin keluar sekarang? Kamu baru saja mulai, dan masih ada waktu untuk melanjutkan.` | Tidak ada | `Lanjut Fokus` |
| 2 | `Motivation Stage` | `Kamu sudah sejauh ini. Tetap lanjutkan dan beri dirimu kesempatan untuk selesai.` | Ada jika tersedia | `Lanjut Fokus` |
| 3 | `Consequence Stage` | `Usahamu akan sia-sia jika berhenti sekarang. Selesaikan komitmen yang kamu mulai.` | Ada jika tersedia | `Lanjut Fokus` |
| 4+ | `Emotional Reflection` | `Hari ini mungkin terasa berat, tapi kamu masih bisa memilih untuk menyelesaikannya.` | Ada jika tersedia | `Lanjut Fokus`, `Akhiri Sesi` |

Ketentuan desain dialog:

- Dialog tidak dapat ditutup dengan tap di luar.
- Quote tampil dengan tanda kutip dan bobot semi-bold.
- Nama author tampil lebih kecil dan redup di bawah quote.
- Aksi `Akhiri Sesi` hanya ada pada level 4 atau lebih.

### 5.7 Session Complete Screen

Tujuan: memberi apresiasi setelah sesi fokus berhasil diselesaikan.

Elemen yang tampil:

1. Heading besar primary:
   - `Great job!`

2. Subtitle:
   - `Kamu berhasil menyelesaikan sesi fokus.`

3. Card hasil sesi:
   - Label `Durasi selesai`
   - Durasi sesi dalam teks besar
   - Teks `Exit attempts: {jumlah}`

4. Card progress goal, hanya jika sesi terhubung dengan goal:
   - Heading `Progress Goal: {nama goal}`
   - Linear progress bar
   - Teks `{jam selesai} / {total jam} jam`

5. Card apresiasi:
   - Heading `Apresiasi`
   - Teks: `Komitmen kecil yang selesai hari ini membangun fokus yang lebih kuat besok.`

6. Tombol utama:
   - `Back to Home`

### 5.8 History Screen

Tujuan: menampilkan seluruh riwayat sesi fokus.

Elemen yang tampil:

1. Top bar:
   - Judul `History`
   - Tombol outlined `Back`

2. Lazy list card sesi:
   - Durasi sesi di kiri dalam teks besar bold
   - Status di kanan:
     - `Success` berwarna primary
     - `Failed` berwarna error
     - `Running` berwarna secondary
   - Tanggal dan waktu sesi dengan warna redup
   - Jika terhubung ke goal: `Goal: {nama goal}` dengan warna primary
   - `Exit attempts: {jumlah}`

State kosong:

- Card berisi `Belum ada sesi fokus.`
- Penjelasan: `Mulai sesi pertama untuk melihat riwayat di sini.`

### 5.9 Statistics Screen

Tujuan: menampilkan ringkasan pencapaian fokus.

Elemen yang tampil:

1. Top bar:
   - Judul `Statistics`
   - Tombol outlined `Back`

2. Stat tile:
   - Label `Total focus time`
   - Nilai total waktu fokus

3. Stat tile:
   - Label `Focus streak`
   - Nilai `{jumlah} hari`

4. Card `Daily goal`:
   - Teks `{waktu fokus hari ini} / {target harian}`
   - Linear progress bar

5. Card `Session result`:
   - Teks `Success: {jumlah}`
   - Progress bar success berwarna primary
   - Teks `Failed: {jumlah}`
   - Progress bar failed berwarna error

6. Card placeholder chart:
   - Heading `Chart placeholder`
   - Penjelasan:
     `Area ini disiapkan untuk grafik fokus harian saat data sudah lebih lengkap.`

Catatan untuk pengembangan desain:

- Area placeholder dapat dirancang sebagai grafik fokus harian atau mingguan.
- Tetap sediakan ruang yang konsisten dengan card lainnya.

## 6. Overlay Saat Membuka Aplikasi Lain

Overlay ini bukan screen Compose, tetapi tetap perlu dibuat pada desain karena muncul di atas aplikasi lain ketika pengguna mencoba berpindah aplikasi selama sesi.

Visual overlay:

- Fullscreen overlay hitam transparan sekitar 80%.
- Konten terpusat secara vertikal.
- Judul putih bold ukuran sekitar `24sp`: `Tetap Fokus!`
- Pesan putih transparan ukuran sekitar `16sp`, center aligned:
  `Anda sedang dalam sesi fokus. Kembalilah ke FutureFocus untuk melanjutkan.`

Overlay hanya bersifat visual sementara aplikasi mengarahkan pengguna kembali ke Focus Lock.

## 7. Notification Saat Focus Lock Aktif

Foreground notification tetap tampil selama sesi berjalan.

Konten:

- Title: `FutureFocus berjalan`
- Body jika durasi tersedia: `Sesi fokus {durasi} menit sedang aktif.`
- Body fallback: `Sesi fokus sedang aktif.`
- Notification bersifat ongoing dan tidak dirancang sebagai aksi interaktif.

## 8. State dan Variasi yang Perlu Dibuat dalam Desain

AI desain sebaiknya membuat variasi berikut agar hasil lengkap:

1. Home dengan active goals.
2. Home tanpa active goals.
3. Home dengan dialog edit daily goal.
4. Home dengan dialog sesi gagal.
5. Home dengan dialog goal completed.
6. Create Goal dengan satu subtask.
7. Create Goal dengan beberapa subtask.
8. Goal Detail dengan progres berjalan.
9. Goal Detail dengan goal selesai.
10. Set Duration tanpa goal.
11. Set Duration dengan goal dipilih.
12. Permission dengan permission belum aktif.
13. Permission dengan seluruh permission aktif.
14. Focus Lock normal.
15. Focus Lock dengan dialog exit level 1.
16. Focus Lock dengan dialog exit level 4 dan tombol akhiri sesi.
17. Session Complete tanpa goal.
18. Session Complete dengan progress goal.
19. History kosong.
20. History dengan beberapa sesi berstatus campuran.
21. Statistics.
22. Overlay fullscreen di atas aplikasi lain.

## 9. Instruksi Singkat untuk AI Pembuat Desain

Gunakan brief berikut jika dokumen ini diberikan kepada AI desain:

```text
Buat UI aplikasi Android bernama FutureFocus berdasarkan dokumen ini.
Gunakan gaya minimal, tenang, modern, dan fokus pada produktivitas.
Pertahankan palet hijau, lime, latar off-white, card putih, radius 8px,
serta hierarki informasi yang jelas. Jangan menambahkan fitur yang tidak
tercantum. Buat seluruh screen dan state penting, termasuk Focus Lock,
dialog exit bertingkat, permission state, empty state, session complete,
history, statistics, dan overlay ketika pengguna membuka aplikasi lain.
Gunakan komponen yang konsisten antar-screen dan prioritaskan tampilan
mobile Android portrait.
```

## 10. Catatan Implementasi Saat Ini

- Aplikasi masih berada pada tahap MVP.
- Data sesi, daily goal, goal, dan subtask disimpan melalui Firestore menggunakan user sementara `demo-user`.
- Quote motivasi dimuat dari ZenQuotes, disimpan lokal melalui Room, dan memiliki fallback lokal.
- Statistics belum memiliki grafik final.
- Firebase Authentication belum tersedia.
- Focus Lock menggunakan Accessibility Service, overlay, foreground service, dan recovery navigasi Android. Ini bukan mode kiosk tingkat sistem.

