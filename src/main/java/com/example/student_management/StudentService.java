package com.example.student_management;

import com.example.student_management.domain.Student;
import com.example.student_management.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepository;

    public List<Student> getAllStudents(){
        return studentRepository.findAll();
    }

    public Student  createStudent(Student student){
        return studentRepository.save(student);
    }

    public Student getStudentById(UUID id){
        return studentRepository.findById(id).orElseThrow(()->new RuntimeException("Student not found with id: " + id));
    }

    public Student updateStudent(UUID id, Student studentDetails) {
        Student student = getStudentById(id);
        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail());
        return studentRepository.save(student);
    }

    public void deleteStudent(UUID id){
        studentRepository.deleteById(id);
    }
}
