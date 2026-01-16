package com.kidula.studentdataprocessor.service;

import com.kidula.studentdataprocessor.entity.Student;
import com.kidula.studentdataprocessor.repository.StudentRepository;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataUploadService {

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private StudentRepository studentRepository;

    private static final int BATCH_SIZE = 1000;

    @Async
    @Transactional
    public void uploadCsvToDatabase(String taskId, MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {

            // Skip header
            String[] header = csvReader.readNext();

            List<Student> batch = new ArrayList<>();
            String[] line;
            long totalProcessed = 0;
            long lineCount = 0;

            // First pass to count total lines (for progress tracking)
            try (CSVReader countReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                countReader.readNext(); // Skip header
                while (countReader.readNext() != null) {
                    lineCount++;
                }
            }

            int updateInterval = (int) Math.max(1, lineCount / 100);

            while ((line = csvReader.readNext()) != null) {
                try {
                    Long studentId = Long.parseLong(line[0]);
                    String firstName = line[1];
                    String lastName = line[2];
                    LocalDate dob = LocalDate.parse(line[3]);
                    String studentClass = line[4];
                    Integer score = Integer.parseInt(line[5]) + 5; // Add 5 to score

                    Student student = new Student(studentId, firstName, lastName,
                            dob, studentClass, score);
                    batch.add(student);

                    // Save in batches
                    if (batch.size() >= BATCH_SIZE) {
                        studentRepository.saveAll(batch);
                        studentRepository.flush();
                        batch.clear();
                        totalProcessed += BATCH_SIZE;

                        if (totalProcessed % updateInterval == 0) {
                            progressTracker.updateProgress(taskId, totalProcessed, lineCount, startTime);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error processing line: " + String.join(",", line));
                    e.printStackTrace();
                }
            }

            // Save remaining records
            if (!batch.isEmpty()) {
                studentRepository.saveAll(batch);
                studentRepository.flush();
                totalProcessed += batch.size();
            }

            progressTracker.completeProgress(taskId, totalProcessed, startTime,
                    "Database upload completed");

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
            e.printStackTrace();
        }
    }
}