package com.example.student_management.controller;

import com.example.student_management.command.CommandDescriber;
import com.example.student_management.command.CommandEntry;
import com.example.student_management.command.CommandRepeater;
import com.example.student_management.command.StudentCommand;
import com.example.student_management.command.StudentCommandInvoker;
import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.exceptions.NothingToUndoException;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

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

    @MockitoBean
    private CommandRepeater repeater;

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

    @Test
    void entries_includeIdAndWhetherRepeatable() throws Exception {
        StudentCommand<?> enroll = mock(StudentCommand.class);
        UUID ana = UUID.randomUUID();
        when(enroll.repeatable()).thenReturn(true);
        when(enroll.studentId()).thenReturn(ana);
        when(invoker.entries()).thenReturn(List.of(new CommandEntry(7, enroll)));
        when(describer.describe(enroll)).thenReturn("Enroll Ana in Algorithms (CS101)");

        mockMvc.perform(get("/api/commands/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].description").value("Enroll Ana in Algorithms (CS101)"))
                .andExpect(jsonPath("$[0].studentId").value(ana.toString()))
                .andExpect(jsonPath("$[0].repeatable").value(true));
    }

    @Test
    void repeat_returnsOneResultPerStudent() throws Exception {
        UUID beka = UUID.randomUUID();
        UUID gio = UUID.randomUUID();
        when(repeater.repeat(7, List.of(beka, gio))).thenReturn(List.of(
                new CommandRepeater.Result(beka, CommandRepeater.Outcome.DONE, "Enroll Beka in CS101"),
                new CommandRepeater.Result(gio, CommandRepeater.Outcome.FAILED, "Gio is suspended")));

        mockMvc.perform(post("/api/commands/7/repeat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + beka + "\",\"" + gio + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].outcome").value("DONE"))
                .andExpect(jsonPath("$.results[1].studentId").value(gio.toString()))
                .andExpect(jsonPath("$.results[1].message").value("Gio is suspended"));
    }

    @Test
    void repeat_withNoStudents_is400() throws Exception {
        mockMvc.perform(post("/api/commands/7/repeat").contentType(MediaType.APPLICATION_JSON).content("{\"studentIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.studentIds").value("Choose at least one student"));
        verifyNoInteractions(repeater);
    }

    @Test
    void repeat_notRepeatableCommand_is400() throws Exception {
        UUID beka = UUID.randomUUID();
        when(repeater.repeat(7, List.of(beka))).thenThrow(new CommandNotRepeatableException("a book can be lent to only one student"));

        mockMvc.perform(post("/api/commands/7/repeat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + beka + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("a book can be lent to only one student"));
    }
}
