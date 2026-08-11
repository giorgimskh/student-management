package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks(){
        return bookRepository.findAll();
    }

    public Book getBookById(UUID id){
        return bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book not found with id: "+id));
    }

    public Book createBook(Book book){
        return bookRepository.save(book);
    }

    public void deleteBook(UUID id){
        Book book = getBookById(id);

        if(book.getStudent()!=null){
            book.getStudent().getBooks().remove(book);
        }

        bookRepository.delete(book);
    }

    public Student getBookOwner(UUID bookId){
        Book book = bookRepository.findById(bookId).orElseThrow(()->new ResourceNotFoundException("No book found with id: " + bookId));

        if(book.getStudent()==null){
            throw new RuntimeException("Book with id: "+bookId+ " has no owner");
        }
        return book.getStudent();
    }
}
