package com.example.student_management.controller;

import com.example.student_management.command.CommandDescriber;
import com.example.student_management.command.StudentCommand;
import com.example.student_management.command.StudentCommandInvoker;
import com.example.student_management.exceptions.NothingToUndoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommandController.class)
class CommandControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentCommandInvoker invoker;

    @MockitoBean
    private CommandDescriber describer;

    @Test
    void history_returnsDescriberOutputMostRecentFirst() throws Exception {
        StudentCommand<?> enroll = mock(StudentCommand.class);
        StudentCommand<?> assign = mock(StudentCommand.class);
        when(invoker.commands()).thenReturn(List.of(enroll, assign));
        when(describer.describe(enroll)).thenReturn("Enroll student 2");
        when(describer.describe(assign)).thenReturn("Assign book 1");

        mockMvc.perform(get("/api/commands/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Enroll student 2"))
                .andExpect(jsonPath("$[1]").value("Assign book 1"));
    }

    @Test
    void history_whenEmpty_returnsEmptyArray() throws Exception {
        when(invoker.commands()).thenReturn(List.of());

        mockMvc.perform(get("/api/commands/history"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void undo_returnsDescriptionOfUndoneCommand() throws Exception {
        StudentCommand<?> command = mock(StudentCommand.class);
        when(describer.describe(command)).thenReturn("Assign book 1 to student 2");
        doReturn(command).when(invoker).undoLast();

        mockMvc.perform(post("/api/commands/undo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.undone").value("Assign book 1 to student 2"));
    }

    @Test
    void undo_withEmptyHistory_returns409() throws Exception {
        when(invoker.undoLast()).thenThrow(new NothingToUndoException("There is no command to undo"));

        mockMvc.perform(post("/api/commands/undo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("There is no command to undo"));
    }
}
