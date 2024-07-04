package com.PlanAceBot.service;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TelegramBot telegramBot;

    @Scheduled(fixedRate = 1000)
    public void checkDeadlines() {
        List<Task> tasks = taskRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Task task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            if (deadline != null && !task.isNotifiedOneDay()) {
                long daysUntilDeadline = ChronoUnit.DAYS.between(now, deadline);
                long daysFromCreationToDeadline = ChronoUnit.DAYS.between(task.getCreationTimestamp().toLocalDateTime(), deadline);

                if (daysFromCreationToDeadline < 1) {
                    sendImmediateDeadlineNotification(task);
                    continue;
                }

                if (daysUntilDeadline == 3 && daysFromCreationToDeadline >= 3) {
                    sendDeadlineNotification(task, daysUntilDeadline);
                    task.setNotifiedThreeDays(true);
                    taskRepository.save(task);
                } else if (daysUntilDeadline == 1 && daysFromCreationToDeadline >= 1) {
                    sendDeadlineNotification(task, daysUntilDeadline);
                    task.setNotifiedOneDay(true);
                    taskRepository.save(task);
                }
            }
        }
    }

    private void sendImmediateDeadlineNotification(Task task) {
        User user = task.getUser();
        String chatId = String.valueOf(user.getChatId());
        String message = String.format(
                "Задача '%s' была создана с дедлайном менее чем через 1 день. Пожалуйста, проверьте её срочно.\n\n" +
                        "Название: %s\nОписание: %s\nПриоритет: %d",
                task.getTitle(), task.getTitle(), task.getDescription(), task.getPriority()
        );

        telegramBot.sendMessage(chatId, message);
        task.setNotifiedOneDay(true);
        taskRepository.save(task);
    }

    private void sendDeadlineNotification(Task task, long daysUntilDeadline) {
        User user = task.getUser();
        String chatId = String.valueOf(user.getChatId());
        String message = String.format("У вас есть задача, дедлайн которой подходит к концу:\n\n" +
                        "Название: %s\nОписание: %s\nПриоритет: %d\nОсталось %d дней до дедлайна.",
                task.getTitle(), task.getDescription(), task.getPriority(), daysUntilDeadline);

        telegramBot.sendMessage(chatId, message);
    }
}
