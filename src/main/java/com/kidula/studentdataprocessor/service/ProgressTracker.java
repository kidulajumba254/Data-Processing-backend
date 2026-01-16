package com.kidula.studentdataprocessor.service;

import com.kidula.studentdataprocessor.dto.ProgressDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressTracker {

    private final Map<String, ProgressDTO> progressMap = new ConcurrentHashMap<>();

    public void updateProgress(String taskId, long current, long total, long startTime) {
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
        ProgressDTO progress = ProgressDTO.running(taskId, current, total, timeTaken);
        progressMap.put(taskId, progress);
    }

    public void completeProgress(String taskId, long total, long startTime, String filePath) {
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
        ProgressDTO progress = ProgressDTO.completed(taskId, total, timeTaken, filePath);
        progressMap.put(taskId, progress);
    }

    public void failProgress(String taskId, String error) {
        ProgressDTO progress = ProgressDTO.failed(taskId, error);
        progressMap.put(taskId, progress);
    }

    public ProgressDTO getProgress(String taskId) {
        return progressMap.getOrDefault(taskId, ProgressDTO.builder()
                .taskId(taskId)
                .status("NOT_FOUND")
                .message("Task not found")
                .build());
    }

    public void removeProgress(String taskId) {
        progressMap.remove(taskId);
    }
}
