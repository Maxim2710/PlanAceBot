package com.PlanAceBot.service;

import com.PlanAceBot.model.Budget;
import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.BudgetRepository;
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
    private BudgetRepository budgetRepository;

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

    @Scheduled(fixedRate = 1000)
    public void checkBudgetWarnings() {
        List<Budget> budgets = budgetRepository.findAll();

        for (Budget budget : budgets) {
            if (!budget.isNotificationSent() && budget.getAmount() < budget.getWarningThreshold()) {
                sendBudgetWarningNotification(budget);
                budget.setNotificationSent(true);
                budget.setLastNotificationSentTime(LocalDateTime.now());
                budgetRepository.save(budget);
            } else if (budget.isNotificationSent()) {
                LocalDateTime lastNotificationSentTime = budget.getLastNotificationSentTime();
                LocalDateTime now = LocalDateTime.now();
                long daysSinceLastNotification = ChronoUnit.DAYS.between(lastNotificationSentTime, now);

                if (daysSinceLastNotification >= 7) {
                    budget.setNotificationSent(false);
                    budgetRepository.save(budget);
                }
            }
        }
    }


    private void sendBudgetWarningNotification(Budget budget) {
        User user = budget.getUser();
        String chatId = String.valueOf(user.getChatId());
        String message = String.format(
                "Уведомление: Бюджет '%s' меньше предупреждающего порога!\n\n" +
                        "Название: %s\nСумма: %s\nДата начала: %s\nДата окончания: %s\nКатегория: %s",
                budget.getName(), budget.getName(), budget.getAmount(), budget.getStartDate(), budget.getEndDate(), budget.getCategory()
        );

        telegramBot.sendMessage(chatId, message);
    }
}
