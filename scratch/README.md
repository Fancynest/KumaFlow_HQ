# prepaid_lib_flutter_null_safety

A Flutter plugin library untuk membaca dan update saldo (top up) kartu e-money prabayar Indonesia (Mandiri, BNI, BCA, DKI) menggunakan NFC. Library ini menyediakan API sederhana untuk integrasi fitur cek saldo dan top up e-money ke aplikasi Flutter Anda.

## Fitur
- Membaca info kartu (UID, nomor kartu, saldo, nama bank)
- Update saldo (top up) untuk kartu e-money yang didukung
- Mendapatkan riwayat transaksi (Mandiri, BNI)
- Berbasis NFC, mendukung Android dan iOS (dengan NFC support)
- Null safety

## Persyaratan (Requirements)
- Flutter SDK >= 3.1.5
- Android dengan NFC (API 19+)
- iOS dengan NFC (iPhone 7 ke atas, iOS 13+)
- Izin penggunaan NFC

### Dukungan Android
- Minimum SDK: 19 (Android 4.4 KitKat)
- Pastikan perangkat memiliki fitur NFC dan NFC dalam keadaan aktif.
- Tambahkan permission berikut pada `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.NFC" />
  <uses-feature android:name="android.hardware.nfc" android:required="true" />
  ```
- Tidak semua perangkat Android memiliki NFC. Pastikan perangkat user kompatibel.
- Untuk pengalaman terbaik, gunakan perangkat dengan Android 6.0 ke atas.

### Dukungan iOS
- Hanya iPhone 7 atau lebih baru yang mendukung NFC untuk aplikasi pihak ketiga.
- Minimum iOS 13.
- Aktifkan capability NFC di Xcode (Signing & Capabilities > Near Field Communication Tag Reading).
- Tambahkan deskripsi penggunaan NFC di `Info.plist`:
  ```xml
  <key>NFCReaderUsageDescription</key>
  <string>Aplikasi ini menggunakan NFC untuk membaca dan update saldo kartu e-money.</string>
  ```
- Pada iOS, user harus memberikan izin akses NFC saat pertama kali digunakan.
- Proses NFC di iOS biasanya lebih interaktif (ada pop-up sistem).

## Kartu yang Didukung
- Mandiri e-money
- BNI TapCash
- BCA Flazz
- DKI JakCard (NEW)

## Instalasi
Tambahkan ke `pubspec.yaml` Anda:

```yaml
dependencies:
  prepaid_lib_flutter_null_safety: ^1.0.0
```

Lalu jalankan:
```sh
flutter pub get
```

### Setup Android
- Pastikan sudah mengikuti langkah pada bagian "Dukungan Android" di atas.
- Tidak perlu konfigurasi tambahan selain permission dan feature NFC.

### Setup iOS
- Pastikan sudah mengikuti langkah pada bagian "Dukungan iOS" di atas.
- Buka project iOS di Xcode, aktifkan capability NFC, dan tambahkan usage description.

## Penggunaan

### 1. Import Library
```dart
import 'package:prepaid_lib_flutter_null_safety/unik_lib_flutter.dart';
```

### 2. Inisialisasi Library
Wajib dilakukan sebelum menggunakan fitur NFC.
```dart
bool isSuccess = await UnikLibFlutter.initUnikLib("YOUR_MID", 0); // 0=dev, 1=prod
```

### 3. Cek Info Kartu (Cek Saldo)
```dart
List<String> cardUid = [''];
List<String> cardNumber = [''];
List<String> balance = [''];
List<String> bankName = [''];

bool isSuccess = await UnikLibFlutter.getCardInfo(
  cardUid, cardNumber, balance, bankName,
  startPooling: true,
  callbackTimeout: (isTimeout) => print("Timeout: $isTimeout"),
  errorNfc: (nfcError) => print("NFC Error: $nfcError"),
);

print('Card: \\${cardNumber[0]}, Balance: \\${balance[0]}, Bank: \\${bankName[0]}');
```

### 4. Update Saldo (Top Up)
```dart
List<String> status = [''];
List<String> cardNumber = [''];
List<String> balance = [''];
List<String> bankName = [''];
List<String> beforeBalance = [''];

bool isSuccess = await UnikLibFlutter.updateBalance(
  status, cardNumber, balance, bankName, beforeBalance,
  "your_email@example.com", "08123456789",
  callbackState: (state) {
    if (state == UnikLibFlutter.WAITING_STATUS) {
      UnikLibFlutter.setIosMessage("Mohon tunggu, jangan lepaskan kartu");
    }
  },
  callbackTimeout: (isTimeout) => print("Timeout: $isTimeout"),
  errorNfc: (nfcError) => print("NFC Error: $nfcError"),
);

if (isSuccess) {
  print("Update balance sukses");
} else {
  print("Update balance gagal: \\${status[0]}");
}
```

### 5. Cek Riwayat Transaksi
```dart
List<String> history = [''];
bool isSuccess = await UnikLibFlutter.getHistory(history);
if (isSuccess) {
  print("History: \\${history[0]}");
}
```

## Contoh Lengkap
Lihat [`example/lib/main.dart`](example/lib/main.dart) untuk aplikasi contoh yang siap pakai.

## Troubleshooting
- Pastikan NFC aktif di perangkat.
- Di iOS, hanya iPhone 7 ke atas yang didukung.
- Jika terjadi timeout atau error NFC, pastikan kartu ditempelkan dengan benar dan perangkat mendukung NFC.
- Untuk produksi, gunakan MID terdaftar dan set env ke 1.

## Lisensi
MIT

