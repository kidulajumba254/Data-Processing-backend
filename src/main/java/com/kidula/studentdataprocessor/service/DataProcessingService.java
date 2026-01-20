package com.kidula.studentdataprocessor.service;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
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
    public void processExcelToCsv(String taskId, String tempFilePath) {
        long startTime = System.currentTimeMillis();
        String csvFileName = "students_" + System.currentTimeMillis() + ".csv";
        String csvFilePath = storagePath + csvFileName;
        File uploadedFile = new File(tempFilePath);
        
        try {
            File directory = new File(storagePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (OPCPackage pkg = OPCPackage.open(uploadedFile);
                 CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilePath))) {

                ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
                XSSFReader xssfReader = new XSSFReader(pkg);
                StylesTable styles = xssfReader.getStylesTable();
                XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

                if (iter.hasNext()) {
                    try (InputStream sheetStream = iter.next()) {
                        InputSource sheetSource = new InputSource(sheetStream);
                        
                        XSSFSheetXMLHandler.SheetContentsHandler sheetHandler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                            private int currentRow = -1;
                            private String[] rowData = new String[6];
                            private int totalRowsEstimate = 1000001; // Default for 1M + header

                            @Override
                            public void startRow(int rowNum) {
                                currentRow = rowNum;
                                for (int i = 0; i < 6; i++) rowData[i] = "";
                            }

                            @Override
                            public void endRow(int rowNum) {
                                if (rowNum == 0) {
                                    csvWriter.writeNext(rowData); // Header
                                } else if (rowNum > 0) {
                                    try {
                                        if (rowData[5] != null && !rowData[5].isEmpty()) {
                                            int score = (int) Double.parseDouble(rowData[5]);
                                            rowData[5] = String.valueOf(score + 10);
                                        }
                                    } catch (Exception e) {}
                                    csvWriter.writeNext(rowData);
                                }

                                if (rowNum % 2000 == 0) {
                                    progressTracker.updateProgress(taskId, rowNum, totalRowsEstimate, startTime);
                                }
                            }

                            @Override
                            public void cell(String cellReference, String formattedValue, XSSFComment comment) {
                                int col = (new org.apache.poi.ss.util.CellReference(cellReference)).getCol();
                                if (col < 6) rowData[col] = formattedValue;
                            }                                                                                                                                                                                              
                        };

                        XMLReader sheetParser = XMLHelper.newXMLReader();
                        ContentHandler handler = new XSSFSheetXMLHandler(styles, strings, sheetHandler, false);
                        sheetParser.setContentHandler(handler);
                        sheetParser.parse(sheetSource);
                    }
                }
            }

            progressTracker.completeProgress(taskId, 1000000, startTime, csvFilePath);
        } catch (Exception e) {
            progressTracker.failProgress(taskId, "Processing failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (uploadedFile.exists()) {
                uploadedFile.delete();
            }
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
