package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class KurdishVoiceSynthesizer(
    private val context: Context,
    private val onInitComplete: () -> Unit = {}
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var downloadJob: Job? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try to set locale for Central Kurdish (ckb) or Arabic (ar) or default
                val kurdishLocale = Locale("ckb")
                val result = tts?.setLanguage(kurdishLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("KurdishVoiceSynthesizer", "Kurdish locale not supported in native engine, falling back to System default.")
                    tts?.language = Locale.getDefault()
                }
                isTtsInitialized = true
                onInitComplete()
            } else {
                Log.e("KurdishVoiceSynthesizer", "Failed to initialize native TextToSpeech engine.")
            }
        }
    }

    fun playText(
        text: String,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        useNativeEngine: Boolean = false,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopAll()

        if (useNativeEngine) {
            playNativeTts(text, pitch, speed, onStart, onComplete, onError)
        } else {
            playOnlineTts(text, pitch, speed, onStart, onComplete, onError)
        }
    }

    private fun playOnlineTts(
        text: String,
        pitch: Float,
        speed: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        onStart()
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val cacheFile = File(context.cacheDir, "kurdish_tts_temp.mp3")
            var success = false
            val candidates = listOf(
                // Candidate 1: Sorani Original with gtx client
                Triple(text, "ckb", "gtx"),
                // Candidate 2: Sorani Original with tw-ob client
                Triple(text, "ckb", "tw-ob"),
                // Candidate 3: Kurmanji Latin with gtx client
                Triple(transliterateSoraniToLatin(text), "ku", "gtx"),
                // Candidate 4: Kurmanji Latin with tw-ob client
                Triple(transliterateSoraniToLatin(text), "ku", "tw-ob")
            )

            withContext(Dispatchers.IO) {
                for ((ttsText, lang, client) in candidates) {
                    try {
                        if (cacheFile.exists()) {
                            cacheFile.delete()
                        }
                        val encodedText = URLEncoder.encode(ttsText, "UTF-8")
                        val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=$lang&client=$client&q=$encodedText"
                        Log.d("KurdishVoiceSynthesizer", "Trying candidate URL: $ttsUrl")
                        
                        val url = URL(ttsUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 4000
                        connection.readTimeout = 4000
                        connection.setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36"
                        )
                        
                        val responseCode = connection.responseCode
                        Log.d("KurdishVoiceSynthesizer", "Candidate response code for lang=$lang client=$client: HTTP $responseCode")
                        if (responseCode == 200) {
                            connection.inputStream.use { input ->
                                FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (cacheFile.exists() && cacheFile.length() > 0) {
                                Log.i("KurdishVoiceSynthesizer", "Successfully downloaded TTS for lang=$lang client=$client, size=${cacheFile.length()} bytes")
                                success = true
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("KurdishVoiceSynthesizer", "Candidate download failed for lang=$lang client=$client", e)
                    }
                }
            }

            if (success && cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(cacheFile.absolutePath)
                        setOnPreparedListener { mp ->
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val params = PlaybackParams().apply {
                                        setPitch(pitch.coerceIn(0.5f, 2.0f))
                                        setSpeed(speed.coerceIn(0.5f, 2.0f))
                                    }
                                    mp.playbackParams = params
                                }
                                mp.start()
                            } catch (e: Exception) {
                                Log.e("KurdishVoiceSynthesizer", "Error setting playback params", e)
                                mp.start()
                            }
                        }
                        setOnCompletionListener {
                            onComplete()
                            stopAll()
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.w("KurdishVoiceSynthesizer", "MediaPlayer playing local file failed. Trying direct streaming fallback.")
                            tryDirectStreamingFallback(text, pitch, speed, onStart, onComplete, onError)
                            true
                        }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    Log.e("KurdishVoiceSynthesizer", "MediaPlayer local play setup failed. Trying direct streaming fallback.", e)
                    tryDirectStreamingFallback(text, pitch, speed, onStart, onComplete, onError)
                }
            } else {
                Log.w("KurdishVoiceSynthesizer", "Online TTS download failed. Trying direct streaming fallback.")
                tryDirectStreamingFallback(text, pitch, speed, onStart, onComplete, onError)
            }
        }
    }

    private fun tryDirectStreamingFallback(
        text: String,
        pitch: Float,
        speed: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val latinText = transliterateSoraniToLatin(text)
                val encodedText = URLEncoder.encode(latinText, "UTF-8")
                val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ku&client=tw-ob&q=$encodedText"
                Log.d("KurdishVoiceSynthesizer", "Direct streaming fallback URL: $ttsUrl")
                
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    val headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36"
                    )
                    setDataSource(context, android.net.Uri.parse(ttsUrl), headers)
                    setOnPreparedListener { mp ->
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val params = PlaybackParams().apply {
                                    setPitch(pitch.coerceIn(0.5f, 2.0f))
                                    setSpeed(speed.coerceIn(0.5f, 2.0f))
                                }
                                mp.playbackParams = params
                            }
                            mp.start()
                        } catch (e: Exception) {
                            Log.e("KurdishVoiceSynthesizer", "Error setting playback params on direct stream", e)
                            mp.start()
                        }
                    }
                    setOnCompletionListener {
                        onComplete()
                        stopAll()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w("KurdishVoiceSynthesizer", "Direct streaming failed (code: $what, extra: $extra). Falling back to Native TTS.")
                        playNativeTts(text, pitch, speed, onStart, onComplete, onError)
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("KurdishVoiceSynthesizer", "Direct streaming setup failed. Falling back to Native TTS.", e)
                playNativeTts(text, pitch, speed, onStart, onComplete, onError)
            }
        }
    }

    fun transliterateSoraniToLatin(text: String): String {
        var result = text
        
        // Multi-character / Digraph replacements first to preserve diphthongs and unique sounds
        result = result.replace("وو", "û")
        result = result.replace("یی", "î")
        result = result.replace("ئێ", "ê")
        result = result.replace("ئۆ", "o")
        result = result.replace("ئە", "e")
        result = result.replace("ئا", "a")
        result = result.replace("ئی", "i")
        result = result.replace("ئو", "u")
        result = result.replace("ڕ", "r")
        result = result.replace("ڵ", "l")
        result = result.replace("ێ", "ê")
        result = result.replace("ۆ", "o")
        
        // Mapping Kurdish/Arabic/Persian digits to Western digits
        val digitMap = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
            '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
        )
        
        // Individual Kurdish character mappings to Latin equivalents (Hawar alphabet rules)
        val charMap = mapOf(
            'ئ' to "",
            'ا' to "a",
            'ب' to "b",
            'پ' to "p",
            'ت' to "t",
            'ج' to "c",
            'چ' to "ç",
            'ح' to "h",
            'خ' to "x",
            'د' to "d",
            'ر' to "r",
            'ز' to "z",
            'ژ' to "j",
            'س' to "s",
            'ش' to "ş",
            'ع' to "a",
            'غ' to "x",
            'ف' to "f",
            'ڤ' to "v",
            'ق' to "q",
            'ک' to "k",
            'گ' to "g",
            'ل' to "l",
            'م' to "m",
            'ن' to "n",
            'ه' to "h",
            'و' to "u",
            'ی' to "i"
        )
        
        val sb = StringBuilder()
        for (char in result) {
            val mappedDigit = digitMap[char]
            if (mappedDigit != null) {
                sb.append(mappedDigit)
                continue
            }
            val mappedChar = charMap[char]
            if (mappedChar != null) {
                sb.append(mappedChar)
            } else {
                sb.append(char)
            }
        }
        
        var latin = sb.toString()
        // Convert trailing 'h' to 'e' because 'ه' at the end of Kurdish words indicates vowel 'e' (Ae)
        latin = latin.replace(Regex("h\\b"), "e")
        // Clean up redundant double vowels
        latin = latin.replace("ii", "î")
        latin = latin.replace("uu", "û")
        
        return latin
    }

    fun convertToEnglishPhonetics(text: String): String {
        var latin = transliterateSoraniToLatin(text)
        
        // Replace special characters with English phonetic approximations
        latin = latin.replace("ç", "ch")
        latin = latin.replace("ş", "sh")
        latin = latin.replace("x", "kh")
        latin = latin.replace("ê", "ay")
        latin = latin.replace("î", "ee")
        latin = latin.replace("û", "oo")
        latin = latin.replace("ç", "ch")
        latin = latin.replace("ş", "sh")
        latin = latin.replace("x", "kh")
        latin = latin.replace("ê", "ay")
        latin = latin.replace("î", "ee")
        latin = latin.replace("û", "oo")
        latin = latin.replace("é", "ay")
        latin = latin.replace("é", "ay")
        
        // Remove apostrophes or non-ascii symbols to keep it clean for TTS
        latin = latin.replace(Regex("[^a-zA-Z0-9\\s.,!?]"), "")
        
        return latin
    }

    private fun playNativeTts(
        text: String,
        pitch: Float,
        speed: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            // Wait up to 3 seconds for TTS to initialize if in progress
            var waitCount = 0
            while (!isTtsInitialized && waitCount < 30) {
                delay(100)
                waitCount++
            }

            // If TTS is null, try to initialize it on-demand
            if (tts == null) {
                withContext(Dispatchers.Main) {
                    try {
                        tts = TextToSpeech(context) { status ->
                            if (status == TextToSpeech.SUCCESS) {
                                isTtsInitialized = true
                                val kurdishLocale = Locale("ckb")
                                tts?.setLanguage(kurdishLocale)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("KurdishVoiceSynthesizer", "Error creating TTS on-demand", e)
                    }
                }
                // Wait up to 2 seconds for on-demand initialization to complete
                var onDemandWait = 0
                while (!isTtsInitialized && onDemandWait < 20) {
                    delay(100)
                    onDemandWait++
                }
            }

            val currentTts = tts
            if (!isTtsInitialized || currentTts == null) {
                withContext(Dispatchers.Main) {
                    onError("سیستەمی خوێندنەوەی دەنگی مۆبایلەکەت ئامادە نییە. تکایە دڵنیابەرەوە کە Speech Services لەسەر مۆبایلەکەت چالاکە.")
                }
                return@launch
            }

            try {
                withContext(Dispatchers.Main) {
                    onStart()
                    
                    val kurdishLocale = Locale("ckb")
                    val langResult = currentTts.setLanguage(kurdishLocale)
                    
                    val textToSpeak: String
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fall back to English phonetic pronunciation
                        currentTts.setLanguage(Locale.US)
                        textToSpeak = convertToEnglishPhonetics(text)
                        Log.d("KurdishVoiceSynthesizer", "Kurdish not supported natively. Speaking phonetic English: $textToSpeak")
                    } else {
                        textToSpeak = text
                        Log.d("KurdishVoiceSynthesizer", "Kurdish supported natively. Speaking original: $textToSpeak")
                    }

                    currentTts.setPitch(pitch)
                    currentTts.setSpeechRate(speed)
                    
                    val utteranceId = "KurdishTTS_${System.currentTimeMillis()}"
                    
                    currentTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Managed by caller state
                        }

                        override fun onDone(utteranceId: String?) {
                            scope.launch(Dispatchers.Main) {
                                onComplete()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            scope.launch(Dispatchers.Main) {
                                onError("سیستەمی دەنگی مۆبایلەکەت تووشی کێشە بوو لە کاتی خوێندنەوەدا.")
                            }
                        }
                        
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            scope.launch(Dispatchers.Main) {
                                onError("خوێندنەوەی دەنگی سیستەم سەرکەوتوو نەبوو (کۆد: $errorCode).")
                            }
                        }
                    })

                    currentTts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("کێشەیەک لە خوێندنەوەی دەنگی سیستەم ڕوویدا: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stopAll() {
        try {
            downloadJob?.cancel()
            downloadJob = null
        } catch (e: Exception) {
            Log.e("KurdishVoiceSynthesizer", "Error cancelling download job", e)
        }

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("KurdishVoiceSynthesizer", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("KurdishVoiceSynthesizer", "Error stopping TTS", e)
        }
    }

    fun release() {
        stopAll()
        try {
            scope.cancel()
        } catch (e: Exception) {
            Log.e("KurdishVoiceSynthesizer", "Error cancelling scope", e)
        }
        tts?.shutdown()
        tts = null
    }
}
