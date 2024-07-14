package com.PlanAceBot.service;

import com.PlanAceBot.model.*;
import com.PlanAceBot.repository.AdsRepository;
import com.PlanAceBot.repository.BudgetRepository;
import com.PlanAceBot.repository.TaskRepository;
import com.PlanAceBot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PomodoroService pomodoroService;

    @Autowired
    private NinetyThirtyService ninetyThirtyService;

    @Scheduled(fixedRate = 1000)
    public void checkDeadlines() {
        List<Task> tasks = taskRepository.findAll();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        for (Task task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            if (deadline != null && !task.isNotifiedOneDay()) {
                User user = task.getUser();
                String userTimezone = user.getTimezone();
                if (userTimezone == null || userTimezone.isEmpty()) {
                    userTimezone = "UTC";
                }

                ZonedDateTime userNow = now.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(userTimezone));
                ZonedDateTime userDeadline = deadline.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(userTimezone));

                long daysUntilDeadline = ChronoUnit.DAYS.between(userNow, userDeadline);
                long daysFromCreationToDeadline = ChronoUnit.DAYS.between(task.getCreationTimestamp().toLocalDateTime(), deadline);

                if (daysFromCreationToDeadline < 1) {
                    sendImmediateDeadlineNotification(task);
                    continue;
                }

                if (daysUntilDeadline == 3 && daysFromCreationToDeadline >= 3) {
                    sendDeadlineNotification(task, daysUntilDeadline);
                    task.setNotifiedThreeDays(true);
                    taskRepository.save(task);
                } else if (daysUntilDeadline == 1) {
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
                "\uD83D\uDCE2 Задача '%s' была создана с дедлайном менее чем через 1 день. Пожалуйста, проверьте её срочно. \uD83D\uDCE2\n\n" +
                        "\uD83D\uDD16 Название: %s\n\uD83D\uDCDD Описание: %s⭐ Приоритет: %d",
                task.getTitle(), task.getTitle(), task.getDescription(), task.getPriority()
        );

        telegramBot.sendMessage(chatId, message);
        task.setNotifiedOneDay(true);
        taskRepository.save(task);
    }

    private void sendDeadlineNotification(Task task, long daysUntilDeadline) {
        User user = task.getUser();
        String chatId = String.valueOf(user.getChatId());
        String userTimezone = user.getTimezone();
        if (userTimezone == null || userTimezone.isEmpty()) {
            userTimezone = "UTC";
        }

        ZonedDateTime userDeadline = task.getDeadline().atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(userTimezone));
        String deadlineString = userDeadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String message = String.format("\uD83D\uDCC6 У вас есть задача, дедлайн которой подходит к концу:\n\n" +
                        "\uD83D\uDD16 Название: %s\n\uD83D\uDCDD Описание: %s⭐ Приоритет: %d\n⏳ Осталось %d дней до дедлайна (%s).",
                task.getTitle(), task.getDescription(), task.getPriority(), daysUntilDeadline, deadlineString);

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
                "\uD83D\uDCB0 Уведомление: Бюджет '%s' меньше предупреждающего порога! \uD83D\uDCB0\n\n" +
                        "\uD83D\uDD16 Название: %s\n\uD83D\uDCCB Сумма: %s\n\uD83D\uDCC5 Дата начала: %s\n\uD83D\uDCC5 Дата окончания: %s\n\uD83D\uDCD2 Категория: %s",
                budget.getName(), budget.getName(), budget.getAmount(), budget.getStartDate(), budget.getEndDate(), budget.getCategory()
        );

        telegramBot.sendMessage(chatId, message);
    }

    @Scheduled(fixedRate = 1000)
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();
        var currentTime = LocalDateTime.now();

        ads.parallelStream().forEach(ad -> {
            if (ad.getSendTime().isBefore(currentTime) || ad.getSendTime().isEqual(currentTime)) {
                users.parallelStream().forEach(user ->
                        telegramBot.prepareAndSendMessage(user.getChatId(), ad.getAd())
                );
                adsRepository.deleteById(ad.getId());
            }
        });
    }

    @Scheduled(fixedRate = 1000)
    public void checkPomodoroSessions() {
        List<Pomodoro> allSessions = pomodoroService.getAllPomodoroSessions();

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        for (Pomodoro pomodoro : allSessions) {
            Timestamp endTime = pomodoro.getEndTime();

            if (endTime != null && currentTime.after(endTime)) {
                if ("work".equals(pomodoro.getIntervalType())) {
                    pomodoro.setSessionActive(false);
                    pomodoro.setIntervalType("rest");
                    pomodoro.setEndTime(new Timestamp(currentTime.getTime() + 5 * 60 * 1000));
                    pomodoroService.savePomodoroSession(pomodoro);

                    String chatId = String.valueOf(pomodoro.getUser().getChatId());
                    telegramBot.sendPomodoroMessage(chatId, "\uD83D\uDD50 Время рабочего интервала истекло. Отдохните 5 минут! \uD83D\uDD50", telegramBot.createPomodoroKeyboard());
                } else if ("rest".equals(pomodoro.getIntervalType())) {
                    pomodoro.setSessionActive(true);
                    pomodoro.setIntervalType("work");
                    pomodoro.setEndTime(new Timestamp(currentTime.getTime() + 25 * 60 * 1000));
                    pomodoroService.savePomodoroSession(pomodoro);

                    String chatId = String.valueOf(pomodoro.getUser().getChatId());
                    telegramBot.sendPomodoroMessage(chatId, "\uD83D\uDCAA Отдых завершен. Сфокусируйтесь на работе в течение 25 минут! \uD83D\uDCAA", telegramBot.createPomodoroKeyboard());
                }
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void checkNinetyThirtySessions() {
        List<NinetyThirty> allSessions = ninetyThirtyService.getAllNinetyThirtySessions();

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        for (NinetyThirty session : allSessions) {
            Timestamp endTime = session.getEndTime();

            if (endTime != null && currentTime.after(endTime)) {
                if ("work90".equals(session.getIntervalType())) {
                    session.setSessionActive(false);
                    session.setIntervalType("rest30");
                    session.setEndTime(new Timestamp(currentTime.getTime() + 30 * 60 * 1000));
                    ninetyThirtyService.saveNinetyThirtySession(session);

                    String chatId = String.valueOf(session.getUser().getChatId());
                    telegramBot.sendNinetyThirtyMessage(chatId, "\uD83D\uDD50 Время рабочего интервала истекло. Отдохните 30 минут! \uD83D\uDD50", telegramBot.createNinetyThirtyKeyboard());
                } else if ("rest30".equals(session.getIntervalType())) {
                    session.setSessionActive(true);
                    session.setIntervalType("work90");
                    session.setEndTime(new Timestamp(currentTime.getTime() + 90 * 60 * 1000));
                    ninetyThirtyService.saveNinetyThirtySession(session);

                    String chatId = String.valueOf(session.getUser().getChatId());
                    telegramBot.sendNinetyThirtyMessage(chatId, "\uD83D\uDCAA Отдых завершен. Сфокусируйтесь на работе в течение 90 минут! \uD83D\uDCAA", telegramBot.createNinetyThirtyKeyboard());
                }
            }
        }
    }


}
