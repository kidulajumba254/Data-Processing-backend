package com.kidula.studentdataprocessor.service;

import com.kidula.studentdataprocessor.entity.Student;
import com.kidula.studentdataprocessor.repository.StudentRepository;
import com.opencsv.CSVWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProgressTracker progressTracker;

    private static final int BATCH_SIZE = 10000;

    public byte[] exportToExcel(Long studentId, String studentClass) throws Exception {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Students");
            createExcelHeader(sheet);

            int rowNum = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                fillExcelRow(row, student);
            }

            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        }
    }

    public byte[] exportToCsv(Long studentId, String studentClass) throws Exception {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Header
            String[] header = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
            csvWriter.writeNext(header);

            // Data
            for (Student student : students) {
                String[] data = {
                        student.getStudentId().toString(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getDob().toString(),
                        student.getStudentClass(),
                        student.getScore().toString()
                };
                csvWriter.writeNext(data);
            }
        }

        return stringWriter.toString().getBytes();
    }

    public byte[] exportToPdf(Long studentId, String studentClass) throws Exception {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            contentStream.newLineAtOffset(220, 800);
            contentStream.showText("Student Report");
            contentStream.endText();

            // Table header
            float yPosition = 750;
            float margin = 50;
            float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
            float rowHeight = 20;

            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("ID");
            contentStream.newLineAtOffset(50, 0);
            contentStream.showText("First Name");
            contentStream.newLineAtOffset(80, 0);
            contentStream.showText("Last Name");
            contentStream.newLineAtOffset(80, 0);
            contentStream.showText("DOB");
            contentStream.newLineAtOffset(80, 0);
            contentStream.showText("Class");
            contentStream.newLineAtOffset(60, 0);
            contentStream.showText("Score");
            contentStream.endText();

            yPosition -= rowHeight;

            // Draw line
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(page.getMediaBox().getWidth() - margin, yPosition);
            contentStream.stroke();

            yPosition -= 10;

            // Table data
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);

            int recordsPerPage = 30;
            int recordCount = 0;

            for (Student student : students) {
                if (recordCount >= recordsPerPage) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 780;
                    recordCount = 0;
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(String.valueOf(student.getStudentId()));
                contentStream.newLineAtOffset(50, 0);
                contentStream.showText(truncate(student.getFirstName(), 10));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(truncate(student.getLastName(), 10));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(student.getDob().toString());
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(student.getStudentClass());
                contentStream.newLineAtOffset(60, 0);
                contentStream.showText(String.valueOf(student.getScore()));
                contentStream.endText();

                yPosition -= rowHeight;
                recordCount++;
            }

            contentStream.close();
            document.save(outputStream);

            return outputStream.toByteArray();
        }
    }

    @Async
    public void exportAllToExcel(String taskId) {
        long startTime = System.currentTimeMillis();

        try {
            long totalRecords = studentRepository.count();
            int totalPages = (int) Math.ceil((double) totalRecords / BATCH_SIZE);

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                Sheet sheet = workbook.createSheet("Students");
                createExcelHeader(sheet);

                int rowNum = 1;
                long processedRecords = 0;

                for (int page = 0; page < totalPages; page++) {
                    Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                    Page<Student> studentPage = studentRepository.findAll(pageable);

                    for (Student student : studentPage.getContent()) {
                        Row row = sheet.createRow(rowNum++);
                        fillExcelRow(row, student);
                        processedRecords++;

                        if (processedRecords % 10000 == 0) {
                            progressTracker.updateProgress(taskId, processedRecords,
                                    totalRecords, startTime);
                        }
                    }
                }

                String fileName = "all_students_" + System.currentTimeMillis() + ".xlsx";
                String filePath = System.getProperty("java.io.tmpdir") + fileName;

                try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                    workbook.write(fileOut);
                }

                workbook.dispose();
                progressTracker.completeProgress(taskId, totalRecords, startTime, filePath);
            }

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void exportAllToCsv(String taskId) {
        long startTime = System.currentTimeMillis();

        try {
            long totalRecords = studentRepository.count();
            int totalPages = (int) Math.ceil((double) totalRecords / BATCH_SIZE);

            String fileName = "all_students_" + System.currentTimeMillis() + ".csv";
            String filePath = System.getProperty("java.io.tmpdir") + fileName;

            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath))) {

                // Header
                String[] header = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
                csvWriter.writeNext(header);

                long processedRecords = 0;

                for (int page = 0; page < totalPages; page++) {
                    Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                    Page<Student> studentPage = studentRepository.findAll(pageable);

                    for (Student student : studentPage.getContent()) {
                        String[] data = {
                                student.getStudentId().toString(),
                                student.getFirstName(),
                                student.getLastName(),
                                student.getDob().toString(),
                                student.getStudentClass(),
                                student.getScore().toString()
                        };
                        csvWriter.writeNext(data);
                        processedRecords++;

                        if (processedRecords % 10000 == 0) {
                            progressTracker.updateProgress(taskId, processedRecords,
                                    totalRecords, startTime);
                        }
                    }
                }

                progressTracker.completeProgress(taskId, totalRecords, startTime, filePath);
            }

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void exportAllToPdf(String taskId) {
        long startTime = System.currentTimeMillis();

        try {
            long totalRecords = studentRepository.count();
            int totalPages = (int) Math.ceil((double) totalRecords / BATCH_SIZE);

            String fileName = "all_students_" + System.currentTimeMillis() + ".pdf";
            String filePath = System.getProperty("java.io.tmpdir") + fileName;

            try (PDDocument document = new PDDocument()) {

                long processedRecords = 0;
                PDPage currentPage = null;
                PDPageContentStream contentStream = null;
                float yPosition = 0;
                int recordsOnPage = 0;

                for (int page = 0; page < totalPages; page++) {
                    Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                    Page<Student> studentPage = studentRepository.findAll(pageable);

                    for (Student student : studentPage.getContent()) {

                        // Create new page if needed
                        if (currentPage == null || recordsOnPage >= 30) {
                            if (contentStream != null) {
                                contentStream.close();
                            }

                            currentPage = new PDPage(PDRectangle.A4);
                            document.addPage(currentPage);
                            contentStream = new PDPageContentStream(document, currentPage);

                            // Add header
                            if (processedRecords == 0) {
                                contentStream.beginText();
                                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                                contentStream.newLineAtOffset(220, 800);
                                contentStream.showText("Student Report");
                                contentStream.endText();
                                yPosition = 750;
                            } else {
                                yPosition = 780;
                            }

                            // Table header
                            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(50, yPosition);
                            contentStream.showText("ID");
                            contentStream.newLineAtOffset(50, 0);
                            contentStream.showText("First Name");
                            contentStream.newLineAtOffset(80, 0);
                            contentStream.showText("Last Name");
                            contentStream.newLineAtOffset(80, 0);
                            contentStream.showText("DOB");
                            contentStream.newLineAtOffset(80, 0);
                            contentStream.showText("Class");
                            contentStream.newLineAtOffset(60, 0);
                            contentStream.showText("Score");
                            contentStream.endText();

                            yPosition -= 20;
                            contentStream.moveTo(50, yPosition);
                            contentStream.lineTo(550, yPosition);
                            contentStream.stroke();
                            yPosition -= 10;

                            recordsOnPage = 0;
                            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                        }

                        // Add student data
                        contentStream.beginText();
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText(String.valueOf(student.getStudentId()));
                        contentStream.newLineAtOffset(50, 0);
                        contentStream.showText(truncate(student.getFirstName(), 10));
                        contentStream.newLineAtOffset(80, 0);
                        contentStream.showText(truncate(student.getLastName(), 10));
                        contentStream.newLineAtOffset(80, 0);
                        contentStream.showText(student.getDob().toString());
                        contentStream.newLineAtOffset(80, 0);
                        contentStream.showText(student.getStudentClass());
                        contentStream.newLineAtOffset(60, 0);
                        contentStream.showText(String.valueOf(student.getScore()));
                        contentStream.endText();

                        yPosition -= 20;
                        recordsOnPage++;
                        processedRecords++;

                        if (processedRecords % 1000 == 0) {
                            progressTracker.updateProgress(taskId, processedRecords,
                                    totalRecords, startTime);
                        }
                    }
                }

                if (contentStream != null) {
                    contentStream.close();
                }

                document.save(filePath);
                progressTracker.completeProgress(taskId, totalRecords, startTime, filePath);
            }

        } catch (Exception e) {
            progressTracker.failProgress(taskId, e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Student> getFilteredStudents(Long studentId, String studentClass) {
        if (studentId != null) {
            return studentRepository.findByStudentId(studentId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (studentClass != null && !studentClass.isEmpty()) {
            return studentRepository.findByStudentClass(studentClass, Pageable.unpaged()).getContent();
        } else {
            return studentRepository.findAll();
        }
    }

    private void createExcelHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private void fillExcelRow(Row row, Student student) {
        row.createCell(0).setCellValue(student.getStudentId());
        row.createCell(1).setCellValue(student.getFirstName());
        row.createCell(2).setCellValue(student.getLastName());
        row.createCell(3).setCellValue(student.getDob().toString());
        row.createCell(4).setCellValue(student.getStudentClass());
        row.createCell(5).setCellValue(student.getScore());
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}