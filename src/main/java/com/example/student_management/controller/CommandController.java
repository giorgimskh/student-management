package com.example.student_management.controller;

import com.example.student_management.command.CommandDescriber;
import com.example.student_management.command.StudentCommand;
import com.example.student_management.command.StudentCommandInvoker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commands")
public class CommandController {
    private final StudentCommandInvoker invoker;
    private final CommandDescriber describer;

    public CommandController(StudentCommandInvoker invoker, CommandDescriber describer) {
        this.invoker = invoker;
        this.describer = describer;
    }

    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory() {
        return ResponseEntity.ok(invoker.commands().stream().map(describer::describe).toList());
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, String>> undoLast() {
        StudentCommand<?> undone = invoker.undoLast();
        return ResponseEntity.ok(Map.of("undone", describer.describe(undone)));
    }
}
