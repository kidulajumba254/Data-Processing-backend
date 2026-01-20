package com.kidula.studentdataprocessor.controller;

import com.kidula.studentdataprocessor.dto.ProgressDTO;
import com.kidula.studentdataprocessor.entity.Student;
import com.kidula.studentdataprocessor.repository.StudentRepository;
import com.kidula.studentdataprocessor.service.ExportService;
import com.kidula.studentdataprocessor.service.ProgressTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
@Tag(name = "Student Reports", description = "APIs for student data reporting and exporting")
public class ReportController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private ProgressTracker progressTracker;

    @Operation(
            summary = "Get paginated student list",
            description = "Retrieves a paginated list of students with optional filtering by student ID and class. " +
                    "Supports pagination and returns total count information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Students retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStudents(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by specific student ID")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filter by class (Class1, Class2, Class3, Class4, Class5)")
            @RequestParam(required = false) String studentClass) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Student> studentPage;

        if (studentId != null || (studentClass != null && !studentClass.isEmpty())) {
            studentPage = studentRepository.findByFilters(studentId, studentClass, pageable);
        } else {
            studentPage = studentRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("students", studentPage.getContent());
        response.put("currentPage", studentPage.getNumber());
        response.put("totalItems", studentPage.getTotalElements());
        response.put("totalPages", studentPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Export students to Excel",
            description = "Exports student data to Excel format. Optionally filter by student ID or class."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Excel file generated successfully",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error generating Excel file"
            )
    })
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "Filter by specific student ID")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filter by class")
            @RequestParam(required = false) String studentClass) {

        try {
            byte[] excelData = exportService.exportToExcel(studentId, studentClass);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "students.xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Export students to CSV",
            description = "Exports student data to CSV format. Optionally filter by student ID or class."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV file generated successfully",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error generating CSV file"
            )
    })
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "Filter by specific student ID")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filter by class")
            @RequestParam(required = false) String studentClass) {

        try {
            byte[] csvData = exportService.exportToCsv(studentId, studentClass);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "students.csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Export students to PDF",
            description = "Exports student data to PDF format. Optionally filter by student ID or class."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF file generated successfully",
                    content = @Content(mediaType = "application/pdf")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error generating PDF file"
            )
    })
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @Parameter(description = "Filter by specific student ID")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filter by class")
            @RequestParam(required = false) String studentClass) {

        try {
            byte[] pdfData = exportService.exportToPdf(studentId, studentClass);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "students.pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Export all students to Excel (Async)",
            description = "Initiates an asynchronous export of all student records to Excel. " +
                    "Returns a task ID to track progress. Suitable for large datasets (1M+ records)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Export task started successfully"
            )
    })
    @PostMapping("/export/all/excel")
    public ResponseEntity<Map<String, String>> exportAllExcel() {
        String taskId = UUID.randomUUID().toString();
        exportService.exportAllToExcel(taskId);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "Excel export started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Export all students to CSV (Async)",
            description = "Initiates an asynchronous export of all student records to CSV. " +
                    "Returns a task ID to track progress. Suitable for large datasets (1M+ records)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Export task started successfully"
            )
    })
    @PostMapping("/export/all/csv")
    public ResponseEntity<Map<String, String>> exportAllCsv() {
        String taskId = UUID.randomUUID().toString();
        exportService.exportAllToCsv(taskId);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "CSV export started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Export all students to PDF (Async)",
            description = "Initiates an asynchronous export of all student records to PDF. " +
                    "Returns a task ID to track progress. Suitable for large datasets (1M+ records)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Export task started successfully"
            )
    })
    @PostMapping("/export/all/pdf")
    public ResponseEntity<Map<String, String>> exportAllPdf() {
        String taskId = UUID.randomUUID().toString();
        exportService.exportAllToPdf(taskId);

        Map<String, String> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("message", "PDF export started");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get export task progress",
            description = "Retrieves the current progress of a bulk export task."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Progress retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProgressDTO.class)
                    )
            )
    })
    @GetMapping("/export/progress/{taskId}")
    public ResponseEntity<ProgressDTO> getExportProgress(
            @Parameter(description = "Task ID from export operation", required = true)
            @PathVariable String taskId) {

        ProgressDTO progress = progressTracker.getProgress(taskId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/export/download/{fileName}")
    public ResponseEntity<Resource> downloadExport(
            @PathVariable String fileName) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = "application/octet-stream";
            if (fileName.endsWith(".pdf")) contentType = "application/pdf";
            else if (fileName.endsWith(".csv")) contentType = "text/csv";
            else if (fileName.endsWith(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
