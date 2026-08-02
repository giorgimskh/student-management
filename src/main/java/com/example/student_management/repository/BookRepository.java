package com.example.student_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.student_management.domain.Book;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

}
