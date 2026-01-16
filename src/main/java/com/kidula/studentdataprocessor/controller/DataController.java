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

    @Operation(
            summary = "Generate Excel file with student data",
            description = "Generates an Excel file with the specified number of student records. " +
                    "Returns a task ID for tracking progress. The process runs asynchronously."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Data generation started successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid number of records"
            )
    })
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateData(
            @Parameter(description = "Number of student records to generate (e.g., 1000000)", required = true)
            @RequestParam long numberOfRecords) {

        String taskId = UUID.randomUUID().toString();
        dataGenerationService.generateExcelData(taskId, numberOfRecords);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "Data generation started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Process Excel file to CSV",
            description = "Uploads an Excel file, processes it, and converts it to CSV format. " +
                    "Student scores are automatically increased by 10. Returns a task ID for tracking progress."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Excel processing started successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "File is empty or invalid"
            )
    })
    @PostMapping(value = "/process-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> processExcel(
            @Parameter(description = "Excel file to process (.xlsx)", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String taskId = UUID.randomUUID().toString();
        dataProcessingService.processExcelToCsv(taskId, file);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "Excel processing started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Upload CSV file to database",
            description = "Uploads a CSV file and imports all records into the database. " +
                    "Student scores are automatically increased by 5 before insertion. " +
                    "Returns a task ID for tracking progress."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV upload started successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "File is empty or invalid"
            )
    })
    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCsv(
            @Parameter(description = "CSV file to upload", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String taskId = UUID.randomUUID().toString();
        dataUploadService.uploadCsvToDatabase(taskId, file);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "CSV upload started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get task progress",
            description = "Retrieves the current progress of a data processing task. " +
                    "Returns information about current records processed, total records, " +
                    "percentage complete, and time elapsed."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Progress retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProgressDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found"
            )
    })
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<ProgressDTO> getProgress(
            @Parameter(description = "Task ID returned from generation/processing/upload operations", required = true)
            @PathVariable String taskId) {

        ProgressDTO progress = progressTracker.getProgress(taskId);
        return ResponseEntity.ok(progress);
    }
}