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
    private StudentRepository studentRepository;

    @Autowired
    private ProgressTracker progressTracker;

    private static final int BATCH_SIZE = 1000;

    @Async

    public void uploadCsvToDatabase(String taskId, String tempFilePath) {
        long startTime = System.currentTimeMillis();
        File csvFile = new File(tempFilePath);

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {

            studentRepository.deleteAllInBatch();

            reader.readNext(); // Skip header
            List<Student> batch = new ArrayList<>();
            String[] line;
            long totalProcessed = 0;

            // Dynamic row counting for accurate progress
            long totalLines = java.nio.file.Files.lines(csvFile.toPath()).count() - 1;
            if (totalLines <= 0) totalLines = 1000000; 

            while ((line = reader.readNext()) != null) {
                Student student = new Student(
                        Long.parseLong(line[0]),
                        line[1], line[2],
                        LocalDate.parse(line[3]),
                        line[4],
                        Integer.parseInt(line[5]) + 5 // Task 3 Requirement (+5)
                );
                batch.add(student);

                if (batch.size() >= BATCH_SIZE) {
                    saveBatch(batch); // Save in a small transaction
                    batch.clear();
                    totalProcessed += BATCH_SIZE;
                    progressTracker.updateProgress(taskId, totalProcessed, totalLines, startTime);
                }
            }
            if (!batch.isEmpty()) {
                saveBatch(batch);
                totalProcessed += batch.size();
            }

            progressTracker.completeProgress(taskId, totalProcessed, startTime, "Success");

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
        } finally {
            if (csvFile.exists()) csvFile.delete();
        }
    }

    @Transactional 
    public void saveBatch(List<Student> batch) {
        studentRepository.saveAll(batch);
        studentRepository.flush();
    }
}
