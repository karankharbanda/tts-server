package com.tts_app.App;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api")
  // Allow frontend requests
public class VideoController {

    private final WebClient webClient;
    private static final String RUNWAY_API_URL = "https://api.runwayml.com/v1/generate-video";
    private static final String API_KEY = "key_5fe2cc8affa4c68eafdfac7dd6de05d99ef12fa65f45cde4bda1a6ffcb6ab6cca66ce7b743a435495940bf84cbd12a120342fb63e1827503511f4f94181fd38a"; //  Replace with your Runway API key

    public VideoController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(RUNWAY_API_URL).build();
    }

    @PostMapping("/generate-video")
    public ResponseEntity<?> generateVideo(@RequestBody Map<String, String> request) {
        String script = request.get("script");

        if (script == null || script.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(" Script cannot be empty.");
        }

        try {
            String videoUrl = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", script))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block()
                    .get("video_url").toString();

            return ResponseEntity.ok(Map.of("videoUrl", videoUrl));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Video generation failed.");
        }
    }
}
