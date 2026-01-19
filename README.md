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
- Memory-efficient streaming (SXSSFWorkbook)

### Task 2: Excel to CSV Processing
- Upload and process Excel files
- Automatic score adjustment (+10)
- Progress monitoring during conversion

### Task 3: CSV to Database Upload
- Batch processing for optimal performance
- Score adjustment before database insert (+5)
- Transactional integrity
- Progress tracking for large datasets

### Task 4: Student Report
- Pagination support
- Search by Student ID
- Filter by Class
- Export to Excel, CSV, and PDF

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

Swagger UI provides:
- Interactive API testing
- Complete endpoint documentation
- Request/response examples
- Schema definitions
- Try-it-out functionality for all endpoints

## API Endpoints

### Data Generation
```
POST /api/data/generate?numberOfRecords=1000000
Response: { "taskId": "uuid", "message": "Data generation started" }
```

### Process Excel to CSV
```
POST /api/data/process-excel
Body: multipart/form-data with file
Response: { "taskId": "uuid", "message": "Excel processing started" }
```

### Upload CSV to Database
```
POST /api/data/upload-csv
Body: multipart/form-data with file
Response: { "taskId": "uuid", "message": "CSV upload started" }
```

### Get Progress
```
GET /api/data/progress/{taskId}
Response: {
  "taskId": "uuid",
  "status": "RUNNING",
  "currentRecords": 500000,
  "totalRecords": 1000000,
  "progressPercentage": 50.0,
  "timeTakenSeconds": 120
}
```

### Student Report (Paginated)
```
GET /api/students?page=0&size=10&studentId=123&studentClass=Class1
Response: {
  "students": [...],
  "currentPage": 0,
  "totalItems": 1000000,
  "totalPages": 100000
}
```

### Export Reports
```
GET /api/students/export/excel?studentId=123&studentClass=Class1
GET /api/students/export/csv?studentId=123&studentClass=Class1
GET /api/students/export/pdf?studentId=123&studentClass=Class1
```

### Export All Data
```
POST /api/students/export/all/excel
POST /api/students/export/all/csv
POST /api/students/export/all/pdf
Response: { "taskId": "uuid", "message": "Export started" }
```

### Get Export Progress
```
GET /api/students/export/progress/{taskId}
```

## File Storage Locations

### Windows
```
C:\var\log\applications\API\dataprocessing\
```

### Linux
```
/var/log/applications/API/dataprocessing/
```

Ensure these directories exist or the application will create them automatically.

## Performance Optimization

- **Batch Processing**: Database inserts use batch size of 1000
- **Streaming**: SXSSFWorkbook for memory-efficient Excel generation
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
