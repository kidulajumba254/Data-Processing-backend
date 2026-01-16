package com.kidula.studentdataprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private String taskId;
    private String status; // RUNNING, COMPLETED, FAILED
    private long currentRecords;
    private long totalRecords;
    private double progressPercentage;
    private long timeTakenSeconds;
    private String message;
    private String filePath;
    private String error;

    public static ProgressDTO running(String taskId, long current, long total, long seconds) {
        return ProgressDTO.builder()
                .taskId(taskId)
                .status("RUNNING")
                .currentRecords(current)
                .totalRecords(total)
                .progressPercentage(total > 0 ? (double) current / total * 100 : 0)
                .timeTakenSeconds(seconds)
                .build();
    }

    public static ProgressDTO completed(String taskId, long total, long seconds, String filePath) {
        return ProgressDTO.builder()
                .taskId(taskId)
                .status("COMPLETED")
                .currentRecords(total)
                .totalRecords(total)
                .progressPercentage(100.0)
                .timeTakenSeconds(seconds)
                .filePath(filePath)
                .message("Process completed successfully")
                .build();
    }

    public static ProgressDTO failed(String taskId, String error) {
        return ProgressDTO.builder()
                .taskId(taskId)
                .status("FAILED")
                .error(error)
                .message("Process failed")
                .build();
    }
}
