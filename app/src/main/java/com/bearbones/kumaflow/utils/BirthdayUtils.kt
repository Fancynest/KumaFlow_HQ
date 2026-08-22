package com.bearbones.kumaflow.utils

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

object BirthdayUtils {

    fun isBirthdayToday(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
        val dob = sharedPref.getString("user_dob", "") ?: ""
        if (dob.length >= 5) {
            val parts = dob.split("-")
            if (parts.size >= 2) {
                try {
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val today = LocalDate.now()
                    return today.dayOfMonth == day && today.monthValue == month
                } catch (e: Exception) {
                    return false
                }
            }
        }
        return false
    }

    data class BirthdayContent(val title: String, val message: String)

    fun getBirthdayContent(context: Context, isId: Boolean, displayName: String): BirthdayContent {
        val messagesId = listOf(
            "Selamat ulang tahun! Kami menyadari bahwa hari ini mungkin terasa seperti hari biasa pada umumnya. Namun, kami ingin mengingatkan bahwa kehadiranmu sangat berarti. Terima kasih telah bertahan dan terus melangkah sejauh ini. Hari ini, lupakan sejenak tentang catatan pengeluaran, dan berikan dirimu apresiasi yang pantas kamu dapatkan. KumaFlow akan selalu di sini untuk menemani perjalanan finansialmu.",
            "Selamat bertambah umur! Meskipun KumaFlow hanyalah sebuah aplikasi di perangkatmu, kami dengan tulus berharap hari ini membawa kedamaian dan kebahagiaan untukmu. Tidak perlu memaksakan diri untuk selalu sempurna setiap saat. Nikmati waktumu hari ini, tarik napas panjang, dan ketahuilah bahwa kamu telah melakukan yang terbaik. Semoga hal-hal baik selalu menyertaimu.",
            "Selamat ulang tahun! Perjalanan menuju kemandirian finansial memang tidak selalu mudah. Namun, melihatmu terus berusaha adalah sesuatu yang luar biasa. Di hari spesial ini, kami harap kamu bisa meluangkan waktu sejenak untuk mensyukuri hal-hal kecil dan mengistirahatkan pikiran. KumaFlow akan selalu mendukung setiap langkahmu.",
            "Selamat ulang tahun! Kehidupan terkadang berjalan terlalu cepat, membuat kita lupa untuk menghargai diri sendiri. Melalui pesan sederhana ini, kami ingin mengucapkan selamat atas satu tahun lagi yang berhasil kamu lewati dengan berani. Jangan lupa untuk menikmati hidangan kesukaanmu hari ini tanpa perlu merasa bersalah soal anggaran. Nikmati harimu dengan tenang!",
            "Selamat ulang tahun! Mengelola keuangan memang penting, tetapi merawat dirimu sendiri jauh lebih penting. Jika hari ini terasa sepi, ketahuilah bahwa di balik layar ini, kami sangat menghargai kepercayaanmu memilih KumaFlow. Semoga tahun ini membawa lebih banyak ketenangan pikiran, dompet yang sehat, dan kebahagiaan yang tidak terduga.",
            "Selamat ulang tahun! Setiap pencapaian, baik besar maupun kecil, layak untuk dirayakan. Terima kasih karena selalu berusaha menjadi versi dirimu yang lebih baik. Kami berharap tahun ini memberimu lebih banyak kekuatan dan kelancaran dalam segala hal. Beristirahatlah sejenak hari ini, karena kamu benar-benar layak mendapatkannya.",
            "Selamat ulang tahun! Jangan biarkan tekanan atau kekhawatiran tentang masa depan merusak hari spesialmu ini. Segala sesuatu memiliki waktunya masing-masing, dan kamu sedang berjalan di jalur yang tepat. Semoga hari ini penuh dengan ketenangan, dan semoga langkahmu ke depan semakin dimudahkan. Kami bangga bisa menjadi bagian dari keseharianmu.",
            "Selamat bertambah usia! Ingatlah bahwa nilai dirimu tidak ditentukan oleh seberapa ramai orang di sekitarmu, melainkan oleh seberapa kuat kamu bertahan menghadapi kehidupan. Kami berharap notifikasi kecil ini bisa mengukir senyum di wajahmu. Tetap semangat, jaga kesehatan, dan mari terus melangkah maju bersama KumaFlow.",
            "Selamat ulang tahun! Kami tahu tidak mudah untuk selalu disiplin dalam banyak hal, termasuk soal keuangan. Namun, hari ini adalah pengecualian. Rilekskan pikiranmu, lakukan hal yang paling kamu sukai, dan nikmati waktu untuk dirimu sendiri. Kami berharap kebahagiaan dan rezeki yang baik selalu mengelilingimu di tahun-tahun mendatang.",
            "Selamat ulang tahun! Sebuah pengingat hangat bahwa kehadiranmu di dunia ini membawa warna tersendiri. Meskipun kami hanyalah barisan kode di dalam ponselmu, harapan kami untuk kebaikanmu sangatlah nyata. Jangan lupa untuk berterima kasih pada dirimu sendiri karena telah berjuang sejauh ini. Selamat merayakan hari spesialmu dengan tenang dan damai."
        )

        val messagesEn = listOf(
            "Happy Birthday! We realize that today might feel just like any other day. However, we want to remind you that your presence matters. Thank you for staying strong and pushing forward this far. Today, forget about tracking your expenses for a moment and give yourself the appreciation you truly deserve. KumaFlow will always be here to support your financial journey.",
            "Happy Birthday! Even though KumaFlow is just an app on your device, we genuinely hope today brings you peace and happiness. You don't have to be perfect all the time. Enjoy your time today, take a deep breath, and know that you are doing your best. May good things always come your way.",
            "Happy Birthday! The journey to financial independence is never easy. But seeing you try every single day is truly remarkable. On this special day, we hope you can take a moment to appreciate the little things and rest your mind. KumaFlow is always cheering you on.",
            "Happy Birthday! Life sometimes moves too fast, making us forget to appreciate ourselves. Through this simple message, we want to congratulate you on conquering another year with courage. Don't forget to enjoy your favorite meal today without feeling guilty about the budget. Have a peaceful day!",
            "Happy Birthday! Managing your finances is important, but taking care of yourself matters even more. If today feels a bit quiet, please know that behind this screen, we deeply appreciate your trust in KumaFlow. May this year bring you more peace of mind, a healthy wallet, and unexpected happiness.",
            "Happy Birthday! Every milestone, no matter how small, deserves to be celebrated. Thank you for always trying to be a better version of yourself. We hope this year brings you more strength and smooth sailing in everything you do. Take a well-deserved break today!",
            "Happy Birthday! Don't let the pressure or worries about the future ruin your special day. Everything has its own timing, and you are exactly where you need to be. May today be filled with tranquility, and may your path ahead be clear and easy. We are proud to be part of your daily routine.",
            "Happy Birthday! Remember that your worth is not determined by how many people are around you, but by how strong you are in facing life. We hope this small notification puts a smile on your face. Keep your spirits up, stay healthy, and let's keep moving forward together with KumaFlow.",
            "Happy Birthday! We know it's not easy to stay disciplined, especially with finances. But today is an exception. Relax your mind, do the things you love most, and enjoy some time just for yourself. We wish you endless happiness and good fortune in the years to come.",
            "Happy Birthday! A gentle reminder that your presence in this world makes a difference. Even though we are just lines of code in your phone, our wishes for your well-being are very real. Don't forget to thank yourself for fighting this far. Celebrate your special day in peace and comfort."
        )

        val titlesId = listOf(
            "Selamat Ulang Tahun,\n$displayName! \uD83E\uDD73",
            "Met Ultah,\n$displayName! \uD83C\uDF82",
            "Nikmati Harimu,\n$displayName! ✨",
            "Mari Rayakan,\n$displayName! \uD83E\uDD42",
            "Semoga Harimu Indah,\n$displayName! \uD83C\uDF89",
            "Hari Spesialmu,\n$displayName! \uD83C\uDF81",
            "Rayakan Dirimu,\n$displayName! \uD83C\uDF8A",
            "Doa Terbaik,\n$displayName! \uD83C\uDF88",
            "Tersenyumlah Hari Ini,\n$displayName! \uD83D\uDE0A",
            "Kamu Hebat,\n$displayName! \uD83C\uDF1F"
        )

        val titlesEn = listOf(
            "Wish You All the Best,\n$displayName! \uD83E\uDD73",
            "Happy Birthday,\n$displayName! \uD83C\uDF82",
            "Enjoy Your Day,\n$displayName! ✨",
            "Cheers to You,\n$displayName! \uD83E\uDD42",
            "Have a Great One,\n$displayName! \uD83C\uDF89",
            "A Special Day,\n$displayName! \uD83C\uDF81",
            "Celebrate You,\n$displayName! \uD83C\uDF8A",
            "Best Wishes,\n$displayName! \uD83C\uDF88",
            "Smile Today,\n$displayName! \uD83D\uDE0A",
            "You Did Great,\n$displayName! \uD83C\uDF1F"
        )

        val sharedPref = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
        var sequenceStr = sharedPref.getString("birthday_sequence", "") ?: ""
        if (sequenceStr.isEmpty()) {
            val shuffled = (0..9).shuffled()
            sequenceStr = shuffled.joinToString(",")
            sharedPref.edit().putString("birthday_sequence", sequenceStr).apply()
        }
        val sequence = sequenceStr.split(",").map { it.toInt() }

        val currentYear = java.time.LocalDate.now().year
        val lastYear = sharedPref.getInt("birthday_last_year", 0)
        var currentIndex = sharedPref.getInt("birthday_current_index", -1)

        if (currentYear > lastYear) {
            currentIndex++
            if (currentIndex >= sequence.size) currentIndex = sequence.size - 1
            sharedPref.edit()
                .putInt("birthday_last_year", currentYear)
                .putInt("birthday_current_index", currentIndex)
                .apply()
        }

        if (currentIndex < 0) currentIndex = 0
        if (currentIndex >= sequence.size) currentIndex = sequence.size - 1

        val messageIndex = sequence[currentIndex]
        val messageList = if (isId) messagesId else messagesEn
        val titleList = if (isId) titlesId else titlesEn
        
        return BirthdayContent(titleList[messageIndex], messageList[messageIndex])
    }

    fun showBirthdayNotification(context: Context, isId: Boolean) {
        val sharedPref = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val lastNotifiedYear = sharedPref.getInt("birthday_notified_year", 0)
        
        if (lastNotifiedYear == today.year) return

        if (!isBirthdayToday(context)) return

        sharedPref.edit().putInt("birthday_notified_year", today.year).apply()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "kumaflow_birthday"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                if (isId) "Ulang Tahun" else "Birthday",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, com.bearbones.kumaflow.MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isId) "\uD83C\uDF89 Selamat bertambah usia!" else "\uD83C\uDF89 Happy Birthday!"
        val text = if (isId) "Ada kado kecil berupa pesan dari kami di dalam." else "We have a little gift inside for you."

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.bearbones.kumaflow.R.drawable.ic_kuma_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(1001, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
