package com.brahmadeo.supertonic.tts.service

import android.content.Context
import android.os.Build
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.brahmadeo.supertonic.tts.SupertonicTTS
import com.brahmadeo.supertonic.tts.utils.AssetManager
import java.io.File
import java.util.Locale

class SupertonicTextToSpeechService : TextToSpeechService() {

    private val attributionContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createAttributionContext("supertonic_playback")
        } else {
            this
        }
    }

    @Volatile
    private var initializedModelVersion: String? = null

    private val initLock = Any()

    companion object {
        const val VOLUME_BOOST_FACTOR = 2.5f

        private val VOICE_NAMES = listOf("M1", "M2", "M3", "M4", "M5", "F1", "F2", "F3", "F4", "F5")

        private val V2_LOCALES = listOf(
            Locale.KOREA,
            Locale.forLanguageTag("es-ES"),
            Locale.forLanguageTag("pt-PT"),
            Locale.FRANCE
        )

        private val V3_LOCALES = listOf(
            Locale.JAPAN,
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("bg"),
            Locale.forLanguageTag("cs"),
            Locale.forLanguageTag("da"),
            Locale.GERMANY,
            Locale.forLanguageTag("el"),
            Locale.forLanguageTag("et"),
            Locale.forLanguageTag("fi"),
            Locale.forLanguageTag("hi-IN"),
            Locale.forLanguageTag("hr"),
            Locale.forLanguageTag("hu"),
            Locale.forLanguageTag("id"),
            Locale.ITALY,
            Locale.forLanguageTag("lt"),
            Locale.forLanguageTag("lv"),
            Locale.forLanguageTag("nl"),
            Locale.forLanguageTag("pl"),
            Locale.forLanguageTag("ro"),
            Locale.forLanguageTag("ru"),
            Locale.forLanguageTag("sk"),
            Locale.forLanguageTag("sl"),
            Locale.forLanguageTag("sv"),
            Locale.forLanguageTag("tr"),
            Locale.forLanguageTag("uk"),
            Locale.forLanguageTag("vi")
        )

        fun normalizeLanguage(lang: String?): String {
            if (lang == null) return "en"
            val l = lang.lowercase(Locale.ROOT)
            return when {
                l.startsWith("en") || l.startsWith("eng") -> "en"
                l.startsWith("ko") || l.startsWith("kor") -> "ko"
                l.startsWith("es") || l.startsWith("spa") -> "es"
                l.startsWith("pt") || l.startsWith("por") -> "pt"
                l.startsWith("fr") || l.startsWith("fra") || l.startsWith("fre") -> "fr"
                l.startsWith("ja") || l.startsWith("jpn") -> "ja"
                l.startsWith("ar") || l.startsWith("ara") -> "ar"
                l.startsWith("bg") || l.startsWith("bul") -> "bg"
                l.startsWith("cs") || l.startsWith("ces") || l.startsWith("cze") -> "cs"
                l.startsWith("da") || l.startsWith("dan") -> "da"
                l.startsWith("de") || l.startsWith("deu") || l.startsWith("ger") -> "de"
                l.startsWith("el") || l.startsWith("ell") || l.startsWith("gre") -> "el"
                l.startsWith("et") || l.startsWith("est") -> "et"
                l.startsWith("fi") || l.startsWith("fin") -> "fi"
                l.startsWith("hi") || l.startsWith("hin") -> "hi"
                l.startsWith("hr") || l.startsWith("hrv") -> "hr"
                l.startsWith("hu") || l.startsWith("hun") -> "hu"
                l.startsWith("id") || l.startsWith("ind") -> "id"
                l.startsWith("it") || l.startsWith("ita") -> "it"
                l.startsWith("lt") || l.startsWith("lit") -> "lt"
                l.startsWith("lv") || l.startsWith("lav") -> "lv"
                l.startsWith("nl") || l.startsWith("nld") || l.startsWith("dut") -> "nl"
                l.startsWith("pl") || l.startsWith("pol") -> "pl"
                l.startsWith("ro") || l.startsWith("ron") || l.startsWith("rum") -> "ro"
                l.startsWith("ru") || l.startsWith("rus") -> "ru"
                l.startsWith("sk") || l.startsWith("slk") || l.startsWith("slo") -> "sk"
                l.startsWith("sl") || l.startsWith("slv") -> "sl"
                l.startsWith("sv") || l.startsWith("swe") -> "sv"
                l.startsWith("tr") || l.startsWith("tur") -> "tr"
                l.startsWith("uk") || l.startsWith("ukr") -> "uk"
                l.startsWith("vi") || l.startsWith("vie") -> "vi"
                else -> "en"
            }
        }

        fun parseVoiceFileName(voiceName: String?): String? {
            if (voiceName.isNullOrBlank() || !voiceName.contains("-supertonic-")) return null
            val style = voiceName.substringAfter("-supertonic-").ifBlank { return null }
            return "${File(style).name}.json"
        }

        fun resolveLanguageForRequest(requestLanguage: String?, voiceName: String?): String {
            if (!voiceName.isNullOrBlank() && voiceName.contains("-supertonic-")) {
                return normalizeLanguage(voiceName.substringBefore("-supertonic-"))
            }
            return normalizeLanguage(requestLanguage)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("SupertonicTTS", "Service created")
        com.brahmadeo.supertonic.tts.utils.LexiconManager.load(this)
    }

    private fun getLanguageAvailability(language: String, country: String?): Int {
        val normalized = normalizeLanguage(language)
        val modelVersion = AssetManager.getModelVersionForLanguage(normalized)
        if (!AssetManager.isVersionReady(this, modelVersion)) {
            return TextToSpeech.LANG_MISSING_DATA
        }
        return if (!country.isNullOrEmpty()) TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_AVAILABLE
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val language = lang ?: return TextToSpeech.LANG_NOT_SUPPORTED
        return getLanguageAvailability(language, country)
    }

    override fun onGetLanguage(): Array<String> {
        val prefs = getSharedPreferences("SupertonicPrefs", MODE_PRIVATE)
        val selectedLang = normalizeLanguage(prefs.getString("selected_lang", "en") ?: "en")

        return when (selectedLang) {
            "ko" -> arrayOf("kor", "KOR", "")
            "es" -> arrayOf("spa", "ESP", "")
            "pt" -> arrayOf("por", "PRT", "")
            "fr" -> arrayOf("fra", "FRA", "")
            "ja" -> arrayOf("jpn", "JPN", "")
            "ar" -> arrayOf("ara", "ARA", "")
            "bg" -> arrayOf("bul", "BGR", "")
            "cs" -> arrayOf("ces", "CZE", "")
            "da" -> arrayOf("dan", "DNK", "")
            "de" -> arrayOf("deu", "DEU", "")
            "el" -> arrayOf("ell", "GRC", "")
            "et" -> arrayOf("est", "EST", "")
            "fi" -> arrayOf("fin", "FIN", "")
            "hi" -> arrayOf("hin", "IND", "")
            "hr" -> arrayOf("hrv", "HRV", "")
            "hu" -> arrayOf("hun", "HUN", "")
            "id" -> arrayOf("ind", "IDN", "")
            "it" -> arrayOf("ita", "ITA", "")
            "lt" -> arrayOf("lit", "LTU", "")
            "lv" -> arrayOf("lav", "LVA", "")
            "nl" -> arrayOf("nld", "NLD", "")
            "pl" -> arrayOf("pol", "POL", "")
            "ro" -> arrayOf("ron", "ROU", "")
            "ru" -> arrayOf("rus", "RUS", "")
            "sk" -> arrayOf("slk", "SVK", "")
            "sl" -> arrayOf("slv", "SVN", "")
            "sv" -> arrayOf("swe", "SWE", "")
            "tr" -> arrayOf("tur", "TUR", "")
            "uk" -> arrayOf("ukr", "UKR", "")
            "vi" -> arrayOf("vie", "VNM", "")
            else -> arrayOf("eng", "USA", "")
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val language = lang ?: return TextToSpeech.LANG_NOT_SUPPORTED
        val availability = getLanguageAvailability(language, country)
        if (availability == TextToSpeech.LANG_MISSING_DATA) return availability

        val modelVersion = AssetManager.getModelVersionForLanguage(normalizeLanguage(language))
        return if (ensureEngineInitialized(modelVersion)) availability else TextToSpeech.LANG_MISSING_DATA
    }

    override fun onLoadVoice(voiceName: String?): Int {
        if (voiceName == null) return TextToSpeech.ERROR
        val langPrefix = resolveLanguageForRequest(null, voiceName)
        val modelVersion = AssetManager.getModelVersionForLanguage(langPrefix)
        if (!AssetManager.isVersionReady(this, modelVersion)) return TextToSpeech.ERROR

        val voiceFile = parseVoiceFileName(voiceName) ?: return TextToSpeech.ERROR
        val file = File(filesDir, "$modelVersion/voice_styles/$voiceFile")
        if (!file.exists()) return TextToSpeech.ERROR

        return if (ensureEngineInitialized(modelVersion)) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String {
        val prefs = getSharedPreferences("SupertonicPrefs", MODE_PRIVATE)
        val selected = prefs.getString("selected_voice", "F3.json") ?: "F3.json"
        val voiceName = if (selected.endsWith(".json")) selected.substringBeforeLast(".json") else selected

        val prefix = normalizeLanguage(lang)
        return "$prefix-supertonic-$voiceName"
    }

    override fun onGetVoices(): List<Voice> {
        val voices = mutableListOf<Voice>()

        if (AssetManager.isVersionReady(this, "v1")) {
            VOICE_NAMES.forEach { name ->
                voices.add(Voice("en-supertonic-$name", Locale.US, Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, setOf()))
            }
        }

        if (AssetManager.isVersionReady(this, "v2")) {
            V2_LOCALES.forEach { locale ->
                VOICE_NAMES.forEach { name ->
                    voices.add(Voice("${locale.language}-supertonic-$name", locale, Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, setOf()))
                }
            }
        }

        if (AssetManager.isVersionReady(this, "v3")) {
            V3_LOCALES.forEach { locale ->
                VOICE_NAMES.forEach { name ->
                    voices.add(Voice("${locale.language}-supertonic-$name", locale, Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, setOf()))
                }
            }
        }

        return voices
    }

    override fun onStop() {
        SupertonicTTS.setCancelled(true)
    }

    private val textNormalizer = com.brahmadeo.supertonic.tts.utils.TextNormalizer()

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        SupertonicTTS.setCancelled(false)

        val rawText = request.charSequenceText?.toString() ?: return
        val requestedVoice = request.voiceName
        val requestedLang = resolveLanguageForRequest(request.language, requestedVoice)
        val modelVersion = AssetManager.getModelVersionForLanguage(requestedLang)

        if (!AssetManager.isVersionReady(this, modelVersion)) {
            callback.error()
            return
        }

        if (!ensureEngineInitialized(modelVersion)) {
            callback.error()
            return
        }

        val prefs = attributionContext.getSharedPreferences("SupertonicPrefs", MODE_PRIVATE)
        val preferredVoice = prefs.getString("selected_voice", "F3.json") ?: "F3.json"
        val voiceFile = parseVoiceFileName(requestedVoice) ?: preferredVoice

        val voiceStyleDir = File(filesDir, "$modelVersion/voice_styles")
        val safeStylePath = File(voiceStyleDir, voiceFile)
        val fallbackStylePath = File(voiceStyleDir, "F3.json")

        val stylePath = try {
            val safeCanonical = safeStylePath.canonicalPath
            val baseCanonical = voiceStyleDir.canonicalPath
            when {
                !safeCanonical.startsWith(baseCanonical) -> fallbackStylePath.absolutePath
                safeStylePath.exists() -> safeStylePath.absolutePath
                fallbackStylePath.exists() -> fallbackStylePath.absolutePath
                else -> {
                    callback.error()
                    return
                }
            }
        } catch (_: Exception) {
            if (fallbackStylePath.exists()) fallbackStylePath.absolutePath else {
                callback.error()
                return
            }
        }

        val effectiveSpeed = (request.speechRate / 100.0f).coerceIn(0.5f, 2.5f)
        callback.start(SupertonicTTS.getAudioSampleRate(), android.media.AudioFormat.ENCODING_PCM_16BIT, 1)

        val steps = prefs.getInt("diffusion_steps", 5)
        val isAdvancedEnabled = prefs.getBoolean("is_advanced_normalization", false)
        val sibilanceMode = prefs.getInt("sibilance_reduction_mode", 1)

        var finalStylePath = stylePath
        val isMixing = prefs.getBoolean("is_mixing_enabled", false)
        if (isMixing) {
            val voice2 = prefs.getString("selected_voice_2", "M2.json") ?: "M2.json"
            val stylePath2 = File(voiceStyleDir, voice2)
            val alpha = prefs.getFloat("mix_alpha", 0.5f)
            if (stylePath2.exists()) {
                finalStylePath = "$stylePath;${stylePath2.absolutePath};$alpha"
            }
        }

        try {
            val sentences = textNormalizer.splitIntoSentences(rawText, requestedLang)
            var success = true
            for (sentence in sentences) {
                if (SupertonicTTS.isCancelled()) {
                    success = false
                    break
                }

                val normalizedText = textNormalizer.normalize(sentence, requestedLang, isAdvancedEnabled)
                val audioData = SupertonicTTS.generateAudio(
                    normalizedText,
                    requestedLang,
                    finalStylePath,
                    effectiveSpeed,
                    0.0f,
                    steps,
                    VOLUME_BOOST_FACTOR,
                    null,
                    sibilanceMode
                )

                if (audioData != null && audioData.isNotEmpty()) {
                    var offset = 0
                    while (offset < audioData.size) {
                        val length = 4096.coerceAtMost(audioData.size - offset)
                        callback.audioAvailable(audioData, offset, length)
                        offset += length
                    }
                }
            }
            if (success) callback.done() else callback.error()
        } catch (e: Exception) {
            Log.e("SupertonicTTS", "Synthesis failed", e)
            callback.error()
        }
    }

    private fun ensureEngineInitialized(modelVersion: String): Boolean {
        val modelPath = File(filesDir, "$modelVersion/onnx").absolutePath
        val libPath = applicationInfo.nativeLibraryDir + "/libonnxruntime.so"

        synchronized(initLock) {
            if (initializedModelVersion == modelVersion && SupertonicTTS.isInitialized(modelPath)) {
                return true
            }

            val success = SupertonicTTS.initialize(modelPath, libPath)
            if (success) {
                initializedModelVersion = modelVersion
            }
            return success
        }
    }
}
