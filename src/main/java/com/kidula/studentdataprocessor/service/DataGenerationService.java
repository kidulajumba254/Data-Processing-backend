package com.kidula.studentdataprocessor.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DataGenerationService {
    @Autowired
    private ProgressTracker progressTracker;
    @Value("${file.storage.path:C:/var/log/applications/API/dataprocessing/}")
    private String storagePath;
    private static final String[] CLASSES = {"Class1", "Class2", "Class3", "Class4", "Class5"};
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random random = new Random();
    @Async
    public void generateExcelData(String taskId, long numberOfRecords) {

        if (numberOfRecords > 1_048_575) {
            numberOfRecords = 1_048_575;
        }
        long startTime = System.currentTimeMillis();
        String fileName = "students_" + System.currentTimeMillis() + ".xlsx";
        String filePath = storagePath + fileName;
        try {
            File directory = new File(storagePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(filePath);

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                 FileOutputStream fileOut = new FileOutputStream(file)) {
                Sheet sheet = workbook.createSheet("Students");
                // Create header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }
                int updateInterval = (int) Math.max(1, numberOfRecords / 100);
                for (long i = 1; i <= numberOfRecords; i++) {
                    Row row = sheet.createRow((int) i);
                    row.createCell(0).setCellValue(i);
                    row.createCell(1).setCellValue(generateRandomString(3, 8));
                    row.createCell(2).setCellValue(generateRandomString(3, 8));
                    row.createCell(3).setCellValue(generateRandomDate().toString());
                    row.createCell(4).setCellValue(CLASSES[random.nextInt(CLASSES.length)]);
                    row.createCell(5).setCellValue(random.nextInt(21) + 55);
                    // Update progress periodically
                    if (i % updateInterval == 0 || i == numberOfRecords) {
                        progressTracker.updateProgress(taskId, i, numberOfRecords, startTime);
                    }
                }
                workbook.write(fileOut);
                fileOut.flush();
                workbook.dispose();

                progressTracker.completeProgress(taskId, numberOfRecords, startTime, filePath);
            }
        } catch (Exception e) {
            progressTracker.failProgress(taskId, "Generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private String generateRandomString(int min, int max) {
        int length = random.nextInt(max - min + 1) + min;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
    private LocalDate generateRandomDate() {
        LocalDate start = LocalDate.of(2000, 1, 1);
        LocalDate end = LocalDate.of(2010, 12, 31);
        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay + 1);
        return LocalDate.ofEpochDay(randomDay);
    }
}
