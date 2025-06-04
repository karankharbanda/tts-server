package com.tts_app.App;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
@RestController
@RequestMapping("/api/history")
public class TtsHistoryController {

    @Autowired
    private TtsHistoryRepository ttsHistoryRepository;

    @GetMapping
    public List<TtsHistory> getHistory(Principal principal){
        return ttsHistoryRepository.findByUserEmailOrderByCreatedAtDesc(principal.getName());
    }

    @PostMapping
    public ResponseEntity<TtsHistory> saveHistory(@RequestBody TtsHistory history,Principal principal){
        history.setUserEmail(principal.getName());
        history.setCreatedAt(LocalDateTime.now().toString());
        return ResponseEntity.ok(ttsHistoryRepository.save(history));
    }
}
