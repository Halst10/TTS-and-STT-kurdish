package com.example.data.repository

import android.content.Context
import com.example.data.db.HistoryItem
import com.example.data.db.VoiceDao
import com.example.data.db.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class SubscriptionType(val displayName: String, val dailyLimit: Int, val maxWords: Int, val maxClones: Int) {
    FREE("Free Plan", 4, 50, 0),
    PREMIUM("Premium Studio", -1, 500, 1), // -1 means unlimited
    PRO("Pro Clone Pass", -1, 1000, 10)
}

class VoiceRepository(
    private val context: Context,
    private val voiceDao: VoiceDao
) {
    // Current active subscription stored in SharedPreferences for persistence
    private val prefs = context.getSharedPreferences("voice_assistant_prefs", Context.MODE_PRIVATE)
    
    private val _currentSubscription = MutableStateFlow(getSavedSubscription())
    val currentSubscription: StateFlow<SubscriptionType> = _currentSubscription.asStateFlow()

    val allHistory: Flow<List<HistoryItem>> = voiceDao.getAllHistory()
    val allVoiceProfiles: Flow<List<VoiceProfile>> = voiceDao.getAllVoiceProfiles()

    private fun getSavedSubscription(): SubscriptionType {
        val name = prefs.getString("subscription_type", SubscriptionType.FREE.name) ?: SubscriptionType.FREE.name
        return try {
            SubscriptionType.valueOf(name)
        } catch (e: Exception) {
            SubscriptionType.FREE
        }
    }

    fun updateSubscription(subscriptionType: SubscriptionType) {
        prefs.edit().putString("subscription_type", subscriptionType.name).apply()
        _currentSubscription.value = subscriptionType
    }

    suspend fun insertHistory(item: HistoryItem) {
        voiceDao.insertHistory(item)
    }

    suspend fun deleteHistoryById(id: Int) {
        voiceDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        voiceDao.clearHistory()
    }

    suspend fun insertVoiceProfile(profile: VoiceProfile) {
        voiceDao.insertVoiceProfile(profile)
    }

    suspend fun deleteVoiceProfileById(id: Int) {
        voiceDao.deleteVoiceProfileById(id)
    }

    // Helper to check if a request can be executed based on subscription limits
    fun checkRequestLimits(text: String, todayHistory: List<HistoryItem>): LimitCheckResult {
        val currentSub = _currentSubscription.value
        val wordCount = countWords(text)

        // 1. Check Word Count Limit
        if (wordCount > currentSub.maxWords) {
            return LimitCheckResult.Exceeded(
                "Your current ${currentSub.displayName} has a limit of ${currentSub.maxWords} words per request. The input is $wordCount words. Please upgrade to write longer texts."
            )
        }

        // 2. Check Daily Limit (if applicable)
        if (currentSub.dailyLimit != -1) {
            val startOfToday = getStartOfToday()
            val todayUsageCount = todayHistory.count { it.timestamp >= startOfToday }
            if (todayUsageCount >= currentSub.dailyLimit) {
                return LimitCheckResult.Exceeded(
                    "You have reached your daily limit of ${currentSub.dailyLimit} requests for the ${currentSub.displayName}. Upgrade to Premium or Pro for unlimited requests!"
                )
            }
        }

        return LimitCheckResult.Allowed
    }

    // Check if voice cloning is allowed based on subscription and current clone count
    fun checkCloningLimit(currentClonesCount: Int): LimitCheckResult {
        val currentSub = _currentSubscription.value
        if (currentClonesCount >= currentSub.maxClones) {
            return LimitCheckResult.Exceeded(
                if (currentSub == SubscriptionType.FREE) {
                    "Voice cloning is not available on the Free Plan. Please upgrade to Premium or Pro to clone your voice!"
                } else {
                    "Your current ${currentSub.displayName} supports a maximum of ${currentSub.maxClones} cloned voice profiles. Upgrade to Pro for up to ${currentSub.maxClones} profiles!"
                }
            )
        }
        return LimitCheckResult.Allowed
    }

    private fun countWords(text: String): Int {
        if (text.trim().isEmpty()) return 0
        return text.trim().split(Regex("\\s+")).size
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

sealed class LimitCheckResult {
    object Allowed : LimitCheckResult()
    data class Exceeded(val message: String) : LimitCheckResult()
}
