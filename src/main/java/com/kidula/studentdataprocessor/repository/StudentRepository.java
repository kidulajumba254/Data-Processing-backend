package com.kidula.studentdataprocessor.repository;

import com.kidula.studentdataprocessor.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(Long studentId);

    Page<Student> findByStudentClass(String studentClass, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE " +
            "(:studentId IS NULL OR s.studentId = :studentId) AND " +
            "(:studentClass IS NULL OR s.studentClass = :studentClass)")
    Page<Student> findByFilters(@Param("studentId") Long studentId,
                                @Param("studentClass") String studentClass,
                                Pageable pageable);

    long count();
}
