package com.example.ui.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.db.AppDatabase
import com.example.data.db.HistoryItem
import com.example.data.db.VoiceProfile
import com.example.data.repository.LimitCheckResult
import com.example.data.repository.SubscriptionType
import com.example.data.repository.VoiceRepository
import com.example.util.AnalyzedVoiceMetrics
import com.example.util.KurdishVoiceRecorder
import com.example.util.KurdishVoiceSynthesizer
import com.example.util.VoiceAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Database & Repository Setup
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "kurdish_voice_database"
        ).build()
    }

    private val repository: VoiceRepository by lazy {
        VoiceRepository(application, database.voiceDao())
    }

    // 2. Synthesizer & Recorder Setup
    private val synthesizer: KurdishVoiceSynthesizer by lazy {
        KurdishVoiceSynthesizer(application)
    }

    private val recorder: KurdishVoiceRecorder by lazy {
        KurdishVoiceRecorder(application)
    }

    // 3. UI States
    private val _currentTab = MutableStateFlow("TTS") // "TTS", "STT", "CLONE", "HISTORY"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _inputText = MutableStateFlow("بەخێربێیت بۆ ئەپڵیکەیشنی دەنگی کوردی سۆرانی") // Default Kurdish sentence
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceProfile?>(null)
    val selectedVoice: StateFlow<VoiceProfile?> = _selectedVoice.asStateFlow()

    val currentSubscription: StateFlow<SubscriptionType> = repository.currentSubscription

    // Flows collected from DB
    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val voiceProfiles: StateFlow<List<VoiceProfile>> = repository.allVoiceProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculate today's usage count reactively
    val todayUsageCount: StateFlow<Int> = historyList.combine(currentSubscription) { list, _ ->
        val startOfToday = getStartOfToday()
        list.count { it.timestamp >= startOfToday }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Active status flags
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    // Dialog & Alert controllers
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _showUpgradeDialog = MutableStateFlow(false)
    val showUpgradeDialog: StateFlow<Boolean> = _showUpgradeDialog.asStateFlow()

    // Voice Cloning States
    private val _cloningName = MutableStateFlow("")
    val cloningName: StateFlow<String> = _cloningName.asStateFlow()

    private val _isCloningRecording = MutableStateFlow(false)
    val isCloningRecording: StateFlow<Boolean> = _isCloningRecording.asStateFlow()

    private val _analyzedMetrics = MutableStateFlow<AnalyzedVoiceMetrics?>(null)
    val analyzedMetrics: StateFlow<AnalyzedVoiceMetrics?> = _analyzedMetrics.asStateFlow()

    private var tempAudioFile: File? = null

    init {
        // Automatically set temp audio file location in cache directory
        tempAudioFile = File(application.cacheDir, "temp_kurdish_speech.m4a")
    }

    // --- Action Methods ---

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun updateCloningName(name: String) {
        _cloningName.value = name
    }

    fun selectVoice(voice: VoiceProfile?) {
        _selectedVoice.value = voice
    }

    fun selectSubscription(type: SubscriptionType) {
        repository.updateSubscription(type)
        _showUpgradeDialog.value = false
        _successMessage.value = "بە سەرکەوتوویی پلانت گۆڕدرا بۆ: ${type.displayName}"
    }

    fun setShowUpgradeDialog(show: Boolean) {
        _showUpgradeDialog.value = show
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // --- TTS Synthesis ---

    fun speakText() {
        val text = _inputText.value
        if (text.trim().isEmpty()) {
            _errorMessage.value = "تکایە دەقێک بنووسە بۆ خوێندنەوە."
            return
        }

        viewModelScope.launch {
            // Check limits based on current subscription
            val currentHistory = historyList.value
            val limitResult = repository.checkRequestLimits(text, currentHistory)

            if (limitResult is LimitCheckResult.Exceeded) {
                _errorMessage.value = limitResult.message
                _showUpgradeDialog.value = true
                return@launch
            }

            // Retrieve voice specs
            val activeVoice = _selectedVoice.value
            val pitch = activeVoice?.pitch ?: 1.0f
            val speed = activeVoice?.speed ?: 1.0f
            val voiceName = activeVoice?.name ?: "دەنگی سیستەم (سۆرانی)"

            synthesizer.playText(
                text = text,
                pitch = pitch,
                speed = speed,
                useNativeEngine = false, // Google Translate web TTS has higher fidelity for Sorani
                onStart = { _isSynthesizing.value = true },
                onComplete = {
                    _isSynthesizing.value = false
                    saveHistoryItem("TTS", text, "خوێندنەوەی دەنگی سەرکەوتوو", voiceName)
                },
                onError = { err ->
                    _isSynthesizing.value = false
                    _errorMessage.value = "ھەڵە لە خوێندنەوەی دەنگ: $err"
                }
            )
        }
    }

    fun stopSpeaking() {
        synthesizer.stopAll()
        _isSynthesizing.value = false
    }

    // --- STT Speech-to-Text ---

    fun startSpeechRecording() {
        val file = tempAudioFile ?: return
        if (file.exists()) {
            file.delete()
        }

        val success = recorder.startRecording(file)
        if (success) {
            _isRecording.value = true
            _transcribedText.value = ""
            // Start updating amplitude for custom wave animation
            startAmplitudeUpdates()
        } else {
            _errorMessage.value = "نەتوانرا مایکرۆفۆن کار پێ بکرێت. تکایە مۆڵەتی دەستگەیشتن بدە."
        }
    }

    fun stopSpeechRecording() {
        val recordedFile = recorder.stopRecording()
        _isRecording.value = false
        _amplitude.value = 0

        if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
            transcribeAudio(recordedFile)
        } else {
            _errorMessage.value = "تۆمارکردنەکە زۆر کورت بوو یان تۆمار نەکرا."
        }
    }

    private fun startAmplitudeUpdates() {
        viewModelScope.launch {
            while (_isRecording.value) {
                _amplitude.value = recorder.getMaxAmplitude()
                delay(100)
            }
        }
    }

    private fun transcribeAudio(file: File) {
        viewModelScope.launch {
            _isTranscribing.value = true

            // Limit check: Does STT count towards daily limit? Yes!
            val currentHistory = historyList.value
            val limitCheck = repository.checkRequestLimits("Speech Input", currentHistory)
            if (limitCheck is LimitCheckResult.Exceeded) {
                _errorMessage.value = limitCheck.message
                _isTranscribing.value = false
                _showUpgradeDialog.value = true
                return@launch
            }

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val isApiKeyConfigured = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

                val transcription = if (isApiKeyConfigured) {
                    performGeminiTranscription(file, apiKey)
                } else {
                    simulateKurdishTranscription()
                }

                _transcribedText.value = transcription
                
                // Save to local database history
                val voiceLabel = _selectedVoice.value?.name ?: "دەنگی سیستەم (سۆرانی)"
                saveHistoryItem("STT", "تۆماری دەنگی کوردی سۆرانی", transcription, voiceLabel)

                if (!isApiKeyConfigured) {
                    _errorMessage.value = "تێبینی: کلیلی Gemini API رێک نەخراوە لە پانێڵی نهێنییەکان. لەبەر ئەوە دەقێکی سۆرانی ڕاھێنراو وەک نموونە نیشان درا."
                }
            } catch (e: Exception) {
                _errorMessage.value = "ھەڵە لە گۆڕینی دەنگ بۆ دەق: ${e.localizedMessage}"
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    private suspend fun performGeminiTranscription(file: File, apiKey: String): String = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val base64Audio = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val prompt = """
            You are a state-of-the-art Kurdish Speech-to-Text transcriber.
            Transcribe the provided Kurdish Sorani audio.
            Rules:
            1. ONLY respond with the exact Kurdish transcription. Use standard Arabic-script Kurdish letters.
            2. DO NOT translate the speech to English, Arabic, or Persian.
            3. DO NOT include annotations, speaker labels, timestamps, or introductions.
            4. If the audio lacks speech or is completely incomprehensible, respond with 'دەنگەکە ڕوون نییە'.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "audio/mp4", data = base64Audio))
                    )
                )
            )
        )

        val response = GeminiApiClient.service.generateContent(apiKey, request)
        val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        result?.trim() ?: "دەنگەکە ڕوون نییە یان نەبیسترا."
    }

    private suspend fun simulateKurdishTranscription(): String {
        delay(2500) // Simulate processing time
        val phrases = listOf(
            "سڵاو، بەیانیت باش، هیوادارم ڕۆژێکی خۆش و بەرهەمدار بەڕێ بکەیت لەگەڵ ئەپی دەنگی کوردی.",
            "داواکارییەکەم بریتییە لە تۆمارکردنی دەنگی تایبەت بە مەبەستی دروستکردنی کۆپییەکی دەنگی تەواو جێگیر.",
            "زمان و فەرهەنگی کوردی بەشێکی دەوڵەمەندی مێژووی ئێمەن و پاراستنیان ئەرکی سەرەکیمانە.",
            "سوپاس بۆ کارپێکردنی سیستەمی زیرەکی دەنگی سۆرانی بۆ جێبەجێکردنی پڕۆژە پێشکەوتووەکان."
        )
        return phrases.random()
    }

    // --- Voice Cloning ---

    fun startCloningRecording() {
        val name = _cloningName.value.trim()
        if (name.isEmpty()) {
            _errorMessage.value = "تکایە سەرەتا ناوێک بۆ دەنگە نوێیەکە بنووسە پێش تۆمارکردن."
            return
        }

        // Check cloning profile limits
        val currentProfilesCount = voiceProfiles.value.size
        val limitCheck = repository.checkCloningLimit(currentProfilesCount)
        if (limitCheck is LimitCheckResult.Exceeded) {
            _errorMessage.value = limitCheck.message
            _showUpgradeDialog.value = true
            return
        }

        val cloneFile = File(getApplication<Application>().cacheDir, "clone_sample_${System.currentTimeMillis()}.m4a")
        tempAudioFile = cloneFile

        val success = recorder.startRecording(cloneFile)
        if (success) {
            _isCloningRecording.value = true
            _analyzedMetrics.value = null
            startAmplitudeUpdates()
        } else {
            _errorMessage.value = "مایکرۆفۆن کارا نەکرا. مۆڵەتی مایک بدە لە ڕێکخستنەکان."
        }
    }

    fun stopCloningRecording() {
        val recordedFile = recorder.stopRecording()
        _isCloningRecording.value = false
        _amplitude.value = 0

        if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
            // Run analysis to extract unique vocal characteristics
            val metrics = VoiceAnalyzer.analyzeRecording(recordedFile)
            _analyzedMetrics.value = metrics
            _successMessage.value = "دەنگی تۆمارکراو بە سەرکەوتوویی شی کرایەوە! لەرەلەری دەنگ: ${metrics.pitchHz}Hz - جۆری دەنگ: ${metrics.category}."
        } else {
            _errorMessage.value = "تۆمارکردن کورت بوو یان شکست لێ ھێنا."
        }
    }

    fun saveClonedVoice() {
        val name = _cloningName.value.trim()
        val metrics = _analyzedMetrics.value
        val file = tempAudioFile

        if (name.isEmpty() || metrics == null || file == null || !file.exists()) {
            _errorMessage.value = "تکایە سەرەتا نموونە دەنگییەکە تۆمار بکە و شی بکەرەوە."
            return
        }

        viewModelScope.launch {
            val profile = VoiceProfile(
                name = name,
                audioPath = file.absolutePath,
                pitch = metrics.pitchHz / 150f, // Scale around standard 1.0 multiplier
                speed = metrics.speedFactor,
                resonance = metrics.resonance,
                gender = metrics.category,
                timestamp = System.currentTimeMillis()
            )

            repository.insertVoiceProfile(profile)
            _successMessage.value = "کۆپیکردنی دەنگەکە بە سەرکەوتوویی ئەنجامدرا و پڕۆفایلەکە پاشەکەوت کرا!"
            
            // Clear cloning states
            _cloningName.value = ""
            _analyzedMetrics.value = null
            tempAudioFile = File(getApplication<Application>().cacheDir, "temp_kurdish_speech.m4a") // reset to standard
        }
    }

    // --- History & Database Management ---

    private fun saveHistoryItem(type: String, input: String, output: String, voice: String) {
        viewModelScope.launch {
            val item = HistoryItem(
                type = type,
                inputText = input,
                outputText = output,
                voiceName = voice,
                timestamp = System.currentTimeMillis()
            )
            repository.insertHistory(item)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteHistoryById(item.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteVoiceProfile(profile: VoiceProfile) {
        viewModelScope.launch {
            repository.deleteVoiceProfileById(profile.id)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = null
            }
        }
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()
        synthesizer.release()
    }
}
