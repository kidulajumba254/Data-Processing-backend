package com.kidula.studentdataprocessor.service;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

@Service
public class DataProcessingService {

    @Autowired
    private ProgressTracker progressTracker;

    @Value("${file.storage.path:C:/var/log/applications/API/dataprocessing/}")
    private String storagePath;

    @Async
    public void processExcelToCsv(String taskId, MultipartFile file) {
        long startTime = System.currentTimeMillis();
        String csvFileName = "students_" + System.currentTimeMillis() + ".csv";
        String csvFilePath = null;

        try {
            // Create directory if not exists
            File directory = new File(storagePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            csvFilePath = storagePath + csvFileName;

            Sheet sheet;
            try (InputStream inputStream = file.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream);
                 CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilePath))) {

                sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getPhysicalNumberOfRows();
                int updateInterval = Math.max(1, totalRows / 100);

                // Write header
                Row headerRow = sheet.getRow(0);
                String[] headers = new String[headerRow.getPhysicalNumberOfCells()];
                for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                    headers[i] = getCellValueAsString(headerRow.getCell(i));
                }
                csvWriter.writeNext(headers);

                // Process data rows
                for (int i = 1; i < totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        String[] data = new String[row.getPhysicalNumberOfCells()];

                        for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                            Cell cell = row.getCell(j);
                            if (j == 5) { // Score column - add 10
                                int originalScore = (int) cell.getNumericCellValue();
                                data[j] = String.valueOf(originalScore + 10);
                            } else {
                                data[j] = getCellValueAsString(cell);
                            }
                        }

                        csvWriter.writeNext(data);

                        // Update progress
                        if (i % updateInterval == 0 || i == totalRows - 1) {
                            progressTracker.updateProgress(taskId, i, totalRows - 1, startTime);
                        }
                    }
                }
            }

            progressTracker.completeProgress(taskId, sheet.getPhysicalNumberOfRows() - 1,
                    startTime, csvFilePath);

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
