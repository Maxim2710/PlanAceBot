package com.PlanAceBot.service;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.state.TaskCreationState;
import com.PlanAceBot.state.TaskState;
import com.PlanAceBot.сonfig.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private Map<String, TaskCreationState> taskCreationStates = new HashMap<>();


    @Autowired
    private UserService userService;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private TaskService taskService;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String[] parts = messageText.split(" ", 2);
            String command = parts[0];

            if (taskCreationStates.containsKey(chatId)) {
                processTaskCreation(chatId, messageText);
            } else {
                switch (command) {
                    case "/start":
                        registerUserAndSendWelcomeMessage(chatId);
                        break;

                    case "/create":
                        if (parts.length == 1) {
                            startTaskCreation(chatId);
                        } else {
                            sendMessage(chatId, "Неверный формат команды. Используйте /create без параметров.");
                        }
                        break;

//                    case "/update":
//                        handleUpdateCommand(parts, chatId);
//                        break;
//
//                    case "/delete":
//                        handleDeleteCommand(parts, chatId);
//                        break;
//
//                    case "/help":
//                        handleHelpCommand(chatId);
//                        break;

                    default:
                        sendMessage(chatId, "Неизвестная команда. Используйте /help, чтобы увидеть доступные команды.");
                }
            }
        }
    }

    private void registerUserAndSendWelcomeMessage(String chatId) {
        if (!userService.existByChatId(Long.parseLong(chatId))) {
            User currentUser = new User();
            currentUser.setChatId(Long.parseLong(chatId));
            currentUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userService.save(currentUser);
            sendWelcomeMessage(chatId);
        }

        sendWelcomeBackMessage(chatId);
    }

    private void sendWelcomeMessage(String chatId) {
        String welcomeMessage = EmojiParser.parseToUnicode("Добро пожаловать! Я бот для управления задачами. :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");
        sendMessage(chatId, welcomeMessage);
    }

    private void sendWelcomeBackMessage(String chatId) {
        String welcomeBackMessage = EmojiParser.parseToUnicode("С возвращением! :blush:\n" +
                "Используйте команду /help, чтобы увидеть список доступных команд.");
        sendMessage(chatId, welcomeBackMessage);
    }

    private void startTaskCreation(String chatId) {
        taskCreationStates.put(chatId, new TaskCreationState());
        sendMessage(chatId, "Введите название задачи:");
    }

    private void processTaskCreation(String chatId, String messageText) {
        TaskCreationState currentState = taskCreationStates.get(chatId);

        if (currentState.getState() == TaskState.ENTER_TITLE) {
            currentState.setTitle(messageText);
            currentState.setState(TaskState.ENTER_DESCRIPTION);
            sendMessage(chatId, "Введите описание задачи для '" + messageText + "':");
        } else if (currentState.getState() == TaskState.ENTER_DESCRIPTION) {
            String title = currentState.getTitle();
            String description = messageText;

            createTask(title, description, chatId);

            taskCreationStates.remove(chatId);

            sendMessage(chatId, "Задача '" + title + "' с описанием '" + description + "' создана.");
        }
    }

    private void createTask(String title, String description, String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            sendMessage(chatId, "Пользователь не зарегистрирован. Используйте /start для регистрации.");
            return;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setUser(user);
        task.setCompleted(false);

        taskService.save(task);
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }
}
