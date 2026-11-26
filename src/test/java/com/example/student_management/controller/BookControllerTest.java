package com.example.student_management.controller;

import com.example.student_management.mapper.BookMapper;
import com.example.student_management.mapper.StudentMapper;
import org.springframework.context.annotation.Import;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.InvalidCopyCountException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import({BookMapper.class, StudentMapper.class})
public class BookControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean private BookService bookService;
    @Autowired private ObjectMapper objectMapper;

    private static final String VALID_ISBN = "0-306-40615-2";
    @Test
    void getAllBooks_returnsOkAndList() throws Exception{
        Book book = Book.builder().id(UUID.randomUUID()).title("Clean code").isbn(VALID_ISBN).build();

        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(book)));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean code"))   // note: content[0], not [0]
                .andExpect(jsonPath("$.content[0].isbn").value(VALID_ISBN));
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
                        .contentType(MediaType.APPLICATION_JSON)
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
                        .contentType(MediaType.APPLICATION_JSON)
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
                        .contentType(MediaType.APPLICATION_JSON)
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

    @Test
    void addCopies_returns201WithTheNewBooks() throws Exception {
        UUID id = UUID.randomUUID();
        Book copy1 = Book.builder().id(UUID.randomUUID()).title("Clean code").isbn(VALID_ISBN).build();
        Book copy2 = Book.builder().id(UUID.randomUUID()).title("Clean code").isbn(VALID_ISBN).build();
        when(bookService.addCopies(id, 2)).thenReturn(List.of(copy1, copy2));

        mockMvc.perform(post("/api/books/{id}/copies", id).param("count", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(copy2.getId().toString()))
                .andExpect(jsonPath("$[1].title").value("Clean code"));
    }

    @Test
    void addCopies_defaultsToOne() throws Exception {
        UUID id = UUID.randomUUID();
        when(bookService.addCopies(id, 1)).thenReturn(List.of(Book.builder().id(UUID.randomUUID()).title("T").build()));

        mockMvc.perform(post("/api/books/{id}/copies", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addCopies_badCount_is400() throws Exception {
        UUID id = UUID.randomUUID();
        when(bookService.addCopies(id, 0)).thenThrow(new InvalidCopyCountException("Number of copies must be between 1 and 20"));

        mockMvc.perform(post("/api/books/{id}/copies", id).param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Number of copies must be between 1 and 20"));
    }
}
