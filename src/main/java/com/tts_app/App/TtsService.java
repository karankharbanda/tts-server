package com.tts_app.App;

import com.google.cloud.texttospeech.v1.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);
    @CircuitBreaker(name = "ttsService",fallbackMethod = "ttsFallback")
    public CompletableFuture<ResponseEntity<Map<String, String>>> convertTextToSpeech(Map<String, String> body) {
       return CompletableFuture.supplyAsync(() -> {
           try {
               String text = body.get("text");
               String languageCode = body.getOrDefault("languageCode", "en-US");
               String voiceName = body.getOrDefault("voiceName", "en-US-Wavenet-D");
               float pitch = Float.parseFloat(body.getOrDefault("pitch", "0"));
               float speakingRate = Float.parseFloat(body.getOrDefault("speakingRate", "1.0"));
               float volumeGainDb = Float.parseFloat(body.getOrDefault("volumeGainDb", "0"));

               if (text == null || text.trim().isEmpty()) {
                   return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty"));
               }

               try (TextToSpeechClient ttsClient = TextToSpeechClient.create()) {
                   SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
                   VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                           .setLanguageCode(languageCode)
                           .setName(voiceName)
                           .build();
                   AudioConfig audioConfig = AudioConfig.newBuilder()
                           .setAudioEncoding(AudioEncoding.MP3)
                           .setPitch(pitch)
                           .setSpeakingRate(speakingRate)
                           .setVolumeGainDb(volumeGainDb)
                           .build();

                   SynthesizeSpeechResponse response = ttsClient.synthesizeSpeech(input, voice, audioConfig);
                   String base64Audio = Base64.getEncoder().encodeToString(response.getAudioContent().toByteArray());
                   return ResponseEntity.ok(Map.of("audio", base64Audio));
               }

           } catch (Exception e) {
               logger.error("TTS Generation error ",e);
               throw new RuntimeException("TTS Service failed ",e);
           }
       });
    }

    public CompletableFuture<ResponseEntity<Map<String,String>>> ttsFallback(Map<String,String> body,Throwable t){
        logger.error("Fallback triggerd for tts service",t);;
        return CompletableFuture.completedFuture(
                ResponseEntity.status(503).body(Map.of("error","TTS Service temporarily unavailable"))
        );
    }

    public ResponseEntity<Map<String, Object>> getAvailableVoices() {
        try (TextToSpeechClient ttsClient = TextToSpeechClient.create()) {
            ListVoicesResponse response = ttsClient.listVoices(ListVoicesRequest.newBuilder().build());

            Map<String, List<Map<String, Object>>> groupedVoices = response.getVoicesList().stream()
                    .flatMap(voice -> voice.getLanguageCodesList().stream()
                            .map(lang -> Map.entry(lang, voice)))
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(entry -> {
                                Voice voice = entry.getValue();
                                Map<String, Object> v = new HashMap<>();
                                v.put("voiceName", voice.getName());
                                v.put("gender", voice.getSsmlGender().name());
                                v.put("languageCode", entry.getKey());
                                v.put("sampleRateHz", voice.getNaturalSampleRateHertz());
                                v.put("customName", formatCustomName(
                                        voice.getName(),
                                        voice.getSsmlGender().name(),
                                        entry.getKey()
                                ));
                                v.put("supportsPitch", supportsPitch(voice.getName()));
                                v.put("supportsRate", true);  // assumed always supported
                                v.put("supportsVolume", supportsVolume(voice.getName()));
                                return v;
                            }, Collectors.toList())
                    ));

            // Limit to 10 voices per language
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedVoices.entrySet()) {
                List<Map<String, Object>> voices = entry.getValue().stream().limit(10).collect(Collectors.toList());
                result.put(entry.getKey(), voices);
            }

            return ResponseEntity.ok(Map.of("voicesByLanguage", result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to fetch voices: " + e.getMessage(),
                    "voicesByLanguage", Map.of()
            ));
        }
    }

    private String formatCustomName(String voiceName, String gender, String languageCode) {
        String type = getVoiceType(voiceName); // "Natural", "AI", or "Standard"
        String region = getLanguageLabel(languageCode); // "English (US)", etc.
        String voiceGender = capitalize(gender.toLowerCase()); // FEMALE -> Female

        return region + " - " + type + " " + voiceGender + " Voice";
    }

    private boolean supportsPitch(String voiceName) {
        return voiceName.toLowerCase().contains("wavenet") || voiceName.toLowerCase().contains("chirp");
    }

    private boolean supportsVolume(String voiceName) {
        return voiceName.toLowerCase().contains("wavenet") || voiceName.toLowerCase().contains("chirp");
    }

    private String getVoiceType(String voiceName) {
        if (voiceName.toLowerCase().contains("chirp")) return "Natural";
        if (voiceName.toLowerCase().contains("wavenet")) return "AI";
        return "Standard";
    }

    private String getLanguageLabel(String code) {
        switch (code) {
            case "en-US": return "English (US)";
            case "en-GB": return "English (UK)";
            case "hi-IN": return "Hindi";
            case "uk-UA": return "Ukrainian";
            case "fr-FR": return "French";
            case "es-ES": return "Spanish";
            default: return code;
        }
    }

    private String capitalize(String word) {
        return word == null || word.isEmpty()
                ? word
                : word.substring(0, 1).toUpperCase() + word.substring(1);
}
}