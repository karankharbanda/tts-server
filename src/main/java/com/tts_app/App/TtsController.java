package com.tts_app.App;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @Autowired
    private TtsService ttsService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, String>>> convertTextToSpeech(@RequestBody Map<String, String> body) {
        return ttsService.convertTextToSpeech(body);
    }

    @GetMapping("/voices")
    public ResponseEntity<Map<String, Object>> getAvailableVoices() {
        return ttsService.getAvailableVoices();
}
}