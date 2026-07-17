package com.bearbones.kumaflow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.util.Locale

object AppStr {
    val isId get() = Locale.getDefault().language == "in" || Locale.getDefault().language == "id"
    val appLocked get() = if(isId) "Aplikasi Terkunci" else "App Locked"
    val secKuma get() = if(isId) "Keamanan KumaFlow" else "KumaFlow Security"
    val scanBio get() = if(isId) "Pindai sidik jari/wajah Anda" else "Scan your fingerprint or face"
    val usePin get() = if(isId) "Gunakan PIN" else "Use PIN"
    val wrongPin get() = if(isId) "PIN yang dimasukkan salah!" else "Incorrect PIN!"
    val curBal get() = if(isId) "Total Saldo" else "Total Balance"
    val inc get() = if(isId) "Pemasukan" else "Income"
    val exp get() = if(isId) "Pengeluaran" else "Expenses"
    val recTx get() = if(isId) "Daftar Transaksi" else "Transaction List"
    val noTx get() = if(isId) "Belum ada transaksi." else "No transactions found."
    val rep get() = if(isId) "Laporan" else "Report"
    val sum get() = if(isId) "Ringkasan" else "Summary"

    // Split Bill
    val splitBillCfg get() = if(isId) "Pengaturan Split Bill & QRIS" else "Split Bill & QRIS Settings"
    val splitBill get() = if(isId) "Split Bill" else "Split Bill"
    val splitBillCalc get() = if(isId) "Kalkulator Split Bill" else "Split Bill Calculator"
    val splitBillOrg get() = if(isId) "org" else "pax"
    val name get() = if(isId) "Nama" else "Name"
    val qrisImg get() = if(isId) "Gambar QRIS" else "QRIS Image"
    val bankName get() = if(isId) "Nama Bank" else "Bank Name"
    val bankAcc get() = if(isId) "Nomor Rekening" else "Account Number"
    val selectImg get() = if(isId) "Pilih Gambar" else "Select Image"
    val modeEqual get() = if(isId) "Bagi Rata" else "Equal Split"
    val modeCustom get() = if(isId) "Sesuai Pesanan" else "Itemized Split"
    val totalBill get() = if(isId) "Total Tagihan" else "Total Bill"
    val totalPeople get() = if(isId) "Jumlah Orang" else "Total People"
    val taxPct get() = if(isId) "Pajak %" else "Tax %"
    val eachPays get() = if(isId) "Tiap Orang Bayar" else "Each Person Pays"
    val previewQris get() = if(isId) "Pratinjau QRIS" else "Preview QRIS"
    val customItems get() = if(isId) "Daftar Pesanan" else "Custom Items"
    val pricePreTax get() = if(isId) "Harga (Sblm Pajak)" else "Price (Pre-tax)"
    val resultAfterTax get() = if(isId) "Hasil (Stlh Pajak & Pembulatan)" else "Results (After Tax & Rounding)"
    val remaining get() = if(isId) "Sisanya" else "Remaining"
    val shareWa get() = if(isId) "Kirim ke WhatsApp" else "Share to WhatsApp"
    val loadQrisFailed get() = if(isId) "Gagal memuat QRIS" else "Failed to load QRIS"
    val net get() = if(isId) "Tabungan Bersih" else "Net Savings"
    val targetProg get() = if(isId) "Progres Target Bulanan" else "Monthly Target Progress"
    val spendBreak get() = if(isId) "Rincian Pengeluaran" else "Spending Breakdown"
    val noData get() = if(isId) "Tidak Ada Data" else "No Data"
    val trends get() = if(isId) "Tren Bulanan" else "Monthly Trends"
    val noTrendData get() = if(isId) "Belum ada data bulanan" else "No monthly data yet"
    val set get() = if(isId) "Pengaturan" else "Settings"
    val accSec get() = if(isId) "Akun & Keamanan" else "Account & Security"
    val editProf get() = if(isId) "Edit Nama" else "Edit Name"
    val appLck get() = if(isId) "Kunci Aplikasi" else "App Lock"
    val finPref get() = if(isId) "Preferensi Keuangan" else "Financial Preference"
    val cur get() = if(isId) "Mata Uang" else "Currency"
    val date get() = if(isId) "Tanggal" else "Date"
    val tar get() = if(isId) "Target Global" else "Global Target"
    val catBudget get() = if(isId) "Budget Kategori" else "Category Budget"

    const val VERSION = "v6.3.1"
    val dat get() = if(isId) "Data" else "Data"
    val expPdf get() = if(isId) "Ekspor ke PDF" else "Export to PDF"
    val expCsv get() = if(isId) "Ekspor ke CSV" else "Export to CSV"
    val expDrive get() = if(isId) "Ekspor ke Drive" else "Export to Drive"
    val backApp get() = if(isId) "Cadangkan Aplikasi" else "Backup App"
    val rest get() = if(isId) "Pulihkan Data" else "Restore"
    val abt get() = if(isId) "Tentang" else "About"
    val appVer get() = if(isId) "Versi Aplikasi" else "App Version"
    val priv get() = if(isId) "Kebijakan Privasi" else "Privacy Policy"
    val trms get() = if(isId) "Syarat & Ketentuan" else "Terms"
    val contDev get() = if(isId) "Hubungi Pengembang" else "Contact Developer"
    val save get() = if(isId) "Simpan" else "Save"
    val usr get() = if(isId) "Nama Pengguna" else "Username"
    val selCur get() = if(isId) "Pilih Mata Uang" else "Select Currency"
    val setTar get() = if(isId) "Set Target Bulanan" else "Set Monthly Target"
    val limExp get() = if(isId) "Limit Pengeluaran" else "Expense Limit"
    val btnSet get() = if(isId) "Atur" else "Set"
    val selDat get() = if(isId) "Pilih Format Tanggal" else "Select Date Format"
    val setPin get() = if(isId) "Atur PIN 6 Digit" else "Set 6-Digit PIN"
    val confPin get() = if(isId) "Konfirmasi PIN" else "Confirm PIN"
    val pinAct get() = if(isId) "Sistem Keamanan Aktif!" else "Security System Activated!"
    val pinDeact get() = if(isId) "Sistem Keamanan Dinonaktifkan!" else "Security System Disabled!"
    val info get() = if(isId) "Informasi KumaFlow" else "KumaFlow Info"
    val close get() = if(isId) "Tutup" else "Close"
    val privDesc get() = if(isId) "Semua data transaksi dan profil disimpan secara lokal di perangkat Anda. KumaFlow tidak membagikan data Anda ke server eksternal, sehingga privasi Anda 100% aman." else "All data is stored locally on your device. KumaFlow does not share your data to external servers, ensuring 100% privacy."
    val gotIt get() = if(isId) "Mengerti" else "Got it"
    val termDesc get() = if(isId) "1. Penggunaan aplikasi sepenuhnya tanggung jawab pengguna.\n\n2. Karena data disimpan secara lokal (offline), kehilangan perangkat berarti kehilangan data kecuali Anda rutin melakukan pencadangan.\n\n3. Pengembang tidak bertanggung jawab atas kerugian finansial pengguna." else "1. Use of this app is strictly the user's responsibility.\n\n2. Data is stored offline. Losing your device means losing your data unless backed up regularly.\n\n3. Developers are not liable for any financial discrepancies."
    val agree get() = if(isId) "Setuju" else "Agree"
    val home get() = if(isId) "Beranda" else "Home"
    val hist get() = if(isId) "Riwayat" else "History"
    val addTx get() = if(isId) "Tambah Transaksi" else "Add New Transaction"
    val editTx get() = if(isId) "Edit Transaksi" else "Edit Transaction"
    val cat get() = if(isId) "Kategori" else "Category"
    val nme get() = if(isId) "Judul Transaksi" else "Transaction Title"
    val msgInp get() = if(isId) "Catatan Tambahan (Opsional)" else "Notes (Optional)"
    val amt get() = if(isId) "Jumlah" else "Amount"
    val saveTx get() = if(isId) "Simpan Transaksi" else "Save Transaction"
    val txSaved get() = if(isId) "Transaksi Disimpan!" else "Transaction Saved!"
    val resOk get() = if(isId) "Restore Data Berhasil!" else "Data Restored Successfully!"
    val resFail get() = if(isId) "Gagal Restore: File korup/salah format" else "Restore Failed: Corrupted/invalid file"
    val repPdf get() = if(isId) "LAPORAN TRANSAKSI" else "TRANSACTION REPORT"
    val sharePdf get() = if(isId) "Bagikan PDF" else "Share PDF"
    val failPdf get() = if(isId) "Gagal membuat PDF" else "Failed to generate PDF"
    val type get() = if(isId) "Tipe" else "Type"
    val shareCsv get() = if(isId) "Bagikan CSV" else "Share CSV"
    val failCsv get() = if(isId) "Gagal membuat CSV" else "Failed to generate CSV"
    val noDrive get() = if(isId) "Google Drive tidak ditemukan, membuka bagikan standar..." else "Google Drive not found, opening standard share..."
    val saveBak get() = if(isId) "Simpan File Backup (.kuma)" else "Save Backup File (.kuma)"
    val failBak get() = if(isId) "Gagal membackup aplikasi" else "Failed to backup app"
    val noFileMgr get() = if(isId) "Aplikasi File Manager tidak ditemukan" else "File Manager app not found"
    val theme get() = if(isId) "Tema Tampilan" else "App Theme"
    val themeSys get() = if(isId) "Ikuti Sistem" else "Use System Setting"
    val themeDark get() = if(isId) "Mode Gelap" else "Dark Mode"
    val themeLight get() = if(isId) "Mode Terang" else "Light Mode"
    val amoledDark get() = if(isId) "AMOLED Gelap" else "AMOLED Dark"
    val amoledDesc get() = if(isId) "Latar belakang hitam murni" else "Pure black background"
    val liquidGlass get() = if(isId) "Kuma Glass UI" else "Kuma Glass UI"
    val liquidGlassDesc get() = if(isId) "Efek kaca transparan premium" else "Premium transparent glass effect"
    val edit get() = if(isId) "Edit" else "Edit"
    val delete get() = if(isId) "Hapus" else "Delete"
    val delConf get() = if(isId) "Yakin hapus transaksi ini?" else "Delete this transaction?"
    val yes get() = if(isId) "Ya, Hapus" else "Yes, Delete"
    val no get() = if(isId) "Batal" else "Cancel"
    val notif get() = if(isId) "Notifikasi & Pengingat" else "Notifications & Reminders"
    val dailyRem get() = if(isId) "Pengingat 5 Waktu" else "5 Times Reminder"
    val rem get() = if(isId) "Pengingat" else "Reminder"
    val searchTx get() = if(isId) "Cari Transaksi..." else "Search Transactions..."
    val carryOver get() = if(isId) "Bawa Saldo Bulan Lalu" else "Carry-Over Balance"
    val manageCat get() = if(isId) "Kelola Kategori" else "Manage Categories"
    val addCat get() = if(isId) "+ Tambah Kategori" else "+ Add Category"
    val wallet get() = if(isId) "Sumber Dana / Dompet" else "Wallet / Source"
    val walletShort get() = if(isId) "Dompet" else "Wallet"
    val manageWallet get() = if(isId) "Kelola Dompet" else "Manage Wallets"
    val addWallet get() = if(isId) "+ Tambah Dompet" else "+ Add Wallet"
    val splitSource get() = if(isId) "Sumber Dana (Split Payment)" else "Funding Source (Split)"
    val addOtherWallet get() = if(isId) "Tambah Dompet Lain" else "Add Another Wallet"
    val total get() = if(isId) "Total" else "Total"
    val multiWallet get() = if(isId) "Multi-Dompet" else "Multi-Wallet"
    val mutasi get() = if(isId) "Mutasi" else "Transfer"
    val tarikDari get() = if(isId) "Tarik Dari" else "Withdraw From"
    val simpanKe get() = if(isId) "Simpan Ke" else "Deposit To"
    val jumlahPindah get() = if(isId) "Jumlah Dipindah" else "Transfer Amount"
    val totalTransfer get() = if(isId) "Total Transfer" else "Total Transfer"
    val defMutasiTitle get() = if(isId) "Tarik Tunai / Mutasi" else "Cash Withdrawal / Transfer"
    val newWallet get() = if(isId) "Dompet Baru" else "New Wallet"
    val walletName get() = if(isId) "Nama Dompet" else "Wallet Name"
    val newCat get() = if(isId) "Kategori Baru" else "New Category"
    val catName get() = if(isId) "Nama Kategori" else "Category Name"
    val chooseIcon get() = if(isId) "Pilih Ikon:" else "Choose Icon:"
    val chooseCatIcon get() = if(isId) "Pilih Ikon Kategori:" else "Choose Category Icon:"
    val backupReminderTitle get() = if(isId) "Pengingat Pencadangan Data" else "Data Backup Reminder"
    val backupReminderMsg get() = if(isId) "Data Anda semakin bertambah. Lakukan pencadangan berkala untuk mencegah kehilangan data." else "Your data is growing. Please backup regularly to prevent data loss."
    val backupNow get() = if(isId) "Backup Sekarang" else "Backup Now"
    val later get() = if(isId) "Nanti Saja" else "Later"
    val optDb get() = if(isId) "Optimasi Database" else "Optimize Database"
    val optSuccess get() = if(isId) "Penyimpanan berhasil dioptimasi." else "Storage optimized."
    val optFail get() = if(isId) "Gagal optimasi: " else "Optimization failed: "
    val cancelBulk get() = if(isId) "Batal Massal" else "Cancel Selection"
    val changeCat get() = if(isId) "Ubah Kategori" else "Change Category"
    val selected get() = if(isId) "Dipilih" else "Selected"
    val bulkDel get() = if(isId) "Hapus Massal" else "Bulk Delete"
    val chooseNewCat get() = if(isId) "Pilih Kategori Baru" else "Choose New Category"
    val activeFreeze get() = if(isId) "Pembeku Aktif" else "Active Freezes"
    val progToNextFreeze get() = if(isId) "Progres ke Pembeku Baru" else "Progress to Next Freeze"
    val shareMilestone get() = if(isId) "Bagikan Pencapaian" else "Share Milestone"

    // NewUserDialog
    val infoReminder get() = if(isId) "Informasi Pengingat Sistem" else "System Reminder Information"
    val infoReminderDesc get() = if(isId) "Agar KumaFlow dapat mengingatkan pencatatan, notifikasi \"KumaFlow Aktif\" akan muncul di layar." else "To ensure KumaFlow reminds you to record expenses, a \"KumaFlow Active\" notification will remain visible."
    val sysProtector get() = if(isId) "Pelindung Sistem" else "System Protector"
    val sysProtectorDesc get() = if(isId) "Sistem siaga agar alarm tidak dimatikan paksa oleh sistem operasi perangkat." else "Standby mode to prevent the operating system from force-closing the alarm."
    val doNotDismiss get() = if(isId) "Jangan Dihapus" else "Do Not Dismiss"
    val doNotDismissDesc get() = if(isId) "Mohon biarkan notifikasi ini. Jika dihapus, pengingat berisiko berhenti berfungsi." else "Please keep this notification active. Dismissing it may cause reminders to fail."
    val battEfficiency get() = if(isId) "Efisiensi Baterai" else "Battery Efficiency"
    val battEfficiencyDesc get() = if(isId) "Sistem ini dioptimalkan dan tidak akan menguras daya baterai perangkat Anda." else "This system is highly optimized and will not drain your device's battery."
    val understandCont get() = if(isId) "Mengerti & Lanjutkan" else "Understand & Continue"

    // ReportScreen
    val repGreat get() = if(isId) "Pemasukan Anda lebih besar dari pengeluaran. Pertahankan performa keuangan ini." else "Your income is larger than your expenses. Keep up this good financial performance."
    val repWarn get() = if(isId) "Peringatan: Pengeluaran bulan ini melebihi pemasukan. Mohon kurangi pengeluaran sekunder." else "Warning: Your expenses exceed your income this month. Please reduce secondary spending."
    val repTop get() = if(isId) "Pengeluaran terbesar Anda bulan ini adalah pada kategori %1\$s (%2\$d%%). Harap perhatikan batas anggaran Anda." else "Your largest expense this month is %1\$s (%2\$d%%). Please mind your budget limit."
    val repNoData get() = if(isId) "Belum ada transaksi bulan ini. Silakan mulai pencatatan." else "No transactions this month. Please start tracking."
    val repStable get() = if(isId) "Keuangan Anda stabil bulan ini. Kinerja yang baik." else "Your finances are stable this month. Good performance."

    val importantBattery get() = if(isId) "PENTING: Pengaturan Baterai & Auto-Launch" else "IMPORTANT: Battery & Auto-Launch Settings"
    val openBatterySet get() = if(isId) "Buka Pengaturan Baterai" else "Open Battery Settings"
    val errRestore get() = if(isId) "Error Restore:" else "Restore Error:"
    val reqNotifAcc get() = if(isId) "Mohon berikan izin Notification Access ke KumaFlow" else "Please grant Notification Access to KumaFlow"
    val errOpenBat get() = if(isId) "Tidak dapat membuka pengaturan baterai" else "Cannot open battery settings"
    val autoNotifTracker get() = if(isId) "Auto Notif Tracker" else "Auto Notif Tracker"

    val smartInsights get() = if(isId) "Smart Insights ?" else "Smart Insights ?"
    val showLess get() = if(isId) "Tampilkan Lebih Sedikit" else "Show Less"
    val showMore get() = if(isId) "Tampilkan Lebih Banyak" else "Show More"
    val pinLabel get() = if(isId) "PIN" else "PIN"
    val okBtn get() = if(isId) "OK" else "OK"
    fun wrappedComingSoon(month: String, year: String) = if(isId) "Wrapped $month $year Segera Hadir!" else "Wrapped $month $year is Coming Soon!"
    
    fun txDeleted(count: Int) = if(isId) "$count transaksi dihapus" else "$count transactions deleted"
    fun txChangedTo(count: Int, cat: String) = if(isId) "$count transaksi diubah ke $cat" else "$count transactions changed to $cat"
    
    val totalBalanceTitle get() = if(isId) "Total Saldo" else "Total Balance"
    val rouletteOptionNameHint get() = if(isId) "Nama Opsi" else "Option Name"
    val rouletteNominalHint get() = if(isId) "Rp Nominal" else "Rp Amount"
    val rouletteAddBtn get() = if(isId) "Tambah" else "Add"
    val rouletteNoOptions get() = if(isId) "Tidak ada opsi di bawah batas anggaran" else "No options under budget limit"
    val rouletteSpinHint get() = if(isId) "Putar Roda!" else "Spin the Wheel!"
    val rouletteWinnerTitle get() = if(isId) "Hasil Putaran" else "Spin Result"
    val rouletteWinnerMsg get() = if(isId) "Roda memilih:" else "The wheel chose:"
    val rouletteConfirmBtn get() = if(isId) "Catat Transaksi" else "Record Transaction"
    val rouletteCancelBtn get() = if(isId) "Batal" else "Cancel"
    
    val versionInfo get() = if(isId) "Versi: $VERSION\nBuild: Beta\nTipe: Standalone Local" else "Version: $VERSION\nBuild: Core Edition\nType: Standalone Local"
    val updAvail get() = if(isId) "Pembaruan Tersedia" else "Update Available"
    val newVerRdy get() = if(isId) "Versi baru sudah siap" else "New version is ready"
    val currVer get() = if(isId) "Saat ini" else "Current"
    val newVer get() = if(isId) "Baru" else "New"
    val laterBtn get() = if(isId) "Nanti Saja" else "Later"
    val downloadBtn get() = if(isId) "Unduh" else "Download"
    val retryBtn get() = if(isId) "Coba Lagi" else "Retry"
    val downloading get() = if(isId) "Mengunduh..." else "Downloading..."
    val cancelBtn get() = if(isId) "Batal" else "Cancel"
    val dlDone get() = if(isId) "Unduhan Selesai!" else "Download Complete!"
    val openingInst get() = if(isId) "Membuka installer..." else "Opening installer..."
    val daysStreak get() = if(isId) "Hari Beruntun!" else "Days Streak!"

    // Virtual Wallets (Zero-Based Budgeting)
    val virtualWalletsTitle get() = if(isId) "Dompet Virtual" else "Virtual Wallets"
    val unallocatedFundsTitle get() = if(isId) "Dana Belum Dialokasikan" else "Unallocated Funds"
    val allocateWarning get() = if(isId) "Mohon alokasikan dana Anda yang tersisa ke dalam dompet virtual." else "Please kindly allocate your remaining funds into your virtual wallets."
    val allocateFundsTitle get() = if(isId) "Alokasikan Dana" else "Allocate Funds"
    val selectTargetWallet get() = if(isId) "Pilih dompet tujuan" else "Select target wallet"
    val enterAmount get() = if(isId) "Masukkan nominal" else "Enter amount"
    val allocateExceedError get() = if(isId) "Mohon maaf, nominal yang Anda masukkan melebihi batas dana yang tersedia." else "We sincerely apologize, the amount entered exceeds your available funds."
    val fromVirtualWallet get() = if(isId) "Dari Dompet Virtual" else "From Virtual Wallet"
    val virtualWalletInsufficientError get() = if(isId) "Mohon maaf, saldo dompet virtual Anda tidak mencukupi untuk transaksi ini. Apakah Anda berkenan mengambil dari dompet virtual lain?" else "We apologize, your virtual wallet balance is insufficient for this transaction. Would you like to allocate from another virtual wallet?"

    // Kuma Roulette
    val rouletteHeader get() = if(isId) "Bingung Menentukan Pilihan? Mari Putar Rodanya!" else "Having Trouble Deciding? Let's Spin the Wheel!"
    val rouletteBudgetLabel get() = if(isId) "Batas Anggaran Anda" else "Your Maximum Budget"
    val rouletteEmptyWallet get() = if(isId) "Mohon maaf, saldo dompet Anda belum mencukupi." else "We sincerely apologize, your wallet balance is insufficient."
    val rouletteAddOptionHint get() = if(isId) "Masukkan pilihan Anda" else "Enter your choice"
    val rouletteAddAnother get() = if(isId) "+ Tambah Opsi Lain" else "+ Add Another Option"
    val rouletteDestiny get() = if(isId) "Takdir Anda hari ini:" else "Your destiny today:"
    val rouletteRecordAuto get() = if(isId) "Catat Pengeluaran Otomatis" else "Record Expense Automatically"
    val rouletteIconText get() = if(isId) "Roulette / Dadu" else "Roulette / Dice"
}

// ... [KumaIconLibrary] ...
val kumaIconLibrary = mapOf(
    "Kategori" to Icons.Default.DashboardCustomize,
    "Lainnya" to Icons.Default.MoreHoriz,
    "Bintang" to Icons.Default.Star,
    "Favorit" to Icons.Default.Favorite,
    "Bookmark" to Icons.Default.Bookmark,
    "Selesai" to Icons.Default.TaskAlt,
    "Makanan" to Icons.Default.Restaurant,
    "Fastfood" to Icons.Default.Fastfood,
    "Kopi / Nongkrong" to Icons.Default.LocalCafe,
    "Minuman / Boba" to Icons.Default.LocalDrink,
    "Bar / Party" to Icons.Default.LocalBar,
    "Teh / Hangat" to Icons.Default.EmojiFoodBeverage,
    "Cemilan / Jajanan" to Icons.Default.Tapas,
    "Es Krim" to Icons.Default.Icecream,
    "Roti / Kue" to Icons.Default.Cake,
    "Pizza" to Icons.Default.LocalPizza,
    "Buah & Sayur" to Icons.Default.Eco,
    "Mie / Ramen" to Icons.Default.RamenDining,
    "Sup / Kuah" to Icons.Default.SoupKitchen,
    "Set Menu" to Icons.Default.SetMeal,
    "Rice Bowl" to Icons.Default.RiceBowl,
    "Gofood / Delivery" to Icons.Default.DeliveryDining,
    "Sarapan" to Icons.Default.BreakfastDining,
    "Makan Malam" to Icons.Default.DinnerDining,
    "Piknik / Bekal" to Icons.Default.TakeoutDining,
    "Mobil Pribadi" to Icons.Default.DirectionsCar,
    "Motor" to Icons.Default.TwoWheeler,
    "Sepeda" to Icons.Default.PedalBike,
    "Pesawat / Tiket" to Icons.Default.Flight,
    "KRL / Kereta" to Icons.Default.Train,
    "Kereta Cepat" to Icons.Default.DirectionsRailway,
    "Bus / Travel" to Icons.Default.DirectionsBus,
    "Ojol / Taksi" to Icons.Default.LocalTaxi,
    "Kapal / Feri" to Icons.Default.DirectionsBoat,
    "Tram" to Icons.Default.Tram,
    "Skuter" to Icons.Default.ElectricScooter,
    "Bensin / SPBU" to Icons.Default.LocalGasStation,
    "Cas Mobil Listrik" to Icons.Default.EvStation,
    "Tol / Parkir" to Icons.Default.LocalParking,
    "Jalan Kaki" to Icons.Default.DirectionsWalk,
    "Bengkel / Servis" to Icons.Default.Build,
    "Cuci Kendaraan" to Icons.Default.LocalCarWash,
    "Bagasi / Koper" to Icons.Default.Luggage,
    "Paspor / Visa" to Icons.Default.AirplaneTicket,
    "Belanja / Grosir" to Icons.Default.ShoppingBag,
    "Supermarket" to Icons.Default.ShoppingCart,
    "Mall / Thrift" to Icons.Default.LocalMall,
    "Pakaian / Baju" to Icons.Default.Checkroom,
    "Sepatu / Sneakers" to Icons.Default.Snowshoeing,
    "Aksesoris / Jam" to Icons.Default.Watch,
    "Kacamata" to Icons.Default.FaceRetouchingNatural,
    "Skincare / Makeup" to Icons.Default.Brush,
    "Cukur Rambut" to Icons.Default.ContentCut,
    "Spa / Pijat" to Icons.Default.Spa,
    "Toko Kelontong" to Icons.Default.Storefront,
    "Gaji / Uang Masuk" to Icons.Default.AttachMoney,
    "Bank / Mutasi" to Icons.Default.AccountBalance,
    "ATM / Tarik Tunai" to Icons.Default.Atm,
    "Dompet / E-Wallet" to Icons.Default.AccountBalanceWallet,
    "Kartu Kredit / Paylater" to Icons.Default.CreditCard,
    "Tagihan / Bon" to Icons.Default.Receipt,
    "Pajak" to Icons.Default.ReceiptLong,
    "Investasi / Saham" to Icons.Default.TrendingUp,
    "Crypto / Koin" to Icons.Default.CurrencyBitcoin,
    "Tabungan" to Icons.Default.Savings,
    "Brankas" to Icons.Default.Lock,
    "Diskon / Promo" to Icons.Default.Loyalty,
    "Asuransi" to Icons.Default.Shield,
    "Cicilan" to Icons.Default.RequestQuote,
    "Rumah" to Icons.Default.Home,
    "Kosan / Apartemen" to Icons.Default.Apartment,
    "Listrik / Token" to Icons.Default.Bolt,
    "Air / Galon" to Icons.Default.WaterDrop,
    "Internet / Wifi" to Icons.Default.Wifi,
    "Router / Modem" to Icons.Default.Router,
    "Paket Data / Pulsa" to Icons.Default.PhoneIphone,
    "Telepon" to Icons.Default.Call,
    "Gas / Dapur" to Icons.Default.Propane,
    "Alat Kebersihan" to Icons.Default.CleaningServices,
    "Sapu / Pel" to Icons.Default.Sanitizer,
    "Laundry / Cuci Baju" to Icons.Default.LocalLaundryService,
    "Setrika" to Icons.Default.Iron,
    "Perabotan Rumah" to Icons.Default.Chair,
    "Kamar Tidur" to Icons.Default.Bed,
    "Kipas / AC" to Icons.Default.AcUnit,
    "Panci / Wajan" to Icons.Default.Kitchen,
    "Kesehatan / Dokter" to Icons.Default.MedicalServices,
    "Obat / Apotek" to Icons.Default.Medication,
    "Rumah Sakit" to Icons.Default.LocalHospital,
    "Vitamin / Vaksin" to Icons.Default.Vaccines,
    "Gigi / Dokter Gigi" to Icons.Default.CleanHands,
    "Mata / Optik" to Icons.Default.Visibility,
    "Gym / Fitness" to Icons.Default.FitnessCenter,
    "Suplemen / Whey" to Icons.Default.MonitorWeight,
    "Lari / Jogging" to Icons.Default.DirectionsRun,
    "Basket" to Icons.Default.SportsBasketball,
    "Sepak Bola" to Icons.Default.SportsSoccer,
    "Tenis" to Icons.Default.SportsTennis,
    "Berenang" to Icons.Default.Pool,
    "Beladiri" to Icons.Default.SportsMartialArts,
    "Yoga / Meditasi" to Icons.Default.SelfImprovement,
    "Game / Mabar" to Icons.Default.SportsEsports,
    "Top Up / Konsol" to Icons.Default.VideogameAsset,
    "Gacha / Dadu" to Icons.Default.Casino,
    "Film / Bioskop" to Icons.Default.Movie,
    "Streaming / Nonton" to Icons.Default.Subscriptions,
    "YouTube / Video" to Icons.Default.VideoLibrary,
    "Musik / Spotify" to Icons.Default.MusicNote,
    "Audio / IEM" to Icons.Default.Headphones,
    "Speaker" to Icons.Default.Speaker,
    "Konser / Gig" to Icons.Default.LibraryMusic,
    "Gitar / Alat Musik" to Icons.Default.Piano,
    "Buku / Komik" to Icons.Default.AutoStories,
    "Fotografi" to Icons.Default.PhotoCamera,
    "Seni / Melukis" to Icons.Default.Palette,
    "Berkebun" to Icons.Default.Forest,
    "Tech / PC" to Icons.Default.Computer,
    "Laptop" to Icons.Default.Laptop,
    "Gadget / Aksesoris" to Icons.Default.Devices,
    "Keyboard / Mouse" to Icons.Default.Keyboard,
    "Coding / Software" to Icons.Default.Code,
    "Server / Hosting" to Icons.Default.Dns,
    "Cloud / Backup" to Icons.Default.Cloud,
    "Kerja / Kantor" to Icons.Default.Work,
    "Bisnis / Usaha" to Icons.Default.BusinessCenter,
    "Meeting / Zoom" to Icons.Default.VideoCall,
    "Sekolah / Kampus" to Icons.Default.School,
    "Buku Pelajaran" to Icons.Default.MenuBook,
    "SPP / Pendidikan" to Icons.Default.Science,
    "Edukasi Online" to Icons.Default.CastForEducation,
    "Sertifikat / Lulus" to Icons.Default.WorkspacePremium,
    "Ayang / Kencan" to Icons.Default.Favorite,
    "Keluarga" to Icons.Default.Diversity3,
    "Anak / Adik" to Icons.Default.ChildCare,
    "Teman / Circle" to Icons.Default.Groups,
    "Kucing / Anjing" to Icons.Default.Pets,
    "Kondangan / Hadiah" to Icons.Default.CardGiftcard,
    "Pesta / Ulang Tahun" to Icons.Default.Celebration,
    "Sedekah / Donasi" to Icons.Default.VolunteerActivism,
    "Masjid / Ibadah" to Icons.Default.Mosque,
    "Gereja / Ibadah" to Icons.Default.Church,
    "Pura / Vihara" to Icons.Default.TempleBuddhist,
    "Liburan / Staycation" to Icons.Default.BeachAccess,
    "Hotel / Penginapan" to Icons.Default.Hotel,
    "Camping / Alam" to Icons.Default.Terrain,
    "Cuaca / Musim" to Icons.Default.WbSunny,
    "Seneng / Bahagia" to Icons.Default.SentimentSatisfied,
    "Marah / Emosi" to Icons.Default.SentimentVeryDissatisfied
)
