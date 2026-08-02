package com.example.student_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Book;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

}
