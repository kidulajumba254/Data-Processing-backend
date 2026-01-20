package com.kidula.studentdataprocessor.controller;

import com.kidula.studentdataprocessor.dto.ProgressDTO;
import com.kidula.studentdataprocessor.service.DataGenerationService;
import com.kidula.studentdataprocessor.service.DataProcessingService;
import com.kidula.studentdataprocessor.service.DataUploadService;
import com.kidula.studentdataprocessor.service.ProgressTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
@Tag(name = "Data Processing", description = "APIs for data generation, processing, and uploading")
public class DataController {
    @Autowired
    private DataGenerationService dataGenerationService;
    @Autowired
    private DataProcessingService dataProcessingService;
    @Autowired
    private DataUploadService dataUploadService;
    @Autowired
    private ProgressTracker progressTracker;
    private final String STORAGE_PATH = "C:/var/log/applications/API/dataprocessing/";
    @Operation(summary = "Generate Excel file with student data")
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateData(@RequestParam long numberOfRecords) {
        String taskId = UUID.randomUUID().toString();
        dataGenerationService.generateExcelData(taskId, numberOfRecords);
        return ResponseEntity.ok(Map.of("taskId", taskId, "message", "Data generation started"));
    }
    @Operation(summary = "Process Excel file to CSV")
    @PostMapping(value = "/process-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> processExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            String taskId = UUID.randomUUID().toString();


            File directory = new File(STORAGE_PATH);
            if (!directory.exists()) directory.mkdirs();

            String tempFilePath = STORAGE_PATH + "upload_" + taskId + ".xlsx";
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);
            // Pass the PATH string
            dataProcessingService.processExcelToCsv(taskId, tempFilePath);
            return ResponseEntity.ok(Map.of("taskId", taskId, "message", "Excel processing started"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not save file: " + e.getMessage()));
        }
    }
    @Operation(summary = "Upload CSV file to database")
    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            String taskId = UUID.randomUUID().toString();

            File directory = new File(STORAGE_PATH);
            if (!directory.exists()) directory.mkdirs();

            String tempFilePath = STORAGE_PATH + "upload_" + taskId + ".csv";
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            dataUploadService.uploadCsvToDatabase(taskId, tempFilePath);
            return ResponseEntity.ok(Map.of("taskId", taskId, "message", "CSV upload started"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not save file: " + e.getMessage()));
        }
    }
    @Operation(summary = "Get task progress")
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<ProgressDTO> getProgress(@PathVariable String taskId) {
        ProgressDTO progress = progressTracker.getProgress(taskId);
        return ResponseEntity.ok(progress);
    }
}
