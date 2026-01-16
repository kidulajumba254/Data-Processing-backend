package com.kidula.studentdataprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_id", columnList = "studentId"),
        @Index(name = "idx_class", columnList = "studentClass")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long studentId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(nullable = false, name = "studentClass")
    private String studentClass;

    @Column(nullable = false)
    private Integer score;

    public Student(Long studentId, String firstName, String lastName,
                   LocalDate dob, String studentClass, Integer score) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = dob;
        this.studentClass = studentClass;
        this.score = score;
    }
}
