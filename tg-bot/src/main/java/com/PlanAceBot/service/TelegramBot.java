package com.PlanAceBot.service;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import com.PlanAceBot.state.TaskCreationState;
import com.PlanAceBot.state.TaskState;
import com.PlanAceBot.state.TaskUpdateState;
import com.PlanAceBot.сonfig.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String COMMAND_START = "/start";
    private static final String COMMAND_CREATE = "/create_task";
    private static final String COMMAND_UPDATE = "/update_task";
    private static final String COMMAND_DELETE = "/delete_task";

    private static final String BUTTON_TITLE = "Название";
    private static final String BUTTON_DESCRIPTION = "Описание";
    private static final String BUTTON_CANCEL = "Отмена";
    private static final String BUTTON_CONFIRM = "Да";
    private static final String BUTTON_CANCEL_UPDATE = "Нет";
    private static final String BUTTON_SUBSCRIBE = "Подписаться";
    private static final String BUTTON_CHECK_SUBSCRIPTION = "Проверить подписку";
    private static final String CHANNEL_NAME = "development_max";
    private static final String CHANNEL_USERNAME = "@development_max";


    private Map<String, TaskCreationState> taskCreationStates = new HashMap<>();
    private Map<String, TaskUpdateState> taskUpdateStates = new HashMap<>();
    private Map<String, List<Integer>> taskDeletionStates = new HashMap<>();

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
            handleMessageUpdate(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleMessageUpdate(Update update) {
        String messageText = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        String[] parts = messageText.split(" ", 2);
        String command = parts[0];

        if (taskCreationStates.containsKey(chatId)) {
            processTaskCreation(chatId, messageText);
        } else if (taskUpdateStates.containsKey(chatId)) {
            processFieldAndValue(chatId, messageText);
        } else {
            switch (command) {
                case COMMAND_START:
                    sendSubscribeMessage(chatId);
                    break;

                case COMMAND_CREATE:
                    handleTaskCreationCommand(chatId, parts);
                    break;

                case COMMAND_UPDATE:
                    handleUpdateCommand(parts, chatId);
                    break;

                case COMMAND_DELETE:
                    handleDeleteCommand(parts, chatId);
                    break;

                default:
                    sendUnknownCommandMessage(chatId);
                    break;
            }
        }
    }

    private void sendSubscribeMessage(String chatId) {
        String subscribeMessage = "Подпишитесь на наш канал и затем нажмите кнопку \"Проверить подписку\", чтобы продолжить использование бота.";
        InlineKeyboardMarkup markup = createSubscribeMarkup();
        SendMessage message = createMessage(chatId, subscribeMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending subscribe message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createSubscribeMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton subscribeButton = new InlineKeyboardButton();
        subscribeButton.setText(BUTTON_SUBSCRIBE);
        subscribeButton.setUrl("https://t.me/" + CHANNEL_NAME);
        row1.add(subscribeButton);

        InlineKeyboardButton checkSubscriptionButton = new InlineKeyboardButton();
        checkSubscriptionButton.setText(BUTTON_CHECK_SUBSCRIPTION);
        checkSubscriptionButton.setCallbackData("check_subscription");
        row2.add(checkSubscriptionButton);

        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private boolean isUserSubscribed(String chatId) {
        Long chatIdLong = Long.parseLong(chatId);

        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(CHANNEL_USERNAME);
        getChatMember.setUserId(chatIdLong);

        try {
            ChatMember chatMember = execute(getChatMember);
            String status = chatMember.getStatus();
            return !"left".equals(status) && !"kicked".equals(status);
        } catch (TelegramApiException e) {
            log.error("Error checking subscription status: {}", e.getMessage());
            return false;
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

    private void handleTaskCreationCommand(String chatId, String[] parts) {
        if (parts.length == 1) {
            startTaskCreation(chatId);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте /create_task без параметров.");
        }
    }

    private void startTaskCreation(String chatId) {
        if (getUserTaskCount(chatId) >= 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20). Удалите существующие задачи перед созданием новых.");
            return;
        }

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
        if (getUserTaskCount(chatId) >= 20) {
            sendMessage(chatId, "Вы достигли максимального количества задач (20). Удалите существующие задачи перед созданием новых.");
            return;
        }

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

    private int getUserTaskCount(String chatId) {
        User user = userService.findByChatId(Long.parseLong(chatId));
        if (user == null) {
            log.error("User not found for chatId: {}", chatId);
            return 0;
        }

        return taskService.countTasksByUser(user);
    }

    private void sendUnknownCommandMessage(String chatId) {
        sendMessage(chatId, "Неизвестная команда. Используйте /help, чтобы увидеть доступные команды.");
    }

    private void handleUpdateCommand(String[] parts, String chatId) {
        if (parts.length < 2) {
            sendMessage(chatId, "Неверный формат команды. Используйте /update_task <номер задачи>");
            return;
        }

        try {
            int taskId = Integer.parseInt(parts[1]);

            Task task = taskService.findById(taskId);
            if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
                sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
                return;
            }

            taskUpdateStates.put(chatId, new TaskUpdateState(taskId, "", task));

            sendFieldSelectionMessage(chatId);

        } catch (NumberFormatException e) {
            sendMessage(chatId, "Неверный формат номера задачи.");
        }
    }

    private void sendFieldSelectionMessage(String chatId) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка обновления задачи.");
            return;
        }

        int taskId = currentState.getTaskId();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
            return;
        }

        String currentTitle = task.getTitle();
        String currentDescription = task.getDescription();

        String selectionMessage = "Выберите, что вы хотите обновить для задачи:\n";
        selectionMessage += "Текущее название: " + currentTitle + "\n";
        selectionMessage += "Текущее описание: " + currentDescription + "\n\n";

        InlineKeyboardMarkup markup = createUpdateMarkup();

        SendMessage message = createMessage(chatId, selectionMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending field selection message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createUpdateMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = createButtonRow(BUTTON_TITLE, "update_title");
        List<InlineKeyboardButton> row2 = createButtonRow(BUTTON_DESCRIPTION, "update_description");
        List<InlineKeyboardButton> row3 = createButtonRow(BUTTON_CANCEL, "update_cancel");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private List<InlineKeyboardButton> createButtonRow(String buttonText, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        return row;
    }

    private SendMessage createMessage(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(markup);
        return message;
    }

    private void processFieldAndValue(String chatId, String messageText) {
        TaskUpdateState currentState = taskUpdateStates.get(chatId);

        if (currentState == null) {
            sendMessage(chatId, "Ошибка при обновлении задачи.");
            return;
        }

        int taskId = currentState.getTaskId();
        String fieldToUpdate = currentState.getFieldToUpdate();

        Task task = taskService.findById(taskId);
        if (task == null || !task.getUser().getChatId().equals(Long.parseLong(chatId))) {
            sendMessage(chatId, "Задача с указанным номером не найдена или не принадлежит вам.");
            taskUpdateStates.remove(chatId);
            return;
        }

        updateTaskField(task, fieldToUpdate, messageText);

        sendConfirmationMessage(chatId, task);
    }

    private void updateTaskField(Task task, String fieldToUpdate, String newValue) {
        switch (fieldToUpdate) {
            case "title":
                task.setTitle(newValue);
                break;
            case "description":
                task.setDescription(newValue);
                break;
            default:
                sendMessage(task.getUser().getChatId().toString(), "Ошибка при обновлении задачи.");
                taskUpdateStates.remove(task.getUser().getChatId().toString());
                return;
        }

        taskService.save(task);
    }

    private void sendConfirmationMessage(String chatId, Task task) {
        String confirmationMessage = "Изменения сохранены:\n" +
                "Название: " + task.getTitle() + "\n" +
                "Описание: " + task.getDescription() + "\n\n" +
                "Подтвердить изменения?";

        InlineKeyboardMarkup markup = createConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage, markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_update");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_update"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private InlineKeyboardButton createInlineButton(String buttonText, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        return button;
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();

        if (data.equals("check_subscription")) {
            if (isUserSubscribed(chatId)) {
                registerUserAndSendWelcomeMessage(chatId);
                sendWelcomeMessage(chatId);
            } else {
                sendMessage(chatId, "Вы еще не подписались на канал. Пожалуйста, подпишитесь и нажмите \"Проверить подписку\".");
            }
        } else {
            TaskUpdateState currentState = taskUpdateStates.get(chatId);
            List<Integer> taskIds = taskDeletionStates.get(chatId);

            if (currentState == null && taskIds == null) {
                sendMessage(chatId, "Ошибка при обработке запроса.");
                return;
            }

            switch (data) {
                case "update_title":
                    currentState.setFieldToUpdate("title");
                    sendNewValueRequest(chatId, "title");
                    break;

                case "update_description":
                    currentState.setFieldToUpdate("description");
                    sendNewValueRequest(chatId, "description");
                    break;

                case "confirm_update":
                    taskUpdateStates.remove(chatId);
                    sendMessage(chatId, "Изменения подтверждены.");
                    break;

                case "cancel_update":
                    cancelUpdate(chatId, currentState);
                    break;

                case "confirm_delete":
                    confirmDelete(chatId, taskIds);
                    break;

                case "cancel_delete":
                    cancelDelete(chatId);
                    break;

                default:
                    sendMessage(chatId, "Неверный выбор.");
                    break;
            }
        }

        editMessageReplyMarkup(chatId, callbackQuery.getMessage().getMessageId());
    }

    private void cancelUpdate(String chatId, TaskUpdateState currentState) {
        taskService.save(currentState.getOriginalTask());
        taskUpdateStates.remove(chatId);
        sendMessage(chatId, "Изменения отменены.");
    }

    private void sendNewValueRequest(String chatId, String field) {
        String messageText;
        switch (field) {
            case "title":
                messageText = "Введите новое название задачи:";
                break;
            case "description":
                messageText = "Введите новое описание задачи:";
                break;
            default:
                log.error("Unsupported field type: {}", field);
                return;
        }

        sendMessage(chatId, messageText);
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

    private void editMessageReplyMarkup(String chatId, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        markup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMessage = new EditMessageReplyMarkup();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Error editing message reply markup: {}", e.getMessage());
        }
    }

    private void handleDeleteCommand(String[] parts, String chatId) {
        if (parts.length < 2) {
            sendMessage(chatId, "Неверный формат команды. Используйте /delete_task <номер задачи1> <номер задачи2> ...");
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(parts[1], " ");
        List<Integer> taskIdsToDelete = new ArrayList<>();

        while (tokenizer.hasMoreTokens()) {
            try {
                int taskId = Integer.parseInt(tokenizer.nextToken().trim());
                taskIdsToDelete.add(taskId);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Неверный формат номера задачи.");
                return;
            }
        }

        if (taskIdsToDelete.isEmpty()) {
            sendMessage(chatId, "Неверный формат номеров задачи.");
            return;
        }

        taskDeletionStates.put(chatId, taskIdsToDelete);
        sendDeleteConfirmationMessage(chatId, taskIdsToDelete);
    }


    private void sendDeleteConfirmationMessage(String chatId, List<Integer> taskIdsToDelete) {
        StringBuilder confirmationMessage = new StringBuilder("Вы уверены, что хотите удалить следующие задачи?\n\n");
        for (Integer taskId : taskIdsToDelete) {
            Task task = taskService.findById(taskId);
            if (task != null) {
                confirmationMessage.append("Номер: ").append(task.getId()).append("\n");
                confirmationMessage.append("Название: ").append(task.getTitle()).append("\n");
                confirmationMessage.append("Описание: ").append(task.getDescription()).append("\n\n");
            }
        }

        InlineKeyboardMarkup markup = createDeleteConfirmationMarkup();

        SendMessage message = createMessage(chatId, confirmationMessage.toString(), markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending delete confirmation message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createDeleteConfirmationMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = createButtonRow(BUTTON_CONFIRM, "confirm_delete");
        row.add(createInlineButton(BUTTON_CANCEL_UPDATE, "cancel_delete"));

        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    private void confirmDelete(String chatId, List<Integer> taskIdsToDelete) {
        for (Integer taskId : taskIdsToDelete) {
            taskService.delete(taskId);
        }
        taskDeletionStates.remove(chatId);
        sendMessage(chatId, "Задачи удалены.");
    }

    private void cancelDelete(String chatId) {
        taskDeletionStates.remove(chatId);
        sendMessage(chatId, "Удаление отменено.");
    }




}
