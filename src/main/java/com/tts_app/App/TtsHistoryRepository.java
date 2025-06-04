package com.tts_app.App;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TtsHistoryRepository extends JpaRepository<TtsHistory,Long>
{
    List<TtsHistory> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
