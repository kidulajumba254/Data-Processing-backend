package com.kidula.studentdataprocessor.service;

import com.kidula.studentdataprocessor.dto.ProgressDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressTracker {

    private final Map<String, ProgressDTO> progressMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void updateProgress(String taskId, long current, long total, long startTime) {
        long timeTaken = (System.currentTimeMillis() - startTime);
        ProgressDTO progress = ProgressDTO.running(taskId, current, total, timeTaken);
        progressMap.put(taskId, progress);

        
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/progress/" + taskId, progress);
        }
    }

    public void completeProgress(String taskId, long total, long startTime, String filePath) {
        long timeTaken = (System.currentTimeMillis() - startTime);
        ProgressDTO progress = ProgressDTO.completed(taskId, total, timeTaken, filePath);
        progressMap.put(taskId, progress);

        
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/progress/" + taskId, progress);
        }
    }

    public void failProgress(String taskId, String error) {
        ProgressDTO progress = ProgressDTO.failed(taskId, error);
        progressMap.put(taskId, progress);

        // Send failure update via WebSocket
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/progress/" + taskId, progress);
        }
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
