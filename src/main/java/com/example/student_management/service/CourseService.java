package com.example.student_management.service;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> getAllCourses(){
        return courseRepository.findAll();
    }

    public Course getCourseById(UUID id){
        return courseRepository.findById(id).orElseThrow(()->new RuntimeException("Course not found with id: "+id));
    }

    public Course createCourse(Course course){
        return courseRepository.save(course);
    }

    public void deleteCourse(UUID id){
        Course course = courseRepository.findById(id).orElseThrow(()->new RuntimeException("Course not found with id: " + id));

        for(Student student:course.getStudents()){
            student.getCourses().remove(course);
        }

        course.getStudents().clear();
        courseRepository.delete(course);
    }

    public Set<Student> getStudentsInCourse(UUID courseId){
        return getCourseById(courseId).getStudents();
    }
}
