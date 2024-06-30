package com.PlanAceBot.service;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public void save(Task task) {
        taskRepository.save(task);
    }

    public Task findById(int id) {
        Optional<Task> task = taskRepository.findById((long) id);
        return task.orElse(null);
    }
}
