package com.bearbones.kumaflow.utils

import android.util.Log
import com.bearbones.kumaflow.TransactionDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StreakManager {
    suspend fun checkAndUpdateStreak(dao: TransactionDao) {
        val profile = dao.getProfileSync() ?: return
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val todayStr = today.format(dateFormatter)
        
        if (profile.lastActiveDate == todayStr) {
            // Already active today, streak doesn't increase multiple times a day
            return
        }
        
        var newStreak = profile.currentStreak
        var newFreezeCount = profile.freezeCount
        
        if (profile.lastActiveDate.isNotEmpty()) {
            val lastActive = try {
                LocalDate.parse(profile.lastActiveDate, dateFormatter)
            } catch (e: Exception) {
                LocalDate.now().minusDays(1) // fallback
            }
            
            val daysBetween = ChronoUnit.DAYS.between(lastActive, today)
            
            if (daysBetween == 1L) {
                // Logged consecutive day
                newStreak += 1
            } else if (daysBetween > 1L) {
                // Streak broke, checking freeze
                val missedDays = daysBetween - 1
                if (newFreezeCount >= missedDays) {
                    newFreezeCount -= missedDays.toInt()
                    newStreak += 1 // Kept the streak and increment for today
                    Log.d("StreakManager", "Freeze consumed! Used $missedDays freeze(s).")
                } else {
                    // Reset streak
                    newStreak = 1
                    newFreezeCount = 0
                    Log.d("StreakManager", "Streak reset!")
                }
            }
        } else {
            // First time ever
            newStreak = 1
        }
        
        // Milestone logic: Add 1 freeze for every 30 days streak, max 2 freezes
        if (newStreak > 0 && newStreak % 30 == 0 && newStreak > profile.currentStreak) {
            if (newFreezeCount < 2) {
                newFreezeCount += 1
                Log.d("StreakManager", "Milestone reached! Granted 1 extra freeze.")
            }
        }
        
        val updatedProfile = profile.copy(
            currentStreak = newStreak,
            lastActiveDate = todayStr,
            freezeCount = newFreezeCount
        )
        
        dao.saveProfile(updatedProfile)
    }
}
