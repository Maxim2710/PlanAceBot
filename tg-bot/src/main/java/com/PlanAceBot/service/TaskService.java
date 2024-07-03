package com.PlanAceBot.service;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserService userService;

    public void save(Task task) {
        taskRepository.save(task);
    }

    public Task findById(int id) {
        Optional<Task> task = taskRepository.findById((long) id);
        return task.orElse(null);
    }

    public void delete(int id) {
        taskRepository.deleteById((long) id);
    }

    public int countTasksByUser(User user) {
        return taskRepository.countByUser(user);
    }

    public List<Task> findTasksByUserId(Long userId) {
        return taskRepository.findByUser_ChatId(userId);
    }

    public List<Task> findByCompletedTrue() {
        return taskRepository.findByCompletedTrue();
    }

    public void delete(Task task) {
        taskRepository.delete(task);
    }

    public List<Task> getTasksByUserChatId(Long chatId) {
        User user = userService.findByChatId(chatId);
        if (user != null) {
            return taskRepository.findByUser(user);
        }
        return Collections.emptyList();
    }

}
