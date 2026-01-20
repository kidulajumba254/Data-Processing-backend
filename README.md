# Data Processing API - Spring Boot Backend

A comprehensive Spring Boot application for processing large-scale student data with Excel, CSV, and database operations.

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.4.5
- **Database**: PostgreSQL 17
- **Build Tool**: Maven

## Features

### Task 1: Data Generation
- Generate Excel files with up to 1M student records
- Random data generation with configurable parameters
- Real-time progress tracking
- Memory-efficient streaming (XSSFSheetXMLHandler)

  <img width="877" height="562" alt="data generation" src="https://github.com/user-attachments/assets/f1872e0c-a49f-4c54-93da-754222f53494" />

  <img width="1094" height="766" alt="excel" src="https://github.com/user-attachments/assets/26da470d-03b1-4ae6-b27c-b59cd01010c8" />



### Task 2: Excel to CSV Processing
- Upload and process Excel files
- Automatic score adjustment (+10)
- Progress monitoring during conversion

  <img width="1090" height="591" alt="procesed_excel" src="https://github.com/user-attachments/assets/8b36816a-c655-42bd-8f53-91897b6c80a2" />

  

### Task 3: CSV to Database Upload
- Batch processing for optimal performance
- Score adjustment before database insert (+5)
- Transactional integrity
- Progress tracking for large datasets

  <img width="1141" height="950" alt="FIRST_20_IN_DESC" src="https://github.com/user-attachments/assets/47adb587-1208-417d-9710-2c47e1e00bf7" />

  <img width="693" height="445" alt="Screenshot 2026-01-20 141148" src="https://github.com/user-attachments/assets/1e8ed447-dd2f-485c-9ea1-2c3a32c96873" />

  <img width="238" height="89" alt="1-100000" src="https://github.com/user-attachments/assets/03e4e006-5fa5-4c78-9a66-0f7abb85a341" />



### Task 4: Student Report
- Pagination support
- Search by Student ID
- Filter by Class
- Export to Excel, CSV, and PDF

<img width="1909" height="865" alt="task 4 5 UI" src="https://github.com/user-attachments/assets/fa75730f-d0a3-4502-b6b2-ea4b05cd4032" />


<img width="1336" height="308" alt="var_location" src="https://github.com/user-attachments/assets/c6a0274b-1590-4464-9e37-c686cf852678" />


### Task 5: Bulk Data Export
- Export all records (1M+) in multiple formats
- Asynchronous processing with progress tracking
- Support for Excel, CSV, and PDF formats

  

## Project Structure

```
src/main/java/com/dataprocessing/
├── DataProcessingApplication.java
├── config/
│   ├── AsyncConfig.java
│   └── CorsConfig.java
├── controller/
│   ├── DataController.java
│   └── ReportController.java
├── dto/
│   └── ProgressDTO.java
├── entity/
│   └── Student.java
├── repository/
│   └── StudentRepository.java
└── service/
    ├── DataGenerationService.java
    ├── DataProcessingService.java
    ├── DataUploadService.java
    ├── ExportService.java
    └── ProgressTracker.java
```

## Prerequisites

1. **Java 21** installed
2. **PostgreSQL 17** installed and running
3. **Maven** installed

## Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE student_db;
```

2. Update `application.properties` with your credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dataprocessing
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Installation & Running

1. Clone the repository
```bash
git clone <https://github.com/kidulajumba254/Data-Processing-backend.git>
cd studentdataprocessor
```

2. Build the project
```bash
mvn clean install
```

3. Run the application
```bash
mvn spring-boot:run
```

The API will start on `http://localhost:8081`

## Swagger Documentation

Once the application is running, you can access the interactive API documentation:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8081/api-docs


## File Storage Locations

### Windows
```
C:\var\log\applications\API\dataprocessing\
```


Ensure these directories exist or the application will create them automatically.

## Performance Optimization

- **Batch Processing**: Database inserts use batch size of 1000
- **Streaming**: XSSFSheetXMLHandler for memory-efficient Excel generation
- **Asynchronous**: All long-running tasks run asynchronously
- **Indexing**: Database indexes on studentId and class fields
- **Connection Pooling**: HikariCP for optimal database connections

## Expected Performance

Based on system specifications:

| Task | Expected Time (1M records) |
|------|---------------------------|
| Data Generation | 2-5 minutes |
| Excel to CSV | 3-6 minutes |
| CSV to Database | 4-8 minutes |
| Export All (Excel) | 3-6 minutes |
| Export All (CSV) | 2-4 minutes |
| Export All (PDF) | 8-15 minutes |

*Times vary based on hardware specifications*

## Testing

1. **Generate Test Data**:
```bash
curl -X POST "http://localhost:8081/api/data/generate?numberOfRecords=1000000"
```

2. **Check Progress**:
```bash
curl "http://localhost:8081/api/data/progress/{taskId}"
```

3. **View Students**:
```bash
curl "http://localhost:8081/api/students?page=0&size=10"
```

## Error Handling

- All async operations include comprehensive error handling
- Progress tracking includes failure states
- Validation for file uploads
- Database transaction rollback on errors

## Dependencies

Key libraries used:
- Apache POI (5.2.5) - Excel processing
- OpenCSV (5.9) - CSV handling
- Apache PDFBox (3.0.1) - PDF generation (Free & Open Source)
- Springdoc OpenAPI (2.3.0) - Swagger/OpenAPI documentation
- Spring Data JPA - Database operations
- PostgreSQL JDBC Driver

## Configuration

Adjust these properties in `application.properties`:

```properties
# File upload size limits
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# Batch processing size
spring.jpa.properties.hibernate.jdbc.batch_size=1000

# Thread pool configuration (AsyncConfig.java)
executor.corePoolSize=5
executor.maxPoolSize=10
```

## Troubleshooting

1. **Out of Memory Error**: Increase JVM heap size
```bash
java -Xmx4g -jar target/data-processing-api-1.0.0.jar
```

2. **Database Connection Issues**: Verify PostgreSQL is running and credentials are correct

3. **File Permission Error**: Ensure application has write permissions to storage directory

## License

This project is created for practical assessment purposes.
