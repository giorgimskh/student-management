package com.example.student_management.controller;

import com.example.student_management.command.CommandDescriber;
import com.example.student_management.command.CommandRepeater;
import com.example.student_management.command.StudentCommand;
import com.example.student_management.command.StudentCommandInvoker;
import com.example.student_management.dto.CommandEntryResponse;
import com.example.student_management.dto.RepeatCommandRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commands")
public class CommandController {
    private final StudentCommandInvoker invoker;
    private final CommandDescriber describer;
    private final CommandRepeater repeater;

    public CommandController(StudentCommandInvoker invoker, CommandDescriber describer, CommandRepeater repeater) {
        this.invoker = invoker;
        this.describer = describer;
        this.repeater = repeater;
    }

    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory() {
        return ResponseEntity.ok(invoker.commands().stream().map(describer::describe).toList());
    }

    /** Like {@code /history}, with each entry's id and whether it can be repeated for other students. */
    @GetMapping("/entries")
    public ResponseEntity<List<CommandEntryResponse>> getEntries() {
        return ResponseEntity.ok(invoker.entries().stream()
                .map(e -> new CommandEntryResponse(e.id(), describer.describe(e.command()),
                        e.command().studentId(), e.command().repeatable()))
                .toList());
    }

    /** Runs a copy of history entry {@code id} for each student; one result per student. */
    @PostMapping("/{id}/repeat")
    public ResponseEntity<Map<String, List<CommandRepeater.Result>>> repeat(@PathVariable long id,
                                                                           @Valid @RequestBody RepeatCommandRequest request) {
        return ResponseEntity.ok(Map.of("results", repeater.repeat(id, request.studentIds())));
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, String>> undoLast() {
        StudentCommand<?> undone = invoker.undoLast();
        return ResponseEntity.ok(Map.of("undone", describer.describe(undone)));
    }
}
