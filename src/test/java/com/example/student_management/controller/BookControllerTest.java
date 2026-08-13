package com.example.student_management.controller;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.BookService;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
public class BookControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean private BookService bookService;
    @Autowired private ObjectMapper objectMapper;

    private static final String VALID_ISBN = "0-306-40615-2";
    @Test
    void getAllBooks_returnsOkAndList() throws Exception{
        Book book = Book.builder().id(UUID.randomUUID()).title("Clean code").isbn(VALID_ISBN).build();

        when(bookService.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean code"))
                .andExpect(jsonPath("$[0].isbn").value(VALID_ISBN));
    }


    @Test
    public void getBookById_returns404_whenServiceThrowsResourceNotFound() throws Exception{
        UUID id=UUID.randomUUID();
        Book book=Book.builder().id(id).title("Clean code").isbn(VALID_ISBN).build();

        when(bookService.getBookById(id)).thenThrow(new ResourceNotFoundException("Book not found with id: " + id));

        mockMvc.perform(get("/api/books/{id}",id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book not found with id: "+ id));
    }

    @Test
    void createBook_returns400_whenIsbnIsMalformed() throws Exception {
        // "12345" fails the ISBN @Pattern regex -> should never reach the service.
        String invalidPayload = """
                {"title": "Clean Code", "isbn": "12345"}
                """;

        mockMvc.perform(post("/api/books")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").value("ISBN must be 10 or 13 digits"));

        verifyNoInteractions(bookService);
    }

    @Test
    void createBook_returns400_whenTitleIsBlank() throws Exception {
        String invalidPayload = """
                {"title": "", "isbn": "%s"}
                """.formatted(VALID_ISBN);

        mockMvc.perform(post("/api/books")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());

        verifyNoInteractions(bookService);
    }

    @Test
    void createBook_returns200_whenValid() throws Exception {
        Book input = Book.builder()
                .title("Clean Code")
                .isbn(VALID_ISBN)
                .build();

        Book saved = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn(VALID_ISBN)
                .build();

        when(bookService.createBook(any(Book.class))).thenReturn(saved);

        mockMvc.perform(post("/api/books")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void deleteBook_returns204_andDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(eq(id));
    }

    @Test
    void getBookOwner_returnsOkAndStudent_whenBookHasOwner() throws Exception {
        UUID bookId = UUID.randomUUID();
        Student owner = Student.builder()
                .id(UUID.randomUUID())
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        when(bookService.getBookOwner(bookId)).thenReturn(owner);

        mockMvc.perform(get("/api/books/{bookId}/owner", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nino Kapanadze"));
    }

    @Test
    void getBookOwner_returns404_whenBookHasNoOwner() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(bookService.getBookOwner(bookId))
                .thenThrow(new ResourceNotFoundException("Book with id: " + bookId + " has no owner"));

        mockMvc.perform(get("/api/books/{bookId}/owner", bookId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book with id: " + bookId + " has no owner"));
    }
}

