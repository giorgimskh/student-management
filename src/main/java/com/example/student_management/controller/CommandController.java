package com.example.student_management.controller;

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

    public CommandController(StudentCommandInvoker invoker) {
        this.invoker = invoker;
    }

    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory() {
        return ResponseEntity.ok(invoker.history());
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, String>> undoLast() {
        StudentCommand<?> undone = invoker.undoLast();
        return ResponseEntity.ok(Map.of("undone", undone.description()));
    }
}
